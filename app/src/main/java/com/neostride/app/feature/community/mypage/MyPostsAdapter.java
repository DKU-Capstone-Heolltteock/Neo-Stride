package com.neostride.app.feature.community.mypage;

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
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
import com.neostride.app.feature.community.feed.model.FeedLikeResponse;
import com.neostride.app.feature.community.feed.repository.FeedRepository;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.mypage.repository.MyPageRepository;
import com.neostride.app.feature.community.tip.model.TipBookmarkResponse;
import com.neostride.app.feature.community.tip.model.TipLikeResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;
import com.neostride.app.feature.community.feed.FeedDetailActivity;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.tip.TipDetailActivity;
import com.neostride.app.feature.community.tip.TipUploadActivity;
import com.neostride.app.feature.community.tip.model.TipResponse;

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

    // 삭제 완료 시 Activity에 알리는 콜백 (type: TYPE_FEED 또는 TYPE_TIP)
    public interface OnPostDeletedListener {
        void onPostDeleted(int type);
    }

    // 북마크 해제로 아이템이 제거될 때 Activity에 알리는 콜백
    public interface OnBookmarkRemovedListener {
        void onBookmarkRemoved();
    }

    // 북마크 상태 변경 시 Activity에 알리는 콜백 (추가: true, 해제: false)
    public interface OnBookmarkChangedListener {
        void onBookmarkChanged(boolean isNowBookmarked);
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
    private final boolean isOwner;    // true=내 글(수정/삭제), false=타인 글(신고/차단)
    private final boolean hideHeader; // true=활동탭 모드(헤더 없음, 아이템별 본인 글 감지)
    private Runnable onBlockAction;   // isOwner=false 일 때 차단 버튼 콜백
    private OnPostDeletedListener onPostDeletedListener;     // 삭제 완료 콜백
    private boolean removeOnUnbookmark = false;              // 북마크 해제 시 목록에서 즉시 제거
    private OnBookmarkRemovedListener bookmarkRemovedListener;
    private OnBookmarkChangedListener bookmarkChangedListener;

    public void setOnBlockAction(Runnable onBlockAction) {
        this.onBlockAction = onBlockAction;
    }

    public void setOnPostDeletedListener(OnPostDeletedListener listener) {
        this.onPostDeletedListener = listener;
    }

    public void setRemoveOnUnbookmark(boolean remove) {
        this.removeOnUnbookmark = remove;
    }

    public void setOnBookmarkRemovedListener(OnBookmarkRemovedListener listener) {
        this.bookmarkRemovedListener = listener;
    }

    public void setOnBookmarkChangedListener(OnBookmarkChangedListener listener) {
        this.bookmarkChangedListener = listener;
    }

    // 내가 쓴 글 탭용 생성자 (헤더 있음, isOwner 일괄 설정)
    public MyPostsAdapter(Context context, List<PostItem> items, String currentFilter, OnFilterClickListener filterListener) {
        this(context, items, currentFilter, filterListener, true);
    }

    public MyPostsAdapter(Context context, List<PostItem> items, String currentFilter, OnFilterClickListener filterListener, boolean isOwner) {
        this.context        = context;
        this.items          = items;
        this.currentFilter  = currentFilter;
        this.filterListener = filterListener;
        this.isOwner        = isOwner;
        this.hideHeader     = false;
    }

    // 활동 탭용 생성자 (헤더 없음, 아이템별 본인 글 감지)
    public MyPostsAdapter(Context context, List<PostItem> items) {
        this.context        = context;
        this.items          = items;
        this.hideHeader     = true;
        this.currentFilter  = "all";
        this.filterListener = null;
        this.isOwner        = false;
    }

    @Override
    public int getItemViewType(int position) {
        if (hideHeader) return items.get(position).type;
        if (position == 0) return TYPE_HEADER;
        return items.get(position - 1).type;
    }

    @Override
    public int getItemCount() {
        if (hideHeader) return items != null ? items.size() : 0;
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
        if (!hideHeader && position == 0) {
            bindHeader((HeaderViewHolder) holder);
            return;
        }
        PostItem item = hideHeader ? items.get(position) : items.get(position - 1);
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
                int clickPos = h.getBindingAdapterPosition();
                if (clickPos == RecyclerView.NO_POSITION) return;
                boolean newState = !item.isBookmarked;
                item.isBookmarked = newState;
                updateFeedBookmarkIcon(h.ivBookmark, newState);
                // 북마크 목록 탭에서 해제 시 → 즉시 제거 + Activity 카운트 갱신 알림
                if (removeOnUnbookmark && !newState) {
                    int idx = hideHeader ? clickPos : clickPos - 1;
                    if (idx >= 0 && idx < items.size()) {
                        items.remove(idx);
                        notifyItemRemoved(clickPos);
                        if (bookmarkRemovedListener != null) bookmarkRemovedListener.onBookmarkRemoved();
                    }
                }
                // 북마크 추가/해제 시 카운트 갱신 알림
                if (bookmarkChangedListener != null) bookmarkChangedListener.onBookmarkChanged(newState);
                // 백그라운드 서버 동기화 (실패해도 로컬 상태 유지)
                new MyPageRepository()
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
            h.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", item.totalDistance));
        if (h.tvDuration != null) {
            int m = item.duration / 60, s = item.duration % 60;
            h.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
        }
        if (h.tvPace != null) {
            if (item.pace > 0) {
                int pm = item.pace / 60, ps = item.pace % 60;
                h.tvPace.setText(String.format(Locale.getDefault(), "%d:%02d/km", pm, ps));
            } else {
                h.tvPace.setText("-");
            }
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

        // 미니뷰에서는 좋아요 비활성화 — 좋아요는 상세 페이지에서만 가능함
        if (h.ivLike != null)      { h.ivLike.setClickable(false); h.ivLike.setEnabled(false); h.ivLike.setOnClickListener(null); }
        if (h.tvLikeCount != null) { h.tvLikeCount.setClickable(false); h.tvLikeCount.setOnClickListener(null); }

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

        // ── 프로필/닉네임/배지 클릭 → 상대방 글이면 RunnerPageActivity로 이동 ──
        int myId = com.neostride.app.common.network.TokenManager.getUserId(context);
        View.OnClickListener feedProfileClick = (item.userId != myId) ? v -> {
            Intent intent = new Intent(context, com.neostride.app.feature.community.runnerpage.RunnerPageActivity.class);
            intent.putExtra("user_id", item.userId);
            intent.putExtra("nickname", item.nickname);
            context.startActivity(intent);
        } : null;
        if (h.ivProfile != null)  h.ivProfile.setOnClickListener(feedProfileClick);
        if (h.tvUsername != null) h.tvUsername.setOnClickListener(feedProfileClick);
        if (h.ivBadge != null)    h.ivBadge.setOnClickListener(feedProfileClick);

        // ── ··· 더보기 버튼 ──
        if (h.tvMore != null) {
            h.tvMore.setVisibility(View.VISIBLE);
            // isOwner=true(내가 쓴 글 탭): 항상 본인 글 / isOwner=false(활동 탭): 아이템별 감지
            boolean itemMine = isOwner || (item.userId == myId);
            if (itemMine) {
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
            } else {
                h.tvMore.setOnClickListener(v ->
                    showReportBlockPopup(h.tvMore,
                        () -> confirmAndBlockUser(item.userId, "작성자")));
            }
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
                int clickPos = h.getBindingAdapterPosition();
                if (clickPos == RecyclerView.NO_POSITION) return;
                boolean newState = !item.isBookmarked();
                item.setBookmarked(newState);
                updateTipBookmarkIcon(h.ivBookmark, newState);
                // 북마크 목록 탭에서 해제 시 → 즉시 제거 + Activity 카운트 갱신 알림
                if (removeOnUnbookmark && !newState) {
                    int idx = hideHeader ? clickPos : clickPos - 1;
                    if (idx >= 0 && idx < items.size()) {
                        items.remove(idx);
                        notifyItemRemoved(clickPos);
                        if (bookmarkRemovedListener != null) bookmarkRemovedListener.onBookmarkRemoved();
                    }
                }
                // 북마크 추가/해제 시 카운트 갱신 알림
                if (bookmarkChangedListener != null) bookmarkChangedListener.onBookmarkChanged(newState);
                // 백그라운드 서버 동기화
                if (item.getTipId() != null) {
                    new TipRepository()
                        .toggleTipBookmark(item.getTipId(),
                            new TipRepository.TipBookmarkCallback() {
                                @Override public void onSuccess(TipBookmarkResponse r) {
                                    item.setBookmarked(r.isBookmarked());
                                    updateTipBookmarkIcon(h.ivBookmark, r.isBookmarked());
                                }
                                @Override public void onFailure(String msg) {}
                            });
                }
            });
        }

        // 이미지 (GPS 경로 지도는 미니뷰에서 표시하지 않음 — 상세 페이지에서만 표시)
        if (h.cardPhoto != null && h.ivImage != null) {
            List<String> urls = item.getImageUrls();
            android.graphics.drawable.ColorDrawable blackBg =
                    new android.graphics.drawable.ColorDrawable(Color.BLACK);
            if (urls != null && !urls.isEmpty()) {
                h.cardPhoto.setVisibility(View.VISIBLE);
                h.ivImage.setBackgroundColor(Color.BLACK);
                Glide.with(h.itemView).load(urls.get(0))
                        .placeholder(blackBg).error(blackBg).centerCrop().into(h.ivImage);
            } else {
                h.cardPhoto.setVisibility(View.GONE);
            }
        }

        // 제목 / 카테고리 / 내용
        if (h.tvTitle != null)    h.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        if (h.tvCategory != null) applyTipCategoryBadgeStyle(h.tvCategory, item.getCategory());
        if (h.tvContent != null)  h.tvContent.setText(item.getContent() != null ? item.getContent() : "");

        // GPS 아이콘
        if (h.ivGps != null)
            h.ivGps.setVisibility(item.isGpsVisible() ? View.VISIBLE : View.GONE);

        // 좋아요 / 댓글
        if (h.tvLikeCount != null)    h.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
        if (h.tvCommentCount != null) h.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        int likeColor    = item.isLiked()     ? Color.parseColor("#B8FF06") : Color.WHITE;
        int commentColor = item.isCommented() ? Color.parseColor("#B8FF06") : Color.WHITE;
        if (h.ivLike != null)         h.ivLike.setColorFilter(likeColor);
        if (h.tvLikeCount != null)    h.tvLikeCount.setTextColor(likeColor);
        if (h.ivComment != null)      h.ivComment.setColorFilter(commentColor);
        if (h.tvCommentCount != null) h.tvCommentCount.setTextColor(commentColor);

        // 미니뷰에서는 좋아요 비활성화 — 좋아요는 상세 페이지에서만 가능함
        if (h.ivLike != null)      { h.ivLike.setClickable(false); h.ivLike.setEnabled(false); h.ivLike.setOnClickListener(null); }
        if (h.tvLikeCount != null) { h.tvLikeCount.setClickable(false); h.tvLikeCount.setOnClickListener(null); }

        // ── 카드 전체 클릭 → 팁 상세 ──
        if (item.getTipId() != null) {
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, TipDetailActivity.class);
                intent.putExtra("tipId", item.getTipId());
                context.startActivity(intent);
            });
        }

        // ── 프로필/닉네임/배지 클릭 → 상대방 글이면 RunnerPageActivity로 이동 ──
        int tipMyId = com.neostride.app.common.network.TokenManager.getUserId(context);
        boolean tipMine = item.getWriterId() != null && item.getWriterId() == tipMyId;
        View.OnClickListener tipProfileClick = !tipMine ? v -> {
            if (item.getWriterId() == null) return;
            Intent intent = new Intent(context, com.neostride.app.feature.community.runnerpage.RunnerPageActivity.class);
            intent.putExtra("user_id", item.getWriterId().intValue());
            intent.putExtra("nickname", item.getNickname());
            context.startActivity(intent);
        } : null;
        if (h.ivProfile != null)  h.ivProfile.setOnClickListener(tipProfileClick);
        if (h.tvNickname != null) h.tvNickname.setOnClickListener(tipProfileClick);
        if (h.ivBadge != null)    h.ivBadge.setOnClickListener(tipProfileClick);

        // ── ··· 더보기 버튼 ──
        if (h.tvMore != null) {
            h.tvMore.setVisibility(View.VISIBLE);
            // isOwner=true(내가 쓴 글 탭): 항상 본인 글 / isOwner=false(활동 탭): 아이템별 감지
            boolean itemMine = isOwner || (item.getWriterId() != null && item.getWriterId() == tipMyId);
            if (itemMine) {
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
            } else {
                h.tvMore.setOnClickListener(v ->
                    showReportBlockPopup(h.tvMore,
                        item.getWriterId() != null
                            ? () -> confirmAndBlockUser(item.getWriterId().intValue(), "작성자")
                            : onBlockAction));
            }
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
        DangerConfirmDialog.show(
            context, "피드 삭제", "정말 이 피드를 삭제하시겠습니까?", "삭제",
            () -> new FeedRepository(context)
                .deleteFeed(feedId, new FeedRepository.RepositoryCallback<Boolean>() {
                    @Override public void onSuccess(Boolean data) {
                        Toast.makeText(context, "피드를 삭제했습니다", Toast.LENGTH_SHORT).show();
                        // adapterPosition은 팝업 dismiss 후 stale(-1)할 수 있으므로 ID로 탐색
                        for (int i = 0; i < items.size(); i++) {
                            PostItem pi = items.get(i);
                            if (pi.type == TYPE_FEED && pi.feed != null && pi.feed.contentId == feedId) {
                                items.remove(i);
                                notifyItemRemoved(hideHeader ? i : i + 1);
                                if (onPostDeletedListener != null)
                                    onPostDeletedListener.onPostDeleted(TYPE_FEED);
                                break;
                            }
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
        DangerConfirmDialog.show(
            context, "팁 삭제", "정말 이 팁을 삭제하시겠습니까?", "삭제",
            () -> new TipRepository()
                .deleteTip(tipId, new TipRepository.TipDeleteCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(context, "팁을 삭제했습니다", Toast.LENGTH_SHORT).show();
                        // tipId로 탐색하여 삭제 (stale position 방지)
                        for (int i = 0; i < items.size(); i++) {
                            PostItem pi = items.get(i);
                            if (pi.type == TYPE_TIP && pi.tip != null && tipId.equals(pi.tip.getTipId())) {
                                items.remove(i);
                                notifyItemRemoved(hideHeader ? i : i + 1);
                                if (onPostDeletedListener != null)
                                    onPostDeletedListener.onPostDeleted(TYPE_TIP);
                                break;
                            }
                        }
                    }
                    @Override public void onFailure(String message) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    // ── 팁 카테고리 배지 스타일 적용 ─────────────────────────────────────
    private void applyTipCategoryBadgeStyle(TextView view, String category) {
        if (view == null) return;
        int color = android.graphics.Color.parseColor(tipCategoryColorCode(category));
        view.setText(tipCategoryToKorean(category));
        view.setTextColor(color);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setColor(android.graphics.Color.TRANSPARENT);
        bg.setStroke(dp(1), color);
        bg.setCornerRadius(dp(14));
        view.setBackground(bg);
    }

    private static String tipCategoryToKorean(String category) {
        if (category == null) return "자유";
        switch (category) {
            case "FREE":     return "자유";
            case "TRAINING": return "훈련";
            case "COURSE":   return "코스";
            case "GEAR":     return "장비";
            default:         return category;
        }
    }

    private static String tipCategoryColorCode(String category) {
        switch (tipCategoryToKorean(category)) {
            case "자유":  return "#00E5FF";
            case "훈련":  return "#FF3DFF";
            case "코스":  return "#FFB300";
            case "장비":  return "#00FF85";
            default:     return "#CCFF00";
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ── 사용자 차단 확인 다이얼로그 ─────────────────────────────────────
    private void confirmAndBlockUser(int targetUserId, String label) {
        DangerConfirmDialog.show(
            context, "차단하기",
            "차단하면 상대방의 글과 댓글이 나에게 보이지 않으며,\n상대방 글에 남긴 좋아요·북마크·댓글은 삭제됩니다.\n정말 이 " + label + "을 차단하시겠습니까?",
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
        ImageView ivProfile, ivBadge, ivBookmark, ivImage, ivGps, ivLike, ivComment;
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
            ivComment     = v.findViewById(R.id.iv_tip_comment);
        }
    }
}
