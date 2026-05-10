package com.neostride.app.feature.friend;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.friend.model.FriendResponse;
import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<FriendResponse> friendList = new ArrayList<>();
    private String currentStatus = "friends"; // 현재 선택된 탭 상태 저장용

    // 데이터 갱신 메서드
    public void setFriendList(List<FriendResponse> newList, String status) {
        this.friendList = newList;
        this.currentStatus = status;
        notifyDataSetChanged();
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

        holder.btnAction.setIncludeFontPadding(false); // 텍스트 상단 쏠림 방지
        holder.btnAction.setGravity(Gravity.CENTER);  // 수직/수평 중앙 정렬
        holder.btnAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // 아이콘 초기화
        holder.btnAction.setCompoundDrawablePadding(0);

        // 2. 배지 티어 색상 적용
        BadgeTier tier = BadgeTier.fromString(item.badgeTier);
        if (tier != null) {
            holder.ivBadge.setColorFilter(tier.getColor());
        } else {
            holder.ivBadge.setColorFilter(Color.GRAY);
        }

        // 3. 현재 탭 상태에 따라 우측 버튼 설정
        switch (currentStatus) {
            case "sent":
                holder.btnAction.setText("- 요청 취소");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null); // 원래 형광색으로 복원
                holder.btnAction.setTextColor(Color.BLACK);
                break;

            case "received":
                holder.btnAction.setText("+ 수락");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);
                break;

            case "blocked":
                holder.btnAction.setText("차단 해제");
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(null);
                holder.btnAction.setTextColor(Color.BLACK);
                break;

            default: // "friends" (친구 목록) 탭
                holder.btnAction.setText("친구 삭제");

                // 빨간색 배경 적용
                holder.btnAction.setBackgroundResource(R.drawable.bg_badge_btn);
                holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4444")));

                // 흰색 텍스트 및 흰색 아이콘 설정
                holder.btnAction.setTextColor(Color.WHITE);
                holder.btnAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_friend_remove, 0, 0, 0);
                holder.btnAction.setCompoundDrawablePadding(8);
                holder.btnAction.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
                break;
        }

        // 4. 버튼 클릭 리스너
        holder.btnAction.setOnClickListener(v -> {
            // 현재 status에 따른 액션 처리 (예: 삭제 확인 팝업)
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, ivBadge;
        TextView tvNickname, tvFriendCount, btnAction;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            ivBadge = itemView.findViewById(R.id.iv_badge);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            tvFriendCount = itemView.findViewById(R.id.tv_friend_count);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}