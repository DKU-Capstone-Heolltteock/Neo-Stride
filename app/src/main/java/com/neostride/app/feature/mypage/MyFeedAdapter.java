package com.neostride.app.feature.mypage;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.mypage.repository.MyPageRepository;

import java.util.List;
import java.util.Locale;


//  마이페이지 피드 목록 RecyclerView 어댑터
//  <p>
//  - 피드별 프로필·배지·이미지·경로 지도·통계(좋아요·댓글·태그·북마크)를 바인딩한다.
//  - 북마크 토글 시 서버 API를 호출하여 상태를 동기화한다.

public class MyFeedAdapter extends RecyclerView.Adapter<MyFeedAdapter.ViewHolder> {

    // 프로필 이미지·닉네임 클릭 콜백 (러너 페이지 이동용)
    public interface OnProfileClickListener {
        void onProfileClick(int userId, String nickname);
    }

    private List<CommunityContentResponse> feedList;
    private OnProfileClickListener profileClickListener;

    public MyFeedAdapter(List<CommunityContentResponse> list) {
        this.feedList = list;
    }

    public void setOnProfileClickListener(OnProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityContentResponse item = feedList.get(position);

        // 1. 작성자 정보 세팅 (닉네임 + 프로필 이미지)
        if (holder.tvUsername != null) {
            holder.tvUsername.setText(item.nickname != null ? item.nickname : "Nul");
        }

        if (holder.ivFeedProfile != null) {
            Glide.with(holder.itemView.getContext())
                    .load(item.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.ivFeedProfile);
        }

        // 프로필 이미지 / 닉네임 클릭 → 러너페이지 이동
        View.OnClickListener profileClick = v -> {
            if (profileClickListener != null) {
                profileClickListener.onProfileClick(item.userId, item.nickname);
            }
        };
        if (holder.ivFeedProfile != null) holder.ivFeedProfile.setOnClickListener(profileClick);
        if (holder.tvUsername != null) holder.tvUsername.setOnClickListener(profileClick);

        // 2. 배지 아이콘 (언랭이면 숨김, 그 외 → 티어 색상)
        if (holder.ivFeedBadge != null) {
            BadgeTier tier = BadgeTier.fromString(item.badgeTier);
            if (tier.isNone()) {
                holder.ivFeedBadge.setVisibility(View.GONE);
            } else {
                holder.ivFeedBadge.setVisibility(View.VISIBLE);
                holder.ivFeedBadge.setColorFilter(tier.getColor());
            }
        }

        // 4. 북마크 상태 세팅
        if (item.isBookmarked) {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            holder.ivBookmark.setColorFilter(Color.parseColor("#B8FF06"));
        } else {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark);
            holder.ivBookmark.setColorFilter(Color.WHITE);
        }

