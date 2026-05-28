package com.neostride.app.feature.community.friend;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.community.friend.model.FriendResponse;
import java.util.ArrayList;
import java.util.List;


//  친구 목록 RecyclerView 어댑터
//  <p>
//  - 탭 상태(friends/sent/received/blocked/none/per_item)에 따라 우측 액션 버튼을 다르게 표시한다.
//  - "per_item" 모드에서는 각 항목의 {@link com.neostride.app.feature.friend.model.FriendResponse#status}를 사용한다.

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    // 액션 버튼 클릭 콜백 (action: "cancel"/"accept"/"reject"/"unblock"/"delete"/"request")
    public interface OnActionClickListener {
        void onAction(int userId, String action, String nickname);
    }

    // 항목 전체 클릭 콜백 (러너 페이지 이동용)
    public interface OnItemClickListener {
        void onItemClick(int userId, String nickname, String status);
    }

    private List<FriendResponse> friendList = new ArrayList<>();
    private String currentStatus = "friends";
    private OnActionClickListener actionListener;
    private OnItemClickListener itemClickListener;

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setFriendList(List<FriendResponse> newList, String status) {
        this.friendList = newList;
        this.currentStatus = status;
        notifyDataSetChanged();
    }

    // per_item 모드에서 특정 유저의 status를 변경하고 해당 아이템만 리바인드
    public void updateItemStatus(int userId, String newStatus) {
        for (int i = 0; i < friendList.size(); i++) {
            if (friendList.get(i).userId == userId) {
                friendList.get(i).status = newStatus;
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendResponse item = friendList.get(position);

        // 1. 데이터 및 기본 UI 초기화 (정렬 및 잔상 제거)
        holder.tvNickname.setText(item.nickname);
        holder.tvFriendCount.setText("친구 " + item.friendCount);

        // 프로필 이미지 로드
        holder.ivProfile.setImageTintList(null);
        if (item.profileImageUrl != null && !item.profileImageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.ivProfile);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.ivProfile);
            holder.ivProfile.setImageResource(R.drawable.ic_profile);
        }

        holder.btnAction.setIncludeFontPadding(false);
        holder.btnAction.setGravity(Gravity.CENTER);
        holder.btnAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        holder.btnAction.setCompoundDrawablePadding(0);

        // 거절 버튼 기본 초기화 (received 탭 외에는 숨김)
        holder.btnActionSecondary.setVisibility(View.GONE);
        holder.btnActionSecondary.setOnClickListener(null);

        // 2. 배지 티어 색상 적용 (언랭이면 숨김)
        BadgeTier tier = BadgeTier.fromString(item.badgeTier);
        if (tier == null || tier.isNone()) {
            holder.ivBadge.setVisibility(View.GONE);
        } else {
            holder.ivBadge.setVisibility(View.VISIBLE);
            holder.ivBadge.setColorFilter(tier.getColor());
        }

        // 3. 본인인 경우 "본인" 버튼으로 표시하고 클릭 불가 처리
        int myId = TokenManager.getUserId(holder.itemView.getContext());
        if (item.userId == myId) {
            holder.btnAction.setText("본인");
            holder.btnAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            GradientDrawable selfBg = new GradientDrawable();
            selfBg.setCornerRadius(holder.itemView.getResources().getDisplayMetrics().density * 20);
            selfBg.setColor(Color.parseColor("#444444"));
            holder.btnAction.setBackground(selfBg);
            holder.btnAction.setTextColor(Color.parseColor("#AAAAAA"));
            holder.btnAction.setEnabled(false);
            holder.btnAction.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
            return;
        }

        // 4. 현재 탭 상태에 따라 우측 버튼 설정
        // "per_item" 모드: 각 아이템의 item.status를 사용 (남의 친구 목록 화면 등에서 사용)
        String resolvedStatus = "per_item".equals(currentStatus)
                ? (item.status != null ? item.status : "none")
                : currentStatus;

        switch (resolvedStatus) {
            case "sent":
                holder.btnAction.setText("요청 취소");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);

                android.graphics.drawable.Drawable returnIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_return);
                if (returnIcon != null) {
                    int returnSize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    returnIcon = returnIcon.mutate();
                    returnIcon.setBounds(0, 0, returnSize, returnSize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(returnIcon, Color.BLACK);
                    holder.btnAction.setCompoundDrawables(returnIcon, null, null, null);
                    holder.btnAction.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                break;

            case "received":
                // 거절 버튼 (빨간 스타일 + ic_friend_deny 아이콘)
                holder.btnActionSecondary.setVisibility(View.VISIBLE);
                holder.btnActionSecondary.setText("거절");
                holder.btnActionSecondary.setTextColor(Color.WHITE);
                GradientDrawable rejectBg = new GradientDrawable();
                rejectBg.setCornerRadius(holder.itemView.getResources().getDisplayMetrics().density * 20);
                rejectBg.setColor(Color.parseColor("#FF4444"));
                holder.btnActionSecondary.setBackground(rejectBg);

                android.graphics.drawable.Drawable denyIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_deny);
                if (denyIcon != null) {
                    int denySize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    denyIcon = denyIcon.mutate();
                    denyIcon.setBounds(0, 0, denySize, denySize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(denyIcon, Color.WHITE);
                    holder.btnActionSecondary.setCompoundDrawables(denyIcon, null, null, null);
                    holder.btnActionSecondary.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                holder.btnActionSecondary.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onAction(item.userId, "reject", item.nickname);
                });

                // 수락 버튼 (ic_friend_accept 아이콘)
                holder.btnAction.setText("수락");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);

                android.graphics.drawable.Drawable acceptIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_accept);
                if (acceptIcon != null) {
                    int acceptSize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    acceptIcon = acceptIcon.mutate();
                    acceptIcon.setBounds(0, 0, acceptSize, acceptSize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(acceptIcon, Color.BLACK);
                    holder.btnAction.setCompoundDrawables(acceptIcon, null, null, null);
                    holder.btnAction.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                break;

            case "blocked":
                holder.btnAction.setText("차단 해제");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);

                android.graphics.drawable.Drawable unlockIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_unlock);
                if (unlockIcon != null) {
                    int iconSize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    unlockIcon = unlockIcon.mutate();
                    unlockIcon.setBounds(0, 0, iconSize, iconSize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(unlockIcon, Color.BLACK);
                    holder.btnAction.setCompoundDrawables(unlockIcon, null, null, null);
                    holder.btnAction.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                break;

            case "none": // 친구 관계 없음 → 친구요청 버튼
                holder.btnAction.setText("친구요청");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);

                android.graphics.drawable.Drawable reqIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_request);
                if (reqIcon != null) {
                    int reqSize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    reqIcon = reqIcon.mutate();
                    reqIcon.setBounds(0, 0, reqSize, reqSize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(reqIcon, Color.BLACK);
                    holder.btnAction.setCompoundDrawables(reqIcon, null, null, null);
                    holder.btnAction.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                break;

            default: // "friends" (친구 목록) 탭
                holder.btnAction.setText("친구 삭제");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4444")));
                holder.btnAction.setTextColor(Color.WHITE);

                // 아이콘 크기를 텍스트 높이(16dp)에 맞게 수동 설정
                android.graphics.drawable.Drawable removeIcon = androidx.core.content.ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_friend_remove);
                if (removeIcon != null) {
                    int iconSize = (int) (holder.itemView.getResources().getDisplayMetrics().density * 14);
                    removeIcon = removeIcon.mutate();
                    removeIcon.setBounds(0, 0, iconSize, iconSize);
                    androidx.core.graphics.drawable.DrawableCompat.setTint(removeIcon, Color.WHITE);
                    holder.btnAction.setCompoundDrawables(removeIcon, null, null, null);
                    holder.btnAction.setCompoundDrawablePadding((int)(holder.itemView.getResources().getDisplayMetrics().density * 4));
                }
                break;
        }

        // 5. 아이템 전체 클릭 → 러너 페이지
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(item.userId, item.nickname, resolvedStatus);
        });

        // 6. 버튼 클릭 리스너 (버튼은 별도 처리, 이벤트 전파 차단)
        holder.btnAction.setOnClickListener(v -> {
            if (actionListener == null) return;
            switch (resolvedStatus) {
                case "sent":     actionListener.onAction(item.userId, "cancel",  item.nickname); break;
                case "received": actionListener.onAction(item.userId, "accept",  item.nickname); break;
                case "blocked":  actionListener.onAction(item.userId, "unblock", item.nickname); break;
                case "none":     actionListener.onAction(item.userId, "request", item.nickname); break;
                default:         actionListener.onAction(item.userId, "delete",  item.nickname); break;
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, ivBadge;
        TextView tvNickname, tvFriendCount, btnAction, btnActionSecondary;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile          = itemView.findViewById(R.id.iv_profile);
            ivBadge            = itemView.findViewById(R.id.iv_badge);
            tvNickname         = itemView.findViewById(R.id.tv_nickname);
            tvFriendCount      = itemView.findViewById(R.id.tv_friend_count);
            btnAction          = itemView.findViewById(R.id.btn_action);
            btnActionSecondary = itemView.findViewById(R.id.btn_action_secondary);
        }
    }
}