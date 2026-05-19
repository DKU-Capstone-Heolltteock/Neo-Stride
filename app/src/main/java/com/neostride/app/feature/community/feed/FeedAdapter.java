package com.neostride.app.feature.community.feed;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import com.google.android.material.card.MaterialCardView;
import com.neostride.app.R;
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.feed.model.FeedItem;
import com.neostride.app.feature.community.feed.repository.FeedRepository;
import com.neostride.app.feature.community.mypage.MyPageActivity;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * 피드 RecyclerView에 표시될 카드들을 연결하는 Adapter 클래스임
 * 피드 목록 화면에서 좋아요, 북마크, 태그 확인, 상세 이동, 프로필 이동을 처리함
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    // 피드 목록 데이터를 저장하는 리스트임
    private final List<FeedItem> feedItemList;

    // Adapter에서 Activity 이동, Toast 등에 사용할 Context임
    private Context context;

    // 좋아요를 누른 피드 위치를 저장하는 Set임
    private final Set<Integer> likedPositions = new HashSet<>();

    // 북마크를 누른 피드 위치를 저장하는 Set임
    private final Set<Integer> bookmarkedPositions = new HashSet<>();

    /*
     * FeedAdapter 생성자임
     */
    public FeedAdapter(List<FeedItem> feedItemList) {
        this.feedItemList = feedItemList;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_feed, parent, false);

        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedItem item = feedItemList.get(position);

        Long feedId = item.getFeedId();

        // 기본 텍스트 데이터를 화면에 표시함
        holder.tvUsername.setText(item.getUsername());
        holder.tvTime.setText(item.getTime());

        // 작성자 뱃지 보유 시 등급별 색상으로 표시 (NONE이면 숨김)
        if (holder.ivBadge != null) {
            com.neostride.app.feature.badge.model.BadgeTier tier =
                    com.neostride.app.feature.badge.model.BadgeTier.fromString(item.getBadgeType());
            if (!item.isBadgeOwned() || tier.isNone()) {
                holder.ivBadge.setVisibility(View.GONE);
            } else {
                holder.ivBadge.setVisibility(View.VISIBLE);
                holder.ivBadge.setColorFilter(tier.getColor());
            }
        }
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());

        holder.tvTagCount.setText(String.valueOf(item.getTagCount()));
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        holder.tvDistance.setText(item.getDistance());
        holder.tvDuration.setText(item.getDuration());
        holder.tvPace.setText(item.getPace());

        // 프로필 이미지를 설정함
        bindProfileImage(holder, item);

        // 미니 피드뷰에서는 태그 아이콘 클릭 불가 — 상세 페이지에서만 반응함
        holder.tvTagCount.setOnClickListener(null);
        holder.tvTagCount.setClickable(false);

        // 지도 표시 여부를 설정함
        if (item.isMapVisible()
                && item.getRouteMapImageUri() != null
                && !item.getRouteMapImageUri().isEmpty()) {

            holder.cardRouteMap.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(item.getRouteMapImageUri())
                    .centerCrop()
                    .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .into(holder.ivRouteMap);

        } else {
            holder.cardRouteMap.setVisibility(View.GONE);
        }

        // 피드 이미지 표시함
        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            holder.cardFeedPhoto.setVisibility(View.VISIBLE);
            holder.ivPhoto.setVisibility(View.VISIBLE);
            holder.ivPhoto.setBackgroundColor(android.graphics.Color.BLACK);
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrls().get(0))
                    .centerCrop()
                    .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                    .into(holder.ivPhoto);

            LinearLayout.LayoutParams textParams =
                    (LinearLayout.LayoutParams) holder.layoutFeedTextArea.getLayoutParams();
            textParams.setMargins(dp(16), 0, 0, 0);
            holder.layoutFeedTextArea.setLayoutParams(textParams);

        } else {
            holder.cardFeedPhoto.setVisibility(View.GONE);
            holder.ivPhoto.setVisibility(View.GONE);

            LinearLayout.LayoutParams textParams =
                    (LinearLayout.LayoutParams) holder.layoutFeedTextArea.getLayoutParams();
            textParams.setMargins(0, 0, 0, 0);
            holder.layoutFeedTextArea.setLayoutParams(textParams);
        }

        // 좋아요 하이라이트 — item.isLiked() 기반 (마이페이지 패턴)
        int likeColor = item.isLiked() ? Color.parseColor("#B8FF06") : Color.WHITE;
        holder.ivLike.setImageTintList(ColorStateList.valueOf(likeColor));
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvLikeCount.setTextColor(likeColor);

        // 좋아요 클릭 — 즉시 토글, 다른 뷰 재바인딩 없음
        holder.ivLike.setClickable(true);
        holder.ivLike.setEnabled(true);
        holder.ivLike.setOnClickListener(v -> {
            boolean newLiked = !item.isLiked();
            item.setLiked(newLiked);
            int newColor = newLiked ? Color.parseColor("#B8FF06") : Color.WHITE;
            holder.ivLike.setImageTintList(ColorStateList.valueOf(newColor));
            holder.tvLikeCount.setTextColor(newColor);
        });

        // 댓글 하이라이트 — item.isCommented() 기반
        int commentColor = item.isCommented() ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (holder.ivComment != null) {
            holder.ivComment.setImageTintList(ColorStateList.valueOf(commentColor));
        }
        if (holder.tvCommentCount != null) {
            holder.tvCommentCount.setTextColor(commentColor);
        }

        // 태그 하이라이트 — item.isTagged() 기반 (배지 배경색 변경)
        if (holder.layoutFeedTag != null) {
            android.graphics.drawable.GradientDrawable tagBg =
                    new android.graphics.drawable.GradientDrawable();
            tagBg.setCornerRadius(dp(10));
            tagBg.setColor(item.isTagged()
                    ? Color.parseColor("#B8FF06")
                    : Color.parseColor("#E6E6E6"));
            holder.layoutFeedTag.setBackground(tagBg);
        }

        // 북마크 하이라이트 — item.isBookmarked() 기반
        holder.ivBookmark.setImageResource(
                item.isBookmarked() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
        );
        holder.ivBookmark.setImageTintList(
                ColorStateList.valueOf(
                        item.isBookmarked() ? Color.parseColor("#B8FF06") : Color.WHITE
                )
        );

        // 북마크 버튼 클릭 처리함 — 전체 재바인딩 없이 아이콘만 직접 업데이트 (번쩍거림 방지)
        holder.ivBookmark.setClickable(true);
        holder.ivBookmark.setEnabled(true);
        holder.ivBookmark.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            boolean newBookmarked = !item.isBookmarked();
            item.setBookmarked(newBookmarked);

            holder.ivBookmark.setImageResource(
                    newBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
            );
            holder.ivBookmark.setImageTintList(ColorStateList.valueOf(
                    newBookmarked ? Color.parseColor("#B8FF06") : Color.WHITE
            ));
        });

        // 중단 또는 하단 클릭 시 피드 상세 화면으로 이동함
        View.OnClickListener detailClickListener = v -> {
            Intent intent = new Intent(context, FeedDetailActivity.class);

            intent.putExtra("feedId", feedId);

            intent.putExtra("username", item.getUsername());
            intent.putExtra("time", item.getTime());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("content", item.getContent());

            intent.putExtra("tagCount", item.getTagCount());
            intent.putExtra("likeCount", item.getLikeCount());
            intent.putExtra("commentCount", item.getCommentCount());

            intent.putExtra("distance", item.getDistance());
            intent.putExtra("duration", item.getDuration());
            intent.putExtra("pace", item.getPace());

            intent.putExtra("mapVisible", item.isMapVisible());
            intent.putExtra("routeMapImageUri", item.getRouteMapImageUri());

            if (item.getImageUrls() != null) {
                intent.putStringArrayListExtra(
                        "imageUrls",
                        new ArrayList<>(item.getImageUrls())
                );
            }

            context.startActivity(intent);
        };

        holder.layoutFeedBody.setOnClickListener(detailClickListener);
        holder.layoutRecordArea.setOnClickListener(detailClickListener);

        // 프로필 또는 닉네임 클릭 시 내 글이면 마이페이지, 남의 글이면 러너페이지로 이동함
        View.OnClickListener profileClickListener = v -> {
            Long writerId = item.getWriterId();
            int myId = TokenManager.getUserId(context);

            if (writerId == null || writerId == myId) {
                Intent intent = new Intent(context, MyPageActivity.class);
                context.startActivity(intent);
                return;
            }

            Intent intent = new Intent(context, RunnerPageActivity.class);
            intent.putExtra("user_id", writerId.intValue());
            intent.putExtra("nickname", item.getUsername());
            context.startActivity(intent);
        };

        holder.tvUsername.setOnClickListener(profileClickListener);
        holder.ivProfile.setOnClickListener(profileClickListener);

        // 점 세 개 버튼 — 서버가 알려준 mine 필드로 분기 (ID 비교는 mock 환경에서 부정확)
        holder.tvMore.setOnClickListener(v -> {
            if (item.isMine()) {
                showOwnerMorePopup(holder.tvMore,
                        () -> launchFeedEdit(item),
                        () -> confirmAndDeleteFeed(item, holder.getBindingAdapterPosition()));
            } else {
                Long writerId = item.getWriterId();
                showReportBlockPopup(holder.tvMore, () -> {
                    if (writerId != null) confirmAndBlockUser(writerId.intValue(), "작성자");
                });
            }
        });
    }

    /*
     * 피드 수정 — 상세 화면을 편집 모드로 띄움 (사진 picker가 Activity에 등록돼 있어 거기서 처리)
     */
    private void launchFeedEdit(FeedItem item) {
        if (item.getFeedId() == null) return;
        Intent intent = new Intent(context, FeedDetailActivity.class);
        intent.putExtra("feedId", item.getFeedId().longValue());
        intent.putExtra("autoEdit", true);  // 진입 즉시 편집 다이얼로그 열기
        context.startActivity(intent);
    }

    /*
     * 피드 삭제 — 확인 다이얼로그 → API → 목록에서 제거
     */
    private void confirmAndDeleteFeed(FeedItem item, int position) {
        if (item.getFeedId() == null) return;
        FeedRepository repo = new FeedRepository(context);
        DangerConfirmDialog.show(
                context,
                "피드 삭제",
                "정말 이 피드를 삭제하시겠습니까?",
                "삭제",
                () -> repo.deleteFeed(item.getFeedId(), new FeedRepository.RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        Toast.makeText(context, "피드를 삭제했습니다", Toast.LENGTH_SHORT).show();
                        if (position != RecyclerView.NO_POSITION && position < feedItemList.size()) {
                            feedItemList.remove(position);
                            notifyItemRemoved(position);
                        }
                    }

                    @Override
                    public void onError(String message) {
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
                "상대방의 피드와 댓글을 볼 수 없으며 친구 요청도 불가합니다.\n정말 이 " + label + "을 차단하시겠습니까?",
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
     * 본인 글 — 수정/삭제 드롭다운 (흰색 스타일, Runnable 콜백)
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
     * 남의 글 — 신고/차단 드롭다운 (러너페이지 스타일, 신고는 토스트 유지)
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

    @Override
    public int getItemCount() {
        return feedItemList.size();
    }

    /*
     * 새 피드를 리스트 맨 위에 추가하는 함수임
     */
    public void addFeedItem(FeedItem feedItem) {
        feedItemList.add(0, feedItem);
        notifyItemInserted(0);
    }

    /*
     * 프로필 이미지를 원형으로 표시하는 함수임 (마이페이지와 동일한 Glide circleCrop 사용)
     */
    private void bindProfileImage(@NonNull FeedViewHolder holder, FeedItem item) {
        holder.ivProfile.setImageTintList(null);

        String profileImageUrl = item.getProfileImageUrl();

        if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
            holder.ivProfile.setImageResource(R.drawable.ic_profile);
            return;
        }

        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(holder.ivProfile);
    }

    /*
     * 태그된 사람 목록을 보여주는 함수임
     */
    private void showTaggedUserDialog(Long feedId, FeedItem item, Context context) {
        if (item.getTagCount() <= 0) {
            Toast.makeText(
                    context,
                    "태그된 사람이 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FeedRepository feedRepository = new FeedRepository(context);

        feedRepository.getTaggedUsers(
                feedId,
                new FeedRepository.RepositoryCallback<List<com.neostride.app.feature.community.feed.model.TagUser>>() {
                    @Override
                    public void onSuccess(List<com.neostride.app.feature.community.feed.model.TagUser> taggedUsers) {
                        if (taggedUsers == null || taggedUsers.isEmpty()) {
                            Toast.makeText(
                                    context,
                                    "태그된 사람이 없습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        // 닉네임 배열로 변환해서 AlertDialog에 표시
                        String[] userArray = taggedUsers.stream()
                                .map(u -> u.getNickname() != null ? u.getNickname() : "알 수 없음")
                                .toArray(String[]::new);

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("태그된 사람");

                        builder.setItems(userArray, (dialog, which) -> {
                            com.neostride.app.feature.community.feed.model.TagUser selected = taggedUsers.get(which);
                            Intent intent = new Intent(context, MyPageActivity.class);
                            intent.putExtra("username", selected.getNickname());
                            context.startActivity(intent);
                        });

                        builder.setNegativeButton("닫기", null);
                        builder.show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (
                value *
                        context.getResources().getDisplayMetrics().density
                        + 0.5f
        );
    }

    public static class FeedViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername;
        TextView tvTime;
        TextView tvTitle;
        TextView tvContent;

        TextView tvTagCount;
        TextView tvLikeCount;
        TextView tvCommentCount;

        TextView tvDistance;
        TextView tvDuration;
        TextView tvPace;

        TextView tvMore;

        ImageView ivProfile;
        ImageView ivBadge;
        ImageView ivPhoto;
        ImageView ivLike;
        ImageView ivComment;
        ImageView ivBookmark;
        ImageView ivRouteMap;

        CardView cardFeedPhoto;
        MaterialCardView cardRouteMap;

        LinearLayout layoutFeedBody;
        LinearLayout layoutFeedTextArea;
        LinearLayout layoutRecordArea;
        LinearLayout layoutFeedTag;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUsername = itemView.findViewById(R.id.tv_feed_username);
            tvTime = itemView.findViewById(R.id.tv_feed_time);
            tvTitle = itemView.findViewById(R.id.tv_feed_title);
            tvContent = itemView.findViewById(R.id.tv_feed_content);

            tvTagCount = itemView.findViewById(R.id.tv_feed_tag_count);
            tvLikeCount = itemView.findViewById(R.id.tv_feed_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_feed_comment_count);

            tvDistance = itemView.findViewById(R.id.tv_feed_distance);
            tvDuration = itemView.findViewById(R.id.tv_feed_duration);
            tvPace = itemView.findViewById(R.id.tv_feed_pace);

            tvMore = itemView.findViewById(R.id.tv_feed_more);

            ivProfile = itemView.findViewById(R.id.iv_feed_profile);
            ivBadge = itemView.findViewById(R.id.iv_feed_badge);
            ivPhoto = itemView.findViewById(R.id.iv_feed_photo);
            ivLike = itemView.findViewById(R.id.iv_feed_like);
            ivComment = itemView.findViewById(R.id.ic_comment);
            ivBookmark = itemView.findViewById(R.id.iv_feed_bookmark);
            layoutFeedTag = itemView.findViewById(R.id.layout_feed_tag);
            ivRouteMap = itemView.findViewById(R.id.iv_route_map);

            cardFeedPhoto = itemView.findViewById(R.id.card_feed_photo);
            cardRouteMap = itemView.findViewById(R.id.card_route_map);

            layoutFeedBody = itemView.findViewById(R.id.layout_feed_body);
            layoutFeedTextArea = itemView.findViewById(R.id.layout_feed_text_area);
            layoutRecordArea = itemView.findViewById(R.id.layout_record_area);
        }
    }
}