        // 5. 북마크 클릭 리스너
        holder.ivBookmark.setOnClickListener(v -> {
            item.isBookmarked = !item.isBookmarked;
            if (item.isBookmarked) {
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                holder.ivBookmark.setColorFilter(Color.parseColor("#B8FF06"));
            } else {
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark);
                holder.ivBookmark.setColorFilter(Color.WHITE);
            }

            MyPageRepository repository = new MyPageRepository();
            repository.toggleBookmark(item.contentId, item.isBookmarked, new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                    if (response.isSuccessful()) Log.d("Bookmark", "성공");
                }
                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
            });
        });

        // 6. [핵심] 게시물 이미지 동적 노출 (이미지 없으면 영역 제거)
        if (holder.ivFeedImage != null && holder.cardFeedPhoto != null) {
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                holder.cardFeedPhoto.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .centerCrop()
                        .into(holder.ivFeedImage);
            } else {
                holder.cardFeedPhoto.setVisibility(View.GONE); // 사진 영역 제거 -> 텍스트가 확장됨
            }
        }

        // 5. 경로 지도 표시 (route_map_url 있을 때만 VISIBLE)
        if (holder.cardRouteMap != null && holder.ivRouteMap != null) {
            if (item.routeMapUrl != null && !item.routeMapUrl.isEmpty()) {
                holder.cardRouteMap.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(item.routeMapUrl)
                        .centerCrop()
                        .into(holder.ivRouteMap);
            } else {
                holder.cardRouteMap.setVisibility(View.GONE);
            }
        }

        // 7. 텍스트 정보 세팅
        if (holder.tvContent != null) holder.tvContent.setText(item.contentText);

        if (holder.tvDistance != null) {
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1fkm", item.totalDistance));
        }

        if (holder.tvDuration != null) {
            int min = item.duration / 60;
            int sec = item.duration % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", min, sec));
        }

        if (holder.tvPace != null) {
            int pMin = item.pace / 60;
            int pSec = item.pace % 60;
            holder.tvPace.setText(String.format(Locale.getDefault(), "%d'%02d\"/km", pMin, pSec));
        }

        if (holder.tvTime != null && item.createdAt != null) {
            String date = item.createdAt.length() >= 10 ? item.createdAt.substring(0, 10).replace("-", ".") : item.createdAt;
            holder.tvTime.setText(date);
        }

        // 8. 통계 수치 세팅
        if (holder.tvTitle != null) {
            if (item.contentTitle != null && !item.contentTitle.isEmpty()) {
                holder.tvTitle.setText(item.contentTitle);
                holder.tvTitle.setVisibility(View.VISIBLE);
            } else {
                holder.tvTitle.setVisibility(View.GONE);
            }
        }
        // 태그 하이라이트 (태그됐을 때 배경 형광 초록으로 변경)
        if (holder.tvTagCount != null) holder.tvTagCount.setText(String.valueOf(item.tagCount));
        if (holder.layoutFeedTag != null) {
            android.graphics.drawable.GradientDrawable tagBg = new android.graphics.drawable.GradientDrawable();
            tagBg.setCornerRadius(holder.itemView.getResources().getDisplayMetrics().density * 10);
            tagBg.setColor(item.isTagged ? Color.parseColor("#B8FF06") : Color.parseColor("#E6E6E6"));
            holder.layoutFeedTag.setBackground(tagBg);
        }

        // 좋아요 하이라이트
        if (holder.tvLikeCount != null) holder.tvLikeCount.setText(String.valueOf(item.likeCount));
        int likeColor = item.isLiked ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (holder.tvLikeCount != null) holder.tvLikeCount.setTextColor(likeColor);
        if (holder.ivFeedLike != null) holder.ivFeedLike.setColorFilter(likeColor);

        // 댓글 하이라이트
        if (holder.tvCommentCount != null) holder.tvCommentCount.setText(String.valueOf(item.commentCount));
        int commentColor = item.isCommented ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (holder.tvCommentCount != null) holder.tvCommentCount.setTextColor(commentColor);
        if (holder.ivFeedComment != null) holder.ivFeedComment.setColorFilter(commentColor);
    }

    @Override
    public int getItemCount() {
        return feedList != null ? feedList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDistance, tvDuration, tvPace, tvTime, tvUsername;
        TextView tvTitle, tvTagCount, tvLikeCount, tvCommentCount;
        ImageView ivBookmark, ivFeedImage, ivFeedProfile, ivFeedBadge, ivFeedLike, ivFeedComment, ivFeedTag;
        android.widget.LinearLayout layoutFeedTag;
        androidx.cardview.widget.CardView cardFeedPhoto; // CardView 선언
        com.google.android.material.card.MaterialCardView cardRouteMap;
        ImageView ivRouteMap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername     = itemView.findViewById(R.id.tv_feed_username);
            ivBookmark     = itemView.findViewById(R.id.iv_feed_bookmark);

            // CardView와 내부 ImageView 연결
            cardFeedPhoto  = itemView.findViewById(R.id.card_feed_photo);
            ivFeedImage    = itemView.findViewById(R.id.iv_feed_photo);

            ivFeedProfile  = itemView.findViewById(R.id.iv_feed_profile);
            ivFeedBadge    = itemView.findViewById(R.id.iv_feed_badge);

            cardRouteMap   = itemView.findViewById(R.id.card_route_map);
            ivRouteMap     = itemView.findViewById(R.id.iv_route_map);

            tvContent      = itemView.findViewById(R.id.tv_feed_content);
            tvDistance     = itemView.findViewById(R.id.tv_feed_distance);
            tvDuration     = itemView.findViewById(R.id.tv_feed_duration);
            tvPace         = itemView.findViewById(R.id.tv_feed_pace);
            tvTime         = itemView.findViewById(R.id.tv_feed_time);
            tvTitle        = itemView.findViewById(R.id.tv_feed_title);
            tvTagCount     = itemView.findViewById(R.id.tv_feed_tag_count);
            tvLikeCount    = itemView.findViewById(R.id.tv_feed_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_feed_comment_count);
            ivFeedLike     = itemView.findViewById(R.id.iv_feed_like);
            ivFeedComment  = itemView.findViewById(R.id.ic_comment);
            ivFeedTag      = itemView.findViewById(R.id.iv_feed_tag);
            layoutFeedTag  = itemView.findViewById(R.id.layout_feed_tag);
        }
    }
}