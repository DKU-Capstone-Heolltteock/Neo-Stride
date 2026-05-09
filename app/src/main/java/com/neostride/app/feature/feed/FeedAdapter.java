package com.neostride.app.feature.feed;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.neostride.app.R;
import com.neostride.app.feature.feed.model.FeedItem;
import com.neostride.app.feature.mypage.MyPageActivity;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private final List<FeedItem> feedItemList;
    private Context context;

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

        holder.tvUsername.setText(item.getUsername());
        holder.tvTime.setText("· " + item.getTime());
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());

        holder.tvTagCount.setText(String.valueOf(item.getTagCount()));
        holder.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        holder.tvDistance.setText(item.getDistance());
        holder.tvDuration.setText(item.getDuration());
        holder.tvPace.setText(item.getPace());

        // 지도 표시 여부 설정함
        if (item.isMapVisible()
                && item.getRouteMapImageUri() != null
                && !item.getRouteMapImageUri().isEmpty()) {

            holder.cardRouteMap.setVisibility(View.VISIBLE);
            holder.ivRouteMap.setImageURI(Uri.parse(item.getRouteMapImageUri()));

        } else {
            holder.cardRouteMap.setVisibility(View.GONE);
        }

        // 피드 이미지 표시함
        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            holder.cardFeedPhoto.setVisibility(View.VISIBLE);
            holder.ivPhoto.setVisibility(View.VISIBLE);
            holder.ivPhoto.setImageURI(Uri.parse(item.getImageUrls().get(0)));

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

        // 좋아요 기능 비활성화함
        holder.ivLike.setClickable(false);
        holder.ivLike.setEnabled(false);
        holder.ivLike.setImageTintList(
                ColorStateList.valueOf(Color.WHITE)
        );

        // 북마크 초기 상태 설정함
        holder.isBookmarked = false;
        holder.ivBookmark.setImageTintList(
                ColorStateList.valueOf(Color.WHITE)
        );

        // 북마크 버튼 클릭 처리함
        holder.ivBookmark.setOnClickListener(v -> {
            holder.isBookmarked = !holder.isBookmarked;

            if (holder.isBookmarked) {
                holder.ivBookmark.setImageTintList(
                        ColorStateList.valueOf(Color.parseColor("#B8FF06"))
                );
            } else {
                holder.ivBookmark.setImageTintList(
                        ColorStateList.valueOf(Color.WHITE)
                );
            }
        });

        // 중단 또는 하단 클릭 시 피드 상세 화면으로 이동 예정임
        View.OnClickListener detailClickListener = v -> {
            Toast.makeText(
                    context,
                    "피드 상세 화면 이동 예정",
                    Toast.LENGTH_SHORT
            ).show();

            // 상세 화면 생성 후 아래 코드로 교체하면 됨
            // Intent intent = new Intent(context, FeedDetailActivity.class);
            // context.startActivity(intent);
        };

        holder.layoutFeedBody.setOnClickListener(detailClickListener);
        holder.layoutRecordArea.setOnClickListener(detailClickListener);

        // 닉네임 클릭 시 마이페이지 이동함
        holder.tvUsername.setOnClickListener(v -> {
            Intent intent = new Intent(context, MyPageActivity.class);
            context.startActivity(intent);
        });

        // 프로필 클릭 시 마이페이지 이동함
        holder.ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, MyPageActivity.class);
            context.startActivity(intent);
        });

        // 점 세 개 버튼 클릭 시 수정/삭제 메뉴 표시함
        holder.tvMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.tvMore);

            popupMenu.getMenu().add("수정");
            popupMenu.getMenu().add("삭제");

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                String title = menuItem.getTitle().toString();

                if (title.equals("수정")) {
                    Toast.makeText(context, "수정 클릭", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (title.equals("삭제")) {
                    Toast.makeText(context, "삭제 클릭", Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return feedItemList.size();
    }

    public void addFeedItem(FeedItem feedItem) {
        feedItemList.add(0, feedItem);
        notifyItemInserted(0);
    }

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
        ImageView ivPhoto;
        ImageView ivLike;
        ImageView ivBookmark;
        ImageView ivRouteMap;

        CardView cardFeedPhoto;
        MaterialCardView cardRouteMap;

        LinearLayout layoutFeedBody;
        LinearLayout layoutFeedTextArea;
        LinearLayout layoutRecordArea;

        boolean isBookmarked = false;

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
            ivPhoto = itemView.findViewById(R.id.iv_feed_photo);
            ivLike = itemView.findViewById(R.id.iv_feed_like);
            ivBookmark = itemView.findViewById(R.id.iv_feed_bookmark);
            ivRouteMap = itemView.findViewById(R.id.iv_route_map);

            cardFeedPhoto = itemView.findViewById(R.id.card_feed_photo);
            cardRouteMap = itemView.findViewById(R.id.card_route_map);

            layoutFeedBody = itemView.findViewById(R.id.layout_feed_body);
            layoutFeedTextArea = itemView.findViewById(R.id.layout_feed_text_area);
            layoutRecordArea = itemView.findViewById(R.id.layout_record_area);
        }
    }
}