package com.neostride.app.feature.mypage;

import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.feed.FeedDetailActivity;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.tip.TipDetailActivity;
import com.neostride.app.feature.tip.TipUploadActivity;
import com.neostride.app.feature.tip.model.TipResponse;

import java.util.List;
import java.util.Locale;

/*
 * 마이페이지 "내가 쓴 글" 탭의 통합 어댑터임
 *
 * - TYPE_HEADER: 필터 버튼 헤더 (전체/피드/팁)
 * - TYPE_FEED:   CommunityContentResponse → item_feed.xml
 * - TYPE_TIP:    TipResponse              → item_tip.xml
 */
public class MyPostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_FEED   = 0;
    public static final int TYPE_TIP    = 1;
    public static final int TYPE_HEADER = 2;

    public interface OnFilterClickListener {
        void onFilterClick(String filter); // "all" | "feed" | "tip"
    }

    // ── 통합 아이템 래퍼 ───────────────────────────────────────────────────
    public static class PostItem {
        public final int type;
        public final CommunityContentResponse feed; // TYPE_FEED 일 때만 non-null
        public final TipResponse tip;               // TYPE_TIP  일 때만 non-null

        public PostItem(CommunityContentResponse feed) {
            this.type = TYPE_FEED;
            this.feed = feed;
            this.tip  = null;
        }

        public PostItem(TipResponse tip) {
            this.type = TYPE_TIP;
            this.tip  = tip;
            this.feed = null;
        }
    }

    private final Context context;
    private final List<PostItem> items;
    private final String currentFilter;
    private final OnFilterClickListener filterListener;

    public MyPostsAdapter(Context context, List<PostItem> items, String currentFilter, OnFilterClickListener filterListener) {
        this.context        = context;
        this.items          = items;
        this.currentFilter  = currentFilter;
        this.filterListener = filterListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        return items.get(position - 1).type;
    }

    @Override
    public int getItemCount() {
        return (items != null ? items.size() : 0) + 1; // +1 for header
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_my_posts_filter_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_FEED) {
            View v = inflater.inflate(R.layout.item_feed, parent, false);
            return new FeedViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_tip, parent, false);
            return new TipViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            bindHeader((HeaderViewHolder) holder);
            return;
        }
        PostItem item = items.get(position - 1);
        if (item.type == TYPE_FEED) {
            bindFeed((FeedViewHolder) holder, item.feed);
        } else {
            bindTip((TipViewHolder) holder, item.tip);
        }
    }

    // ── 헤더(필터 버튼) 바인딩 ────────────────────────────────────────────
    private void bindHeader(HeaderViewHolder h) {
        float density = h.itemView.getResources().getDisplayMetrics().density;
        float cornerRadius = 14 * density;

        for (int i = 0; i < 3; i++) {
            TextView btn;
            String filter;
            if      (i == 0) { btn = h.btnAll;  filter = "all";  }
            else if (i == 1) { btn = h.btnFeed; filter = "feed"; }
            else             { btn = h.btnTip;  filter = "tip";  }

            if (btn == null) continue;

            boolean selected = filter.equals(currentFilter);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(cornerRadius);
            if (selected) {
                bg.setColor(Color.parseColor("#CCFF00"));
                btn.setTextColor(Color.BLACK);
            } else {
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke((int)(1.5f * density), Color.parseColor("#444444"));
                btn.setTextColor(Color.parseColor("#AAAAAA"));
            }
            btn.setBackground(bg);

            final String f = filter;
            btn.setOnClickListener(v -> {
                if (filterListener != null) filterListener.onFilterClick(f);
            });
        }
    }

    // ── 피드 아이템 바인딩 ─────────────────────────────────────────────────
    private void bindFeed(FeedViewHolder h, CommunityContentResponse item) {
        // 닉네임
        if (h.tvUsername != null)
            h.tvUsername.setText(item.nickname != null ? item.nickname : "");

        // 프로필 이미지
        if (h.ivProfile != null)
            Glide.with(h.itemView).load(item.profileImageUrl)
                    .circleCrop().placeholder(R.drawable.ic_profile).into(h.ivProfile);

        // 배지
        if (h.ivBadge != null) {
            BadgeTier tier = BadgeTier.fromString(item.badgeTier);
            if (tier.isNone()) {
                h.ivBadge.setVisibility(View.GONE);
            } else {
                h.ivBadge.setVisibility(View.VISIBLE);
                h.ivBadge.setColorFilter(tier.getColor());
            }
        }

        // 북마크 표시 + 토글
        if (h.ivBookmark != null) {
            updateFeedBookmarkIcon(h.ivBookmark, item.isBookmarked);
            h.ivBookmark.setOnClickListener(v -> {
                if (h.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                boolean newState = !item.isBookmarked;
                item.isBookmarked = newState;
                updateFeedBookmarkIcon(h.ivBookmark, newState);
                // 백그라운드 서버 동기화 (실패해도 로컬 상태 유지)
                new com.neostride.app.feature.mypage.repository.MyPageRepository()
                    .toggleBookmark(item.contentId, newState, new retrofit2.Callback<Void>() {
                        @Override public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {}
                        @Override public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
                    });
            });
        }

        // 피드 이미지
        if (h.cardPhoto != null && h.ivImage != null) {
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                h.cardPhoto.setVisibility(View.VISIBLE);
                Glide.with(h.itemView).load(item.imageUrl).centerCrop().into(h.ivImage);
            } else {
                h.cardPhoto.setVisibility(View.GONE);
            }
        }

        // 경로 지도
        if (h.cardRouteMap != null && h.ivRouteMap != null) {
            if (item.routeMapUrl != null && !item.routeMapUrl.isEmpty()) {
                h.cardRouteMap.setVisibility(View.VISIBLE);
                Glide.with(h.itemView).load(item.routeMapUrl).centerCrop().into(h.ivRouteMap);
            } else {
                h.cardRouteMap.setVisibility(View.GONE);
            }
        }

        // 텍스트
        if (h.tvTitle != null) {
            if (item.contentTitle != null && !item.contentTitle.isEmpty()) {
                h.tvTitle.setText(item.contentTitle);
                h.tvTitle.setVisibility(View.VISIBLE);
            } else {
                h.tvTitle.setVisibility(View.GONE);
            }
        }
        if (h.tvContent != null)  h.tvContent.setText(item.contentText);
        if (h.tvTime != null)     h.tvTime.setText(TimeFormatter.format(item.createdAt));

        // 러닝 통계
        if (h.tvDistance != null)
            h.tvDistance.setText(String.format(Locale.getDefault(), "%.1fkm", item.totalDistance));
        if (h.tvDuration != null) {
            int m = item.duration / 60, s = item.duration % 60;
            h.tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", m, s));
        }
        if (h.tvPace != null) {
            int pm = item.pace / 60, ps = item.pace % 60;
            h.tvPace.setText(String.format(Locale.getDefault(), "%d'%02d\"/km", pm, ps));
        }

        // 상호작용 카운트
        if (h.tvLikeCount != null)    h.tvLikeCount.setText(String.valueOf(item.likeCount));
        if (h.tvCommentCount != null) h.tvCommentCount.setText(String.valueOf(item.commentCount));
        if (h.tvTagCount != null)     h.tvTagCount.setText(String.valueOf(item.tagCount));

        // 색상 하이라이트
        int likeColor    = item.isLiked     ? Color.parseColor("#B8FF06") : Color.WHITE;
        int commentColor = item.isCommented ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (h.tvLikeCount != null)    h.tvLikeCount.setTextColor(likeColor);
        if (h.ivLike != null)         h.ivLike.setColorFilter(likeColor);
        if (h.tvCommentCount != null) h.tvCommentCount.setTextColor(commentColor);
        if (h.ivComment != null)      h.ivComment.setColorFilter(commentColor);

        if (h.layoutTag != null) {
            android.graphics.drawable.GradientDrawable tagBg = new android.graphics.drawable.GradientDrawable();
            tagBg.setCornerRadius(h.itemView.getResources().getDisplayMetrics().density * 10);
            tagBg.setColor(item.isTagged ? Color.parseColor("#B8FF06") : Color.parseColor("#E6E6E6"));
            h.layoutTag.setBackground(tagBg);
        }

        // ── 카드 전체 클릭 → 피드 상세 ──
        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FeedDetailActivity.class);
            intent.putExtra("feedId", (long) item.contentId);
            context.startActivity(intent);
        });

        // ── 프로필/닉네임/배지 클릭 → 내가 쓴 글 탭이므로 항상 본인 글 → 무반응 ──
        // (프로필 클릭 리스너 미설정 = 기본 무반응)

        // ── ··· 더보기 버튼 → 항상 본인 글이므로 수정/삭제 ──
        if (h.tvMore != null) {
            h.tvMore.setVisibility(View.VISIBLE);
            h.tvMore.setOnClickListener(v ->
                showOwnerMorePopup(h.tvMore,
                    () -> {
                        Intent intent = new Intent(context, FeedDetailActivity.class);
                        intent.putExtra("feedId", (long) item.contentId);
                        intent.putExtra("autoEdit", true);
                        context.startActivity(intent);
                    },
                    () -> confirmAndDeleteFeed((long) item.contentId, h.getBindingAdapterPosition())
                )
            );
        }
    }

    // ── 팁 아이템 바인딩 ───────────────────────────────────────────────────
    private void bindTip(TipViewHolder h, TipResponse item) {
        // 닉네임
        if (h.tvNickname != null)
            h.tvNickname.setText(item.getNickname() != null ? item.getNickname() : "");

        // 프로필 이미지
        if (h.ivProfile != null)
            Glide.with(h.itemView).load(item.getProfileImageUrl())
                    .circleCrop().placeholder(R.drawable.ic_profile).into(h.ivProfile);

        // 배지
        if (h.ivBadge != null) {
            BadgeTier tier = BadgeTier.fromString(item.getBadgeType());
            if (!item.isBadgeOwned() || tier.isNone()) {
                h.ivBadge.setVisibility(View.GONE);
            } else {
                h.ivBadge.setVisibility(View.VISIBLE);
                h.ivBadge.setColorFilter(tier.getColor());
            }
        }

        // 시간
        if (h.tvTime != null)
            h.tvTime.setText(TimeFormatter.format(item.getCreatedAt()));

        // 북마크 표시 + 토글
        if (h.ivBookmark != null) {
            updateTipBookmarkIcon(h.ivBookmark, item.isBookmarked());
            h.ivBookmark.setOnClickListener(v -> {
                if (h.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                boolean newState = !item.isBookmarked();
                item.setBookmarked(newState);
                updateTipBookmarkIcon(h.ivBookmark, newState);
                // 백그라운드 서버 동기화
                if (item.getTipId() != null) {
                    new com.neostride.app.feature.tip.repository.TipRepository()
                        .toggleTipBookmark(item.getTipId(),
                            new com.neostride.app.feature.tip.repository.TipRepository.TipBookmarkCallback() {
                                @Override public void onSuccess(com.neostride.app.feature.tip.model.TipBookmarkResponse r) {
                                    item.setBookmarked(r.isBookmarked());
                                    updateTipBookmarkIcon(h.ivBookmark, r.isBookmarked());
                                }
                                @Override public void onFailure(String msg) {}
                            });
                }
            });
        }

        // 이미지
        if (h.cardPhoto != null && h.ivImage != null) {
            List<String> urls = item.getImageUrls();
            android.graphics.drawable.ColorDrawable blackBg =
                    new android.graphics.drawable.ColorDrawable(Color.BLACK);
            if (urls != null && !urls.isEmpty()) {
                h.cardPhoto.setVisibility(View.VISIBLE);
                h.ivImage.setBackgroundColor(Color.BLACK);
                Glide.with(h.itemView).load(urls.get(0))
                        .placeholder(blackBg).error(blackBg).centerCrop().into(h.ivImage);
            } else if (item.getRouteMapImageUrl() != null && !item.getRouteMapImageUrl().isEmpty()) {
                h.cardPhoto.setVisibility(View.VISIBLE);
                h.ivImage.setBackgroundColor(Color.BLACK);
                Glide.with(h.itemView).load(item.getRouteMapImageUrl())
                        .placeholder(blackBg).error(blackBg).centerCrop().into(h.ivImage);
            } else {
                h.cardPhoto.setVisibility(View.GONE);
            }
        }

        // 제목 / 카테고리 / 내용
        if (h.tvTitle != null)    h.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        if (h.tvCategory != null) h.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "");
        if (h.tvContent != null)  h.tvContent.setText(item.getContent() != null ? item.getContent() : "");

        // GPS 아이콘
        if (h.ivGps != null)
            h.ivGps.setVisibility(item.isGpsVisible() ? View.VISIBLE : View.GONE);

        // 좋아요 / 댓글
        if (h.tvLikeCount != null)    h.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        if (h.tvCommentCount != null) h.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        int likeColor = item.isLiked() ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (h.ivLike != null)      h.ivLike.setColorFilter(likeColor);
        if (h.tvLikeCount != null) h.tvLikeCount.setTextColor(likeColor);

        // ── 카드 전체 클릭 → 팁 상세 ──
        if (item.getTipId() != null) {
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, TipDetailActivity.class);
                intent.putExtra("tipId", item.getTipId());
                context.startActivity(intent);
            });
        }

        // ── 프로필/닉네임/배지 클릭 → 내가 쓴 글 탭이므로 항상 본인 글 → 무반응 ──
        // (프로필 클릭 리스너 미설정 = 기본 무반응)

        // ── ··· 더보기 버튼 → 항상 본인 글이므로 수정/삭제 ──
        if (h.tvMore != null) {
            h.tvMore.setVisibility(View.VISIBLE);
            h.tvMore.setOnClickListener(v ->
                showOwnerMorePopup(h.tvMore,
                    () -> {
                        if (item.getTipId() == null) return;
                        Intent intent = new Intent(context, TipUploadActivity.class);
                        intent.putExtra("mode", "edit");
                        intent.putExtra("tipId", item.getTipId().longValue());
                        context.startActivity(intent);
                    },
                    () -> confirmAndDeleteTip(item.getTipId(), h.getBindingAdapterPosition())
                )
            );
        }
    }

    // ── 북마크 아이콘 상태 업데이트 헬퍼 ──────────────────────────────────
    private void updateFeedBookmarkIcon(ImageView iv, boolean bookmarked) {
        iv.setImageResource(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        iv.setColorFilter(bookmarked ? Color.parseColor("#B8FF06") : Color.WHITE);
    }

    private void updateTipBookmarkIcon(ImageView iv, boolean bookmarked) {
        iv.setImageResource(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        if (bookmarked) {
            iv.setColorFilter(Color.parseColor("#B8FF06"));
        } else {
            iv.clearColorFilter();
        }
    }

    // ── 본인 글 — 수정/삭제 팝업 ─────────────────────────────────────────
    private void showOwnerMorePopup(View anchor, Runnable onEdit, Runnable onDelete) {
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_owner_more_options, null);
        int width = (int)(160 * context.getResources().getDisplayMetrics().density);
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

    // ── 타인 글 — 신고/차단 팝업 ─────────────────────────────────────────
    private void showReportBlockPopup(View anchor, Runnable onBlock) {
        View menuView = LayoutInflater.from(context).inflate(R.layout.layout_runner_more_options, null);
        int width = (int)(160 * context.getResources().getDisplayMetrics().density);
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

    // ── 피드 삭제 확인 다이얼로그 ────────────────────────────────────────
    private void confirmAndDeleteFeed(long feedId, int adapterPosition) {
        com.neostride.app.feature.community.common.util.DangerConfirmDialog.show(
            context, "피드 삭제", "정말 이 피드를 삭제하시겠습니까?", "삭제",
            () -> new com.neostride.app.feature.feed.repository.FeedRepository(context)
                .deleteFeed(feedId, new com.neostride.app.feature.feed.repository.FeedRepository.RepositoryCallback<Boolean>() {
                    @Override public void onSuccess(Boolean data) {
                        Toast.makeText(context, "피드를 삭제했습니다", Toast.LENGTH_SHORT).show();
                        int dataPos = adapterPosition - 1; // 헤더 offset
                        if (dataPos >= 0 && dataPos < items.size()) {
                            items.remove(dataPos);
                            notifyItemRemoved(adapterPosition);
                        }
                    }
                    @Override public void onError(String message) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    // ── 팁 삭제 확인 다이얼로그 ──────────────────────────────────────────
    private void confirmAndDeleteTip(Long tipId, int adapterPosition) {
        if (tipId == null) return;
        com.neostride.app.feature.community.common.util.DangerConfirmDialog.show(
            context, "팁 삭제", "정말 이 팁을 삭제하시겠습니까?", "삭제",
            () -> new com.neostride.app.feature.tip.repository.TipRepository()
                .deleteTip(tipId, new com.neostride.app.feature.tip.repository.TipRepository.TipDeleteCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(context, "팁을 삭제했습니다", Toast.LENGTH_SHORT).show();
                        int dataPos = adapterPosition - 1; // 헤더 offset
                        if (dataPos >= 0 && dataPos < items.size()) {
                            items.remove(dataPos);
                            notifyItemRemoved(adapterPosition);
                        }
                    }
                    @Override public void onFailure(String message) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    // ── 사용자 차단 확인 다이얼로그 ─────────────────────────────────────
    private void confirmAndBlockUser(int targetUserId, String label) {
        com.neostride.app.feature.community.common.util.DangerConfirmDialog.show(
            context, "차단하기",
            "상대방의 게시글과 댓글을 볼 수 없으며 친구 요청도 불가합니다.\n정말 이 " + label + "을 차단하시겠습니까?",
            "차단",
            () -> {
                com.neostride.app.feature.friend.repository.FriendRepository friendRepo =
                    new com.neostride.app.feature.friend.repository.FriendRepository(
                        com.neostride.app.common.network.ApiClient.getInstance()
                            .create(com.neostride.app.feature.friend.api.FriendApi.class));
                com.neostride.app.feature.friend.model.FriendRequest req =
                    new com.neostride.app.feature.friend.model.FriendRequest(targetUserId, "block");
                friendRepo.updateStatus(req, success ->
                    Toast.makeText(context,
                        success ? label + "을 차단했습니다." : "차단에 실패했습니다.",
                        Toast.LENGTH_SHORT).show());
            }
        );
    }

    // ── HeaderViewHolder ───────────────────────────────────────────────────
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView btnAll, btnFeed, btnTip;

        HeaderViewHolder(@NonNull View v) {
            super(v);
            btnAll  = v.findViewById(R.id.btn_filter_all);
            btnFeed = v.findViewById(R.id.btn_filter_feed);
            btnTip  = v.findViewById(R.id.btn_filter_tip);
        }
    }

    // ── FeedViewHolder ─────────────────────────────────────────────────────
    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTitle, tvContent, tvTime, tvMore;
        TextView tvDistance, tvDuration, tvPace;
        TextView tvLikeCount, tvCommentCount, tvTagCount;
        ImageView ivProfile, ivBadge, ivBookmark, ivImage, ivRouteMap;
        ImageView ivLike, ivComment;
        CardView cardPhoto;
        com.google.android.material.card.MaterialCardView cardRouteMap;
        LinearLayout layoutTag;

        FeedViewHolder(@NonNull View v) {
            super(v);
            tvUsername    = v.findViewById(R.id.tv_feed_username);
            tvTitle       = v.findViewById(R.id.tv_feed_title);
            tvContent     = v.findViewById(R.id.tv_feed_content);
            tvTime        = v.findViewById(R.id.tv_feed_time);
            tvMore        = v.findViewById(R.id.tv_feed_more);
            tvDistance    = v.findViewById(R.id.tv_feed_distance);
            tvDuration    = v.findViewById(R.id.tv_feed_duration);
            tvPace        = v.findViewById(R.id.tv_feed_pace);
            tvLikeCount   = v.findViewById(R.id.tv_feed_like_count);
            tvCommentCount= v.findViewById(R.id.tv_feed_comment_count);
            tvTagCount    = v.findViewById(R.id.tv_feed_tag_count);
            ivProfile     = v.findViewById(R.id.iv_feed_profile);
            ivBadge       = v.findViewById(R.id.iv_feed_badge);
            ivBookmark    = v.findViewById(R.id.iv_feed_bookmark);
            cardPhoto     = v.findViewById(R.id.card_feed_photo);
            ivImage       = v.findViewById(R.id.iv_feed_photo);
            cardRouteMap  = v.findViewById(R.id.card_route_map);
            ivRouteMap    = v.findViewById(R.id.iv_route_map);
            ivLike        = v.findViewById(R.id.iv_feed_like);
            ivComment     = v.findViewById(R.id.ic_comment);
            layoutTag     = v.findViewById(R.id.layout_feed_tag);
        }
    }

    // ── TipViewHolder ──────────────────────────────────────────────────────
    static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView tvNickname, tvTime, tvTitle, tvCategory, tvContent, tvMore;
        TextView tvLikeCount, tvCommentCount;
        ImageView ivProfile, ivBadge, ivBookmark, ivImage, ivGps, ivLike;
        CardView cardPhoto;

        TipViewHolder(@NonNull View v) {
            super(v);
            tvNickname    = v.findViewById(R.id.tv_tip_nickname);
            tvTime        = v.findViewById(R.id.tv_tip_time);
            tvTitle       = v.findViewById(R.id.tv_tip_title);
            tvCategory    = v.findViewById(R.id.tv_tip_category);
            tvContent     = v.findViewById(R.id.tv_tip_content);
            tvMore        = v.findViewById(R.id.tv_tip_more);
            tvLikeCount   = v.findViewById(R.id.tv_tip_like_count);
            tvCommentCount= v.findViewById(R.id.tv_tip_comment_count);
            ivProfile     = v.findViewById(R.id.iv_tip_profile);
            ivBadge       = v.findViewById(R.id.iv_tip_badge);
            ivBookmark    = v.findViewById(R.id.iv_tip_bookmark);
            cardPhoto     = v.findViewById(R.id.card_tip_photo);
            ivImage       = v.findViewById(R.id.iv_tip_image);
            ivGps         = v.findViewById(R.id.iv_tip_gps);
            ivLike        = v.findViewById(R.id.iv_tip_like);
        }
    }
}
