package com.neostride.app.feature.community.tip;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.mypage.MyPageActivity;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;
import com.neostride.app.feature.community.tip.model.TipBookmarkResponse;
import com.neostride.app.feature.community.tip.model.TipItem;
import com.neostride.app.feature.community.tip.model.TipLikeResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * 팁 게시글 RecyclerView 어댑터 클래스임
 * 팁 목록 데이터를 item_tip.xml 화면에 연결하고,
 * 각 팁 게시글 클릭 시 TipDetailActivity로 이동하도록 처리함
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    // 형광 포인트 색상임
    private static final int POINT_COLOR = Color.parseColor("#B8FF06");

    // 기본 흰색임
    private static final int WHITE_COLOR = Color.parseColor("#FFFFFF");

    // 팁 게시글 목록을 저장하는 리스트임
    private final ArrayList<TipItem> tipList;

    // 팁 좋아요 상태를 저장하는 Map임
    // TipFragment에서 서버/목서버 응답값을 넣어주고, Adapter에서 API 응답에 따라 갱신함
    private final Map<Long, Boolean> likedStateMap;

    // 팁 북마크 상태를 저장하는 Map임
    // TipFragment에서 서버/목서버 응답값을 넣어주고, Adapter에서 API 응답에 따라 갱신함
    private final Map<Long, Boolean> bookmarkedStateMap;

    // 팁 좋아요 개수를 저장하는 Map임
    // RecyclerView 재사용 시 좋아요 개수가 꼬이지 않도록 별도로 관리함
    private final Map<Long, Integer> likeCountMap;

    // Activity 이동과 Toast 출력에 사용할 Context 객체임
    private Context context;

    // 팁 API 호출을 담당하는 Repository임
    private final TipRepository tipRepository = new TipRepository();

    /*
     * TipAdapter 생성자임
     * Fragment에서 필터링된 팁 목록과 좋아요/북마크 상태 Map을 전달받음
     */
    public TipAdapter(
            ArrayList<TipItem> tipList,
            Map<Long, Boolean> likedStateMap,
            Map<Long, Boolean> bookmarkedStateMap,
            Map<Long, Integer> likeCountMap
    ) {
        this.tipList = tipList;
        this.likedStateMap = likedStateMap;
        this.bookmarkedStateMap = bookmarkedStateMap;
        this.likeCountMap = likeCountMap;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_tip, parent, false);

        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TipViewHolder holder,
            int position
    ) {
        TipItem item = tipList.get(position);

        // 팁 게시글 기본 정보를 화면에 표시함
        holder.tvNickname.setText(getSafeText(item.getNickname(), "알 수 없음"));

        // 카테고리 알림판에 카테고리별 네온 색상을 적용함
        applyCategoryBadgeStyle(holder.tvCategory, item.getCategory());

        holder.tvTitle.setText(getSafeText(item.getTitle(), ""));
        holder.tvContent.setText(getSafeText(item.getContent(), ""));
        holder.tvLikeCount.setText(String.valueOf(getCurrentLikeCount(item)));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        // 댓글 하이라이트 — 내가 댓글 단 글이면 형광 초록 (마이페이지 패턴)
        int commentColor = item.isCommented()
                ? Color.parseColor("#B8FF06")
                : Color.parseColor("#FFFFFF");
        if (holder.ivComment != null) {
            holder.ivComment.setImageTintList(android.content.res.ColorStateList.valueOf(commentColor));
        }
        holder.tvCommentCount.setTextColor(commentColor);

        // 작성 시간이 있으면 표시하고, 없으면 기본값으로 표시함
        if (item.getCreatedAt() != null && !item.getCreatedAt().trim().isEmpty()) {
            holder.tvTime.setText(item.getCreatedAt());
        } else {
            holder.tvTime.setText("방금 전");
        }

        // 배지 표시 + 등급별 색상 적용 (NONE이면 숨김)
        if (holder.ivBadge != null) {
            com.neostride.app.feature.badge.model.BadgeTier tier =
                    com.neostride.app.feature.badge.model.BadgeTier.fromString(item.getBadgeType());
            if (!item.isBadgeOwner() || tier.isNone()) {
                holder.ivBadge.setVisibility(View.GONE);
            } else {
                holder.ivBadge.setVisibility(View.VISIBLE);
                holder.ivBadge.setColorFilter(tier.getColor());
            }
        }

        // GPS 아이콘 표시 여부를 설정함
        holder.ivGps.setVisibility(item.isGpsVisible() ? View.VISIBLE : View.GONE);

        // 작성자 프로필 이미지를 표시함
        bindProfileImage(holder, item);

        /*
         * 팁 이미지를 표시함
         * 1순위: 로컬 imageUris
         * 2순위: 서버 imageUrls
         * 3순위: 코스 GPS 지도 routeMapImageUrl
         */
        bindTipImage(holder, item);

        /*
         * RecyclerView 재사용 문제를 막기 위해
         * 매번 현재 item의 좋아요/북마크 상태를 다시 적용함
         */
        boolean liked = isLiked(item);
        boolean bookmarked = isBookmarked(item);

        holder.ivLike.setImageTintList(
                ColorStateList.valueOf(liked ? POINT_COLOR : WHITE_COLOR)
        );

        holder.tvLikeCount.setTextColor(liked ? POINT_COLOR : WHITE_COLOR);

        // 북마크 상태에 따라 빈 북마크 / 꽉 찬 북마크 아이콘을 변경함
        holder.ivBookmark.setImageResource(
                bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
        );

        holder.ivBookmark.setImageTintList(
                ColorStateList.valueOf(bookmarked ? POINT_COLOR : WHITE_COLOR)
        );

        /*
         * 카드 전체 클릭 시 상세 화면으로 이동함
         */
        holder.itemView.setOnClickListener(v -> openTipDetail(item));

        /*
         * 제목, 내용, 본문 영역, 사진 클릭 시 상세 화면으로 이동함
         */
        holder.layoutBody.setOnClickListener(v -> openTipDetail(item));
        holder.tvTitle.setOnClickListener(v -> openTipDetail(item));
        holder.tvContent.setOnClickListener(v -> openTipDetail(item));
        holder.cardTipPhoto.setOnClickListener(v -> openTipDetail(item));

        /*
         * 댓글 아이콘/댓글 수 클릭 시 상세 화면으로 이동함
         * 댓글은 상세 화면에서 보는 구조로 처리함
         */
        holder.ivComment.setOnClickListener(v -> openTipDetail(item));
        holder.tvCommentCount.setOnClickListener(v -> openTipDetail(item));

        /*
         * 프사/닉네임 클릭 시 내 계정이면 마이페이지,
         * 남의 계정이면 러너페이지로 이동함
         */
        holder.ivProfile.setOnClickListener(v -> openProfile(item));
        holder.tvNickname.setOnClickListener(v -> openProfile(item));

        /*
         * 좋아요 클릭 시 목록에서도 좋아요 API를 호출함
         * 목서버 Map 상태가 바뀌므로 상세 화면과 상태를 공유할 수 있음
         */
        holder.ivLike.setOnClickListener(v -> {
            if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
            toggleLikeFromList(item, holder);
        });

        holder.tvLikeCount.setOnClickListener(v -> {
            if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
            toggleLikeFromList(item, holder);
        });

        /*
         * 북마크 클릭 — 클릭 즉시 아이콘만 직접 갱신 (notifyItemChanged 없음 = 번쩍거림 방지)
         * 서버 호출은 백그라운드로 보내고, 응답에서는 UI 건드리지 않음 (마이페이지 패턴과 동일)
         */
        holder.ivBookmark.setOnClickListener(v -> {
            Long tipId = getSafeTipId(item);
            boolean wasBookmarked = Boolean.TRUE.equals(bookmarkedStateMap.get(tipId));
            boolean newBookmarked = !wasBookmarked;

            // 즉시 로컬 상태 + 아이콘 토글
            bookmarkedStateMap.put(tipId, newBookmarked);
            holder.ivBookmark.setImageResource(
                    newBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
            );
            holder.ivBookmark.setImageTintList(
                    ColorStateList.valueOf(newBookmarked ? POINT_COLOR : WHITE_COLOR)
            );

            // 서버 호출 — 실패 시에만 사용자에게 안내
            tipRepository.toggleTipBookmark(tipId, new TipRepository.TipBookmarkCallback() {
                @Override
                public void onSuccess(TipBookmarkResponse response) {
                    // 성공이면 별도 처리 없음 (이미 UI는 토글됨)
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(
                            context,
                            "북마크 처리 실패: " + message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });

        /*
         * 우측 상단 점3개 클릭 처리임
         * 수정/삭제 메뉴를 표시함
         */
        holder.tvMore.setOnClickListener(v -> showMoreMenu(holder.tvMore, item));
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    /*
     * 작성자 프로필 이미지를 표시하는 함수임
     * profileImageUrl이 없으면 기본 프로필 아이콘을 표시함
     */
    private void bindProfileImage(TipViewHolder holder, TipItem item) {
        String profileImageUrl = item.getProfileImageUrl();

        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.ivProfile);

            holder.ivProfile.setImageTintList(null);
        } else {
            Glide.with(context).clear(holder.ivProfile);
            holder.ivProfile.setImageResource(R.drawable.ic_profile);
            holder.ivProfile.setImageTintList(null);
        }
    }

    /*
     * 팁 목록 이미지를 표시하는 함수임
     * 로컬 Uri, 서버 이미지 URL, GPS 지도 이미지 URL 순서로 확인함
     */
    private void bindTipImage(TipViewHolder holder, TipItem item) {
        Glide.with(context).clear(holder.ivTipImage);
        holder.ivTipImage.setImageDrawable(null);

        if (item.getImageUris() != null && !item.getImageUris().isEmpty()) {
            Uri firstUri = item.getImageUris().get(0);

            if (firstUri != null) {
                holder.cardTipPhoto.setVisibility(View.VISIBLE);
                holder.ivTipImage.setImageURI(firstUri);
                return;
            }
        }

        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            String firstImageUrl = getFirstValidUrl(item.getImageUrls());

            if (firstImageUrl != null) {
                holder.cardTipPhoto.setVisibility(View.VISIBLE);

                holder.ivTipImage.setBackgroundColor(android.graphics.Color.BLACK);
                Glide.with(context)
                        .load(firstImageUrl)
                        .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                        .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                        .centerCrop()
                        .into(holder.ivTipImage);

                return;
            }
        }

        if (item.isGpsVisible()
                && item.getRouteMapImageUrl() != null
                && !item.getRouteMapImageUrl().trim().isEmpty()) {

            holder.cardTipPhoto.setVisibility(View.VISIBLE);

            holder.ivTipImage.setBackgroundColor(android.graphics.Color.BLACK);
            Glide.with(context)
                    .load(item.getRouteMapImageUrl())
                    .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .centerCrop()
                    .into(holder.ivTipImage);

            return;
        }

        holder.cardTipPhoto.setVisibility(View.GONE);
    }

    /*
     * 문자열 리스트에서 비어있지 않은 첫 번째 URL을 찾는 함수임
     */
    private String getFirstValidUrl(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return imageUrl;
            }
        }

        return null;
    }

    /*
     * 팁 상세 화면으로 이동하는 함수임
     * 현재 상세 화면은 tipId를 받아 상세 API를 호출하는 구조임
     */
    private void openTipDetail(TipItem item) {
        Intent intent = new Intent(context, TipDetailActivity.class);

        intent.putExtra("tipId", item.getTipId());
        intent.putExtra("nickname", item.getNickname());
        intent.putExtra("category", convertCategoryToKorean(item.getCategory()));
        intent.putExtra("title", item.getTitle());
        intent.putExtra("content", item.getContent());
        intent.putExtra("likeCount", getCurrentLikeCount(item));
        intent.putExtra("commentCount", item.getCommentCount());
        intent.putExtra("badgeOwner", item.isBadgeOwner());
        intent.putExtra("gpsVisible", item.isGpsVisible());
        intent.putExtra("createdAt", item.getCreatedAt());
        intent.putExtra("routeMapImageUrl", item.getRouteMapImageUrl());
        intent.putExtra("liked", isLiked(item));
        intent.putExtra("bookmarked", isBookmarked(item));

        /*
         * 이미지 URI 목록이 있으면 상세 화면으로 전달함
         * Uri는 Parcelable이라 Intent로 전달 가능함
         */
        if (item.getImageUris() != null) {
            intent.putParcelableArrayListExtra(
                    "imageUris",
                    new ArrayList<>(item.getImageUris())
            );
        }

        /*
         * 서버 이미지 URL 목록이 있으면 상세 화면으로 전달함
         * 현재 상세 화면은 API로 다시 조회하지만, 추후 fallback 데이터로 사용할 수 있음
         */
        if (item.getImageUrls() != null) {
            intent.putStringArrayListExtra(
                    "imageUrls",
                    new ArrayList<>(item.getImageUrls())
            );
        }

        context.startActivity(intent);
    }

    /*
     * 프로필 화면으로 이동하는 함수임
     * 내 계정이면 MyPageActivity로 이동하고, 남의 계정이면 RunnerPageActivity로 이동함
     */
    private void openProfile(TipItem item) {
        Long writerId = item.getWriterId();
        int myId = TokenManager.getUserId(context);

        if (writerId == null || writerId == myId) {
            Intent intent = new Intent(context, MyPageActivity.class);
            context.startActivity(intent);
            return;
        }

        Intent intent = new Intent(context, RunnerPageActivity.class);
        intent.putExtra("user_id", writerId.intValue());
        intent.putExtra("nickname", item.getNickname());
        context.startActivity(intent);
    }

    /*
     * 팁 목록에서 좋아요 버튼을 눌렀을 때 호출되는 함수임
     * 즉시 UI를 업데이트하고 백그라운드에서 서버 동기화함
     */
    private void toggleLikeFromList(TipItem item, TipViewHolder holder) {
        Long tipId = getSafeTipId(item);

        boolean wasLiked = Boolean.TRUE.equals(likedStateMap.get(tipId));
        boolean newLiked = !wasLiked;
        int oldCount = getCurrentLikeCount(item);
        int newCount = Math.max(0, oldCount + (newLiked ? 1 : -1));

        // 즉시 로컬 상태 + ViewHolder 업데이트 (notifyItemChanged 없음 = 번쩍거림 방지)
        likedStateMap.put(tipId, newLiked);
        likeCountMap.put(tipId, newCount);
        int newColor = newLiked ? POINT_COLOR : WHITE_COLOR;
        holder.ivLike.setImageTintList(ColorStateList.valueOf(newColor));
        holder.tvLikeCount.setTextColor(newColor);
        holder.tvLikeCount.setText(String.valueOf(newCount));

        // 서버 동기화 — 응답값으로 최종 확정
        tipRepository.toggleTipLike(
                tipId,
                new TipRepository.TipLikeCallback() {
                    @Override
                    public void onSuccess(TipLikeResponse response) {
                        // UI는 이미 올바르게 업데이트됨 — 서버 응답 likeCount가 부정확할 수 있어 덮어쓰지 않음
                        likedStateMap.put(tipId, response.isLiked());
                    }

                    @Override
                    public void onFailure(String message) {
                        // 롤백
                        likedStateMap.put(tipId, wasLiked);
                        likeCountMap.put(tipId, oldCount);
                        int c = wasLiked ? POINT_COLOR : WHITE_COLOR;
                        holder.ivLike.setImageTintList(ColorStateList.valueOf(c));
                        holder.tvLikeCount.setTextColor(c);
                        holder.tvLikeCount.setText(String.valueOf(oldCount));
                        Toast.makeText(context, "좋아요 처리 실패: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /*
     * 팁 목록에서 북마크 버튼을 눌렀을 때 호출되는 함수임
     * 서버/목서버 API 호출 후 응답값으로 북마크 상태를 갱신함
     */
    private void toggleBookmarkFromList(TipItem item, int position) {
        Long tipId = getSafeTipId(item);

        tipRepository.toggleTipBookmark(
                tipId,
                new TipRepository.TipBookmarkCallback() {
                    @Override
                    public void onSuccess(TipBookmarkResponse response) {
                        bookmarkedStateMap.put(tipId, response.isBookmarked());

                        notifyItemChanged(position);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                context,
                                "북마크 처리 실패: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 점3개 메뉴 — 서버의 mine 필드로 분기
     */
    private void showMoreMenu(View anchorView, TipItem item) {
        if (item.isMine()) {
            int positionAtClick = tipList.indexOf(item);
            showOwnerMorePopup(anchorView,
                    () -> launchTipEdit(item),
                    () -> confirmAndDeleteTip(item, positionAtClick));
        } else {
            Long writerId = item.getWriterId();
            showReportBlockPopup(anchorView, () -> {
                if (writerId != null) confirmAndBlockUser(writerId.intValue(), "작성자");
            });
        }
    }

    /*
     * 팁 수정 — TipUploadActivity edit 모드로 실행
     */
    private void launchTipEdit(TipItem item) {
        if (item.getTipId() == null) return;
        Intent intent = new Intent(context, TipUploadActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("tipId", item.getTipId().longValue());
        context.startActivity(intent);
    }

    /*
     * 팁 삭제 — 확인 다이얼로그 → API → 목록에서 제거
     */
    private void confirmAndDeleteTip(TipItem item, int position) {
        if (item.getTipId() == null) return;
        DangerConfirmDialog.show(
                context,
                "팁 삭제",
                "정말 이 팁을 삭제하시겠습니까?",
                "삭제",
                () -> tipRepository.deleteTip(item.getTipId(), new TipRepository.TipDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(context, "팁을 삭제했습니다", Toast.LENGTH_SHORT).show();
                        if (position >= 0 && position < tipList.size()) {
                            tipList.remove(position);
                            notifyItemRemoved(position);
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    /*
     * 작성자 차단 — 러너페이지와 동일 스타일 다이얼로그
     */
    private void confirmAndBlockUser(int targetUserId, String label) {
        DangerConfirmDialog.show(
                context,
                "차단하기",
                "상대방의 팁과 댓글을 볼 수 없으며 친구 요청도 불가합니다.\n정말 이 " + label + "을 차단하시겠습니까?",
                "차단",
                () -> {
                    FriendRepository friendRepo =
                            new FriendRepository(
                                    com.neostride.app.common.network.ApiClient.getInstance()
                                            .create(FriendApi.class));
                    FriendRequest req =
                            new FriendRequest(targetUserId, "block");
                    friendRepo.updateStatus(req, success ->
                            Toast.makeText(context,
                                    success ? label + "을 차단했습니다." : "차단에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show());
                }
        );
    }

    /*
     * 본인 글 — 수정/삭제 드롭다운 (흰색, Runnable 콜백)
     */
    private void showOwnerMorePopup(View anchor, Runnable onEdit, Runnable onDelete) {
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_owner_more_options, null);
        int width = (int) (160 * context.getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(25);
        popup.showAsDropDown(anchor, -width + anchor.getWidth(), 8);

        menuView.findViewById(R.id.menu_edit).setOnClickListener(v -> {
            popup.dismiss();
            if (onEdit != null) onEdit.run();
        });
        menuView.findViewById(R.id.menu_delete).setOnClickListener(v -> {
            popup.dismiss();
            if (onDelete != null) onDelete.run();
        });
    }

    /*
     * 남의 글 — 신고/차단 드롭다운 (신고는 토스트 유지)
     */
    private void showReportBlockPopup(View anchor, Runnable onBlock) {
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_runner_more_options, null);
        int width = (int) (160 * context.getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(25);
        popup.showAsDropDown(anchor, -width + anchor.getWidth(), 8);

        menuView.findViewById(R.id.menu_block).setOnClickListener(v -> {
            popup.dismiss();
            if (onBlock != null) onBlock.run();
        });
        menuView.findViewById(R.id.menu_report).setOnClickListener(v -> {
            popup.dismiss();
            Toast.makeText(context, "신고 기능 연결 예정", Toast.LENGTH_SHORT).show();
        });
    }

    //** 팁 목록 카테고리 알림판 스타일을 적용하는 함수임
    // * 배경은 투명하게 유지하고, 글자색과 윤곽선만 카테고리별 네온색으로 변경함
    // */
    private void applyCategoryBadgeStyle(TextView categoryView, String category) {
        if (categoryView == null) {
            return;
        }

        int categoryColor = Color.parseColor(getCategoryColorCode(category));

        categoryView.setText(convertCategoryToKorean(category));
        categoryView.setTextColor(categoryColor);
        categoryView.setTypeface(null, Typeface.BOLD);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);

        // 배경은 채우지 않고 투명하게 유지함
        drawable.setColor(Color.TRANSPARENT);

        // 글자와 같은 색으로 윤곽선만 표시함
        drawable.setStroke(dp(1), categoryColor);

        // 사진처럼 둥근 알림판 형태로 표시함
        drawable.setCornerRadius(dp(14));

        categoryView.setBackground(drawable);
    }



    /*
     * 카테고리별 네온 색상 코드를 반환하는 함수임
     */
    private String getCategoryColorCode(String category) {
        String koreanCategory = convertCategoryToKorean(category);

        switch (koreanCategory) {
            case "전체":
                return "#CCFF00";

            case "자유":
                return "#00E5FF";

            case "훈련":
                return "#FF3DFF";

            case "코스":
                return "#FFB300";

            case "장비":
                return "#00FF85";

            default:
                return "#CCFF00";
        }
    }

    /*
     * 서버 카테고리 값을 화면 표시용 한글 카테고리로 변환하는 함수임
     * 서버에서 이미 한글로 오면 그대로 반환함
     */
    private String convertCategoryToKorean(String category) {
        if (category == null) {
            return "자유";
        }

        switch (category) {
            case "FREE":
                return "자유";

            case "TRAINING":
                return "훈련";

            case "COURSE":
                return "코스";

            case "GEAR":
                return "장비";

            default:
                return category;
        }
    }

    /*
     * 좋아요 상태 확인 함수임
     * TipFragment에서 저장한 서버/목서버 상태를 기준으로 판단함
     */
    private boolean isLiked(TipItem item) {
        Long tipId = getSafeTipId(item);

        if (likedStateMap.containsKey(tipId)) {
            Boolean liked = likedStateMap.get(tipId);
            return liked != null && liked;
        }

        return false;
    }

    /*
     * 북마크 상태 확인 함수임
     * TipFragment에서 저장한 서버/목서버 상태를 기준으로 판단함
     */
    private boolean isBookmarked(TipItem item) {
        Long tipId = getSafeTipId(item);

        if (bookmarkedStateMap.containsKey(tipId)) {
            Boolean bookmarked = bookmarkedStateMap.get(tipId);
            return bookmarked != null && bookmarked;
        }

        return false;
    }

    /*
     * 현재 좋아요 개수를 반환하는 함수임
     * Map에 저장된 최신 값이 있으면 그 값을 사용하고, 없으면 item 기본값을 사용함
     */
    private int getCurrentLikeCount(TipItem item) {
        Long tipId = getSafeTipId(item);

        if (likeCountMap.containsKey(tipId)) {
            Integer count = likeCountMap.get(tipId);
            return count != null ? count : item.getLikeCount();
        }

        return item.getLikeCount();
    }

    /*
     * tipId가 null일 때도 임시로 구분할 수 있도록 안전한 ID를 만드는 함수임
     * 서버 목록에서는 tipId가 있어야 정상임
     */
    private Long getSafeTipId(TipItem item) {
        if (item.getTipId() != null) {
            return item.getTipId();
        }

        return (long) item.hashCode();
    }

    /*
     * null 또는 빈 문자열일 때 기본값을 반환하는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        if (context == null) {
            return value;
        }

        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /*
     * 팁 게시글 1개 ViewHolder 클래스임
     * item_tip.xml 안에 있는 View들을 Java 변수와 연결함
     */
    static class TipViewHolder extends RecyclerView.ViewHolder {

        TextView tvNickname;
        TextView tvCategory;
        TextView tvTitle;
        TextView tvContent;
        TextView tvLikeCount;
        TextView tvCommentCount;
        TextView tvTime;
        TextView tvMore;

        ImageView ivProfile;
        ImageView ivBadge;
        ImageView ivGps;
        ImageView ivTipImage;
        ImageView ivBookmark;
        ImageView ivLike;
        ImageView ivComment;

        CardView cardTipPhoto;

        View layoutBody;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNickname = itemView.findViewById(R.id.tv_tip_nickname);
            tvCategory = itemView.findViewById(R.id.tv_tip_category);
            tvTitle = itemView.findViewById(R.id.tv_tip_title);
            tvContent = itemView.findViewById(R.id.tv_tip_content);
            tvLikeCount = itemView.findViewById(R.id.tv_tip_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_tip_comment_count);
            tvTime = itemView.findViewById(R.id.tv_tip_time);
            tvMore = itemView.findViewById(R.id.tv_tip_more);

            ivProfile = itemView.findViewById(R.id.iv_tip_profile);
            ivBadge = itemView.findViewById(R.id.iv_tip_badge);
            ivGps = itemView.findViewById(R.id.iv_tip_gps);
            ivTipImage = itemView.findViewById(R.id.iv_tip_image);
            ivBookmark = itemView.findViewById(R.id.iv_tip_bookmark);
            ivLike = itemView.findViewById(R.id.iv_tip_like);
            ivComment = itemView.findViewById(R.id.iv_tip_comment);

            cardTipPhoto = itemView.findViewById(R.id.card_tip_photo);

            layoutBody = itemView.findViewById(R.id.layout_tip_body);
        }
    }
}