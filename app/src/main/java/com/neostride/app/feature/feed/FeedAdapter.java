package com.neostride.app.feature.feed;

import android.app.AlertDialog;
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
import com.neostride.app.feature.feed.repository.FeedRepository;
import com.neostride.app.feature.mypage.MyPageActivity;

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
        holder.tvTime.setText("· " + item.getTime());
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

        // 태그 버튼 클릭 시 Repository를 통해 태그 목록을 가져옴
        holder.tvTagCount.setOnClickListener(
                v -> showTaggedUserDialog(
                        feedId,
                        item,
                        holder.itemView.getContext()
                )
        );

        // 지도 표시 여부를 설정함
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

        // 좋아요 상태를 설정함
        boolean isLiked = likedPositions.contains(position);
        holder.ivLike.setImageTintList(
                ColorStateList.valueOf(
                        isLiked ? Color.parseColor("#B8FF06") : Color.WHITE
                )
        );

        int currentLikeCount = item.getLikeCount();

        if (isLiked) {
            holder.tvLikeCount.setText(String.valueOf(currentLikeCount + 1));
        } else {
            holder.tvLikeCount.setText(String.valueOf(currentLikeCount));
        }

        // 좋아요 버튼 클릭 처리함
        holder.ivLike.setClickable(true);
        holder.ivLike.setEnabled(true);
        holder.ivLike.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (likedPositions.contains(adapterPosition)) {
                likedPositions.remove(adapterPosition);
            } else {
                likedPositions.add(adapterPosition);
            }

            notifyItemChanged(adapterPosition);
        });

        // 북마크 상태를 설정함
        boolean isBookmarked = bookmarkedPositions.contains(position);
        holder.ivBookmark.setImageTintList(
                ColorStateList.valueOf(
                        isBookmarked ? Color.parseColor("#B8FF06") : Color.WHITE
                )
        );

        // 북마크 버튼 클릭 처리함
        holder.ivBookmark.setClickable(true);
        holder.ivBookmark.setEnabled(true);
        holder.ivBookmark.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (bookmarkedPositions.contains(adapterPosition)) {
                bookmarkedPositions.remove(adapterPosition);
            } else {
                bookmarkedPositions.add(adapterPosition);
            }

            notifyItemChanged(adapterPosition);
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

        // 프로필 또는 닉네임 클릭 시 해당 유저 프로필/마이페이지로 이동함
        View.OnClickListener profileClickListener = v -> {
            Intent intent = new Intent(context, MyPageActivity.class);
            intent.putExtra("username", item.getUsername());
            context.startActivity(intent);
        };

        holder.tvUsername.setOnClickListener(profileClickListener);
        holder.ivProfile.setOnClickListener(profileClickListener);

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

    /*
     * 새 피드를 리스트 맨 위에 추가하는 함수임
     */
    public void addFeedItem(FeedItem feedItem) {
        feedItemList.add(0, feedItem);
        notifyItemInserted(0);
    }

    /*
     * 프로필 이미지를 설정하는 함수임
     */
    private void bindProfileImage(@NonNull FeedViewHolder holder, FeedItem item) {
        holder.ivProfile.setImageTintList(null);

        String profileImageUrl = item.getProfileImageUrl();

        if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
            holder.ivProfile.setImageResource(R.drawable.ic_profile);
            return;
        }

        if (profileImageUrl.startsWith("content://")
                || profileImageUrl.startsWith("file://")
                || profileImageUrl.startsWith("android.resource://")) {
            holder.ivProfile.setImageURI(Uri.parse(profileImageUrl));
            return;
        }

        // 서버 URL 이미지는 나중에 Glide로 연결하면 됨
        holder.ivProfile.setImageResource(R.drawable.ic_profile);
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
                new FeedRepository.RepositoryCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> taggedUsers) {
                        if (taggedUsers == null || taggedUsers.isEmpty()) {
                            Toast.makeText(
                                    context,
                                    "태그된 사람이 없습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        String[] userArray = taggedUsers.toArray(new String[0]);

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("태그된 사람");

                        builder.setItems(userArray, (dialog, which) -> {
                            String selectedUsername = userArray[which];

                            Intent intent = new Intent(context, MyPageActivity.class);
                            intent.putExtra("username", selectedUsername);
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
        ImageView ivPhoto;
        ImageView ivLike;
        ImageView ivBookmark;
        ImageView ivRouteMap;

        CardView cardFeedPhoto;
        MaterialCardView cardRouteMap;

        LinearLayout layoutFeedBody;
        LinearLayout layoutFeedTextArea;
        LinearLayout layoutRecordArea;

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