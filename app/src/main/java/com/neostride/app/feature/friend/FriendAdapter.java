package com.neostride.app.feature.friend;

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

        // 1. 기본 정보 세팅
        holder.tvNickname.setText(item.nickname);
        holder.tvFriendCount.setText("친구 " + item.friendCount);

        // 2. 배지 티어 색상 적용 (BadgeTier Enum 활용)
        BadgeTier tier = BadgeTier.fromString(item.badgeTier);
        holder.ivBadge.setColorFilter(tier.getColor());

        // 3. 현재 탭 상태에 따라 우측 버튼 텍스트 변경
        switch (currentStatus) {
            case "sent":
                holder.btnAction.setText("- 요청 취소");
                break;
            case "received":
                holder.btnAction.setText("+ 수락");
                break;
            case "blocked":
                holder.btnAction.setText("차단 해제");
                break;
            default: // "friends" 탭
                holder.btnAction.setText("프로필 보기");
                break;
        }

        // 4. 버튼 클릭 리스너 (필요시 구현)
        holder.btnAction.setOnClickListener(v -> {
            // 여기에 친구 요청 취소/수락 등의 로직 연결 가능
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