package com.neostride.app.feature.feed;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.feed.model.FeedItem;

import java.util.List;

// 피드 데이터를 RecyclerView에 연결하는 어댑터 클래스임
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    // 피드 데이터를 담는 리스트임
    private final List<FeedItem> feedItemList;

    // 생성자에서 피드 리스트를 전달받음
    public FeedAdapter(List<FeedItem> feedItemList) {
        this.feedItemList = feedItemList;
    }

    // item_feed.xml을 RecyclerView 아이템으로 연결함
    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed, parent, false);

        return new FeedViewHolder(view);
    }

    // 실제 데이터를 화면에 표시함
    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedItem item = feedItemList.get(position);

        // 텍스트 데이터 연결함
        holder.tvUsername.setText(item.getUsername());
        holder.tvTime.setText("· " + item.getTime());
        holder.tvTitle.setText(item.getTitle());
        holder.tvTagCount.setText(String.valueOf(item.getTagCount()));
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));
        holder.tvDistance.setText(item.getDistance());
        holder.tvDuration.setText(item.getDuration());
        holder.tvPace.setText(item.getPace());

        // 이미지가 둘 다 없으면 이미지 영역 자체를 숨김
        if (item.getPhotoResId() == 0 && item.getRouteMapResId() == 0) {
            holder.layoutImages.setVisibility(View.GONE);
        } else {
            holder.layoutImages.setVisibility(View.VISIBLE);

            // 사용자 사진이 있으면 보여주고, 없으면 해당 ImageView만 숨김
            if (item.getPhotoResId() != 0) {
                holder.ivPhoto.setVisibility(View.VISIBLE);
                holder.ivPhoto.setImageResource(item.getPhotoResId());
            } else {
                holder.ivPhoto.setVisibility(View.GONE);
            }

            // 경로 이미지가 있으면 보여주고, 없으면 해당 ImageView만 숨김
            if (item.getRouteMapResId() != 0) {
                holder.ivRouteMap.setVisibility(View.VISIBLE);
                holder.ivRouteMap.setImageResource(item.getRouteMapResId());
            } else {
                holder.ivRouteMap.setVisibility(View.GONE);
            }
        }

        // RecyclerView 재사용 문제를 막기 위해 좋아요 색상을 기본값으로 초기화함
        holder.isLiked = false;
        holder.ivLike.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        // 좋아요 아이콘 클릭 시 형광색/흰색으로 토글함
        holder.ivLike.setOnClickListener(v -> {
            holder.isLiked = !holder.isLiked;

            if (holder.isLiked) {
                holder.ivLike.setImageTintList(
                        ColorStateList.valueOf(Color.parseColor("#CCFF00"))
                );
            } else {
                holder.ivLike.setImageTintList(
                        ColorStateList.valueOf(Color.WHITE)
                );
            }
        });
    }

    // 피드 개수를 반환함
    @Override
    public int getItemCount() {
        return feedItemList.size();
    }

    // item_feed.xml 내부 View들을 연결하는 ViewHolder 클래스임
    public static class FeedViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername, tvTime, tvTitle;
        TextView tvTagCount, tvLikeCount, tvCommentCount;
        TextView tvDistance, tvDuration, tvPace;

        ImageView ivPhoto, ivRouteMap, ivLike;

        View layoutImages;

        boolean isLiked = false;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUsername = itemView.findViewById(R.id.tv_feed_username);
            tvTime = itemView.findViewById(R.id.tv_feed_time);
            tvTitle = itemView.findViewById(R.id.tv_feed_title);
            tvTagCount = itemView.findViewById(R.id.tv_feed_tag_count);
            tvLikeCount = itemView.findViewById(R.id.tv_feed_like_count);
            tvCommentCount = itemView.findViewById(R.id.tv_feed_comment_count);
            tvDistance = itemView.findViewById(R.id.tv_feed_distance);
            tvDuration = itemView.findViewById(R.id.tv_feed_duration);
            tvPace = itemView.findViewById(R.id.tv_feed_pace);

            ivPhoto = itemView.findViewById(R.id.iv_feed_photo);
            ivRouteMap = itemView.findViewById(R.id.iv_feed_route_map);
            ivLike = itemView.findViewById(R.id.iv_feed_like);

            layoutImages = itemView.findViewById(R.id.layout_images);
        }
    }
}