package com.neostride.app.feature.tip;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.feature.mypage.MyPageActivity;
import com.neostride.app.feature.tip.model.TipItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // Activity 이동에 사용할 Context 객체임
    private Context context;

    /*
     * 좋아요 상태를 임시로 저장하는 Set임
     * 추후 좋아요 API가 연결되면 서버 응답값으로 대체하면 됨
     */
    private final Set<Long> likedTipIds = new HashSet<>();

    /*
     * 북마크 상태를 임시로 저장하는 Set임
     * 추후 북마크 API가 연결되면 서버 응답값으로 대체하면 됨
     */
    private final Set<Long> bookmarkedTipIds = new HashSet<>();

    /*
     * TipAdapter 생성자임
     * Fragment 또는 Activity에서 전달받은 팁 목록을 저장함
     */
    public TipAdapter(ArrayList<TipItem> tipList) {
        this.tipList = tipList;
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
        holder.tvCategory.setText(convertCategoryToKorean(item.getCategory()));
        holder.tvTitle.setText(getSafeText(item.getTitle(), ""));
        holder.tvContent.setText(getSafeText(item.getContent(), ""));
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        // 작성 시간이 있으면 표시하고, 없으면 기본값으로 표시함
        if (item.getCreatedAt() != null && !item.getCreatedAt().trim().isEmpty()) {
            holder.tvTime.setText(item.getCreatedAt());
        } else {
            holder.tvTime.setText("방금 전");
        }

        // 배지 표시 여부를 설정함
        holder.ivBadge.setVisibility(item.isBadgeOwner() ? View.VISIBLE : View.GONE);

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
         * 프사/닉네임 클릭 시 마이페이지로 이동함
         */
        holder.ivProfile.setOnClickListener(v -> openProfile(item));
        holder.tvNickname.setOnClickListener(v -> openProfile(item));

        /*
         * 좋아요 클릭 처리임
         * 현재는 API 없이 로컬에서 색상과 숫자만 변경함
         */
        holder.ivLike.setOnClickListener(v -> toggleLike(holder, item));
        holder.tvLikeCount.setOnClickListener(v -> toggleLike(holder, item));

        /*
         * 북마크 클릭 처리임
         * 현재는 API 없이 로컬에서 색상만 변경함
         */
        holder.ivBookmark.setOnClickListener(v -> toggleBookmark(holder, item));

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

                Glide.with(context)
                        .load(firstImageUrl)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .centerCrop()
                        .into(holder.ivTipImage);

                return;
            }
        }

        if (item.isGpsVisible()
                && item.getRouteMapImageUrl() != null
                && !item.getRouteMapImageUrl().trim().isEmpty()) {

            holder.cardTipPhoto.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(item.getRouteMapImageUrl())
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
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
        intent.putExtra("likeCount", item.getLikeCount());
        intent.putExtra("commentCount", item.getCommentCount());
        intent.putExtra("badgeOwner", item.isBadgeOwner());
        intent.putExtra("gpsVisible", item.isGpsVisible());
        intent.putExtra("createdAt", item.getCreatedAt());
        intent.putExtra("routeMapImageUrl", item.getRouteMapImageUrl());

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
     * 현재 프로젝트의 MyPageActivity로 이동함
     */
    private void openProfile(TipItem item) {
        Intent intent = new Intent(context, MyPageActivity.class);
        intent.putExtra("username", item.getNickname());
        intent.putExtra("nickname", item.getNickname());
        context.startActivity(intent);
    }

    /*
     * 좋아요 상태를 토글하는 함수임
     */
    private void toggleLike(TipViewHolder holder, TipItem item) {
        Long tipId = getSafeTipId(item);

        boolean liked;

        if (likedTipIds.contains(tipId)) {
            likedTipIds.remove(tipId);
            liked = false;
        } else {
            likedTipIds.add(tipId);
            liked = true;
        }

        int currentCount = parseCount(holder.tvLikeCount.getText().toString());

        if (liked) {
            currentCount++;
            holder.ivLike.setImageTintList(ColorStateList.valueOf(POINT_COLOR));
        } else {
            currentCount = Math.max(0, currentCount - 1);
            holder.ivLike.setImageTintList(ColorStateList.valueOf(WHITE_COLOR));
        }

        holder.tvLikeCount.setText(String.valueOf(currentCount));
    }

    /*
     * 북마크 상태를 토글하는 함수임
     */
    private void toggleBookmark(TipViewHolder holder, TipItem item) {
        Long tipId = getSafeTipId(item);

        boolean bookmarked;

        if (bookmarkedTipIds.contains(tipId)) {
            bookmarkedTipIds.remove(tipId);
            bookmarked = false;
        } else {
            bookmarkedTipIds.add(tipId);
            bookmarked = true;
        }

        holder.ivBookmark.setImageTintList(
                ColorStateList.valueOf(bookmarked ? POINT_COLOR : WHITE_COLOR)
        );
    }

    /*
     * 점3개 메뉴를 표시하는 함수임
     */
    private void showMoreMenu(View anchorView, TipItem item) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);

        popupMenu.getMenu().add("수정");
        popupMenu.getMenu().add("삭제");

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String menuTitle = menuItem.getTitle().toString();

            if (menuTitle.equals("수정")) {
                Toast.makeText(context, "팁 수정 기능 연결 예정", Toast.LENGTH_SHORT).show();

                /*
                 * 추후 수정 화면 연결 시 사용하면 됨
                 *
                 * Intent intent = new Intent(context, TipUploadActivity.class);
                 * intent.putExtra("mode", "edit");
                 * intent.putExtra("tipId", item.getTipId());
                 * context.startActivity(intent);
                 */

                return true;
            }

            if (menuTitle.equals("삭제")) {
                Toast.makeText(context, "팁 삭제 기능 연결 예정", Toast.LENGTH_SHORT).show();

                /*
                 * 추후 삭제 API 연결 시 사용하면 됨
                 *
                 * DELETE /api/community/tips/{tipId}
                 */

                return true;
            }

            return false;
        });

        popupMenu.show();
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
     */
    private boolean isLiked(TipItem item) {
        return likedTipIds.contains(getSafeTipId(item));
    }

    /*
     * 북마크 상태 확인 함수임
     */
    private boolean isBookmarked(TipItem item) {
        return bookmarkedTipIds.contains(getSafeTipId(item));
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
     * 문자열 숫자를 int로 변환하는 함수임
     */
    private int parseCount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
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