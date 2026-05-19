package com.neostride.app.feature.community.search;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;
import com.neostride.app.feature.community.search.model.SearchUserResponse;

import java.util.List;

/*
 * 프로필/친구 검색 결과 RecyclerView 어댑터 클래스임
 * item_friend.xml을 사용해 친구 화면과 동일한 카드 디자인으로 표시함
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder> {

    private final List<SearchUserResponse> userList;
    private Context context;

    /*
     * PROFILE 또는 FRIEND 값으로 사용함
     */
    private final String type;

    private FriendRepository friendRepository;

    public SearchUserAdapter(List<SearchUserResponse> userList, String type) {
        this.userList = userList;
        this.type = type;
    }

    @NonNull
    @Override
    public SearchUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (friendRepository == null) {
            friendRepository = new FriendRepository(ApiClient.getInstance().create(FriendApi.class));
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new SearchUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchUserViewHolder holder, int position) {
        SearchUserResponse item = userList.get(position);

        /*
         * 닉네임
         */
        holder.tvNickname.setText(getSafeText(item.getNickname(), "알 수 없음"));

        /*
         * 친구 수
         */
        holder.tvFriendCount.setText("친구 " + item.getFriendCount() + "명");

        /*
         * 프로필 이미지 — Glide로 원형 로드, 없으면 기본 아이콘
         */
        if (item.getProfileImageUrl() != null && !item.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getProfileImageUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_profile);
        }

        /*
         * 배지 아이콘 — 등급 없으면 GONE, 있으면 등급 색상 적용
         */
        BadgeTier tier = BadgeTier.fromString(item.getBadgeTier());
        if (tier == null || tier.isNone()) {
            holder.ivBadge.setVisibility(View.GONE);
        } else {
            holder.ivBadge.setVisibility(View.VISIBLE);
            holder.ivBadge.setColorFilter(tier.getColor());
        }

        /*
         * 버튼 아이콘 잔상 제거 (RecyclerView 재사용 시 이전 아이콘 남는 문제 방지)
         */
        holder.btnAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        holder.btnAction.setCompoundDrawablePadding(0);

        /*
         * 우측 액션 버튼 — 나와의 관계(status)에 따라 텍스트/색상 결정
         */
        applyActionButton(holder.btnAction, item.getStatus());

        /*
         * received 상태일 때만 '거절' 보조 버튼을 표시함 — FriendAdapter와 동일한 빨간 스타일
         */
        holder.btnActionSecondary.setCompoundDrawables(null, null, null, null);
        holder.btnActionSecondary.setCompoundDrawablePadding(0);

        if ("received".equals(item.getStatus())) {
            holder.btnActionSecondary.setVisibility(View.VISIBLE);
            holder.btnActionSecondary.setText("거절");
            holder.btnActionSecondary.setTextColor(Color.WHITE);
            holder.btnActionSecondary.setIncludeFontPadding(false);
            holder.btnActionSecondary.setGravity(android.view.Gravity.CENTER);

            GradientDrawable rejectBg = new GradientDrawable();
            rejectBg.setShape(GradientDrawable.RECTANGLE);
            rejectBg.setCornerRadius(context.getResources().getDisplayMetrics().density * 20);
            rejectBg.setColor(Color.parseColor("#FF4444"));
            holder.btnActionSecondary.setBackground(rejectBg);

            float density = context.getResources().getDisplayMetrics().density;
            int iconSize = (int) (density * 14);
            Drawable denyIcon = ContextCompat.getDrawable(context, R.drawable.ic_friend_deny);
            if (denyIcon != null) {
                denyIcon = denyIcon.mutate();
                denyIcon.setBounds(0, 0, iconSize, iconSize);
                DrawableCompat.setTint(denyIcon, Color.WHITE);
                holder.btnActionSecondary.setCompoundDrawables(denyIcon, null, null, null);
                holder.btnActionSecondary.setCompoundDrawablePadding((int) (density * 4));
            }

            holder.btnActionSecondary.setOnClickListener(v ->
                    handleFriendAction(item, "reject"));
        } else {
            holder.btnActionSecondary.setVisibility(View.GONE);
        }

        /*
         * 메인 버튼 클릭 — status에 따라 친구요청/취소/수락/삭제
         */
        holder.btnAction.setOnClickListener(v -> {
            String status = item.getStatus() != null ? item.getStatus() : "none";
            switch (status) {
                case "none":
                    handleFriendAction(item, "request");
                    break;
                case "sent":
                    handleFriendAction(item, "cancel");
                    break;
                case "received":
                    handleFriendAction(item, "accept");
                    break;
                case "friends":
                    showDeleteFriendDialog(item);
                    break;
            }
        });

        /*
         * 아이템 클릭 → RunnerPageActivity
         */
        holder.itemView.setOnClickListener(v -> {
            if (item.getUserId() == null) return;
            Intent intent = new Intent(context, RunnerPageActivity.class);
            intent.putExtra("user_id", item.getUserId().intValue());
            intent.putExtra("nickname", item.getNickname());
            intent.putExtra("is_friend", item.isFriend());
            context.startActivity(intent);
        });
    }

    /*
     * 친구 관계 변경 API를 호출하고 성공 시 버튼 상태를 갱신하는 함수임
     */
    private void handleFriendAction(SearchUserResponse item, String action) {
        if (item.getUserId() == null || friendRepository == null) return;
        int targetId = item.getUserId().intValue();

        friendRepository.updateStatus(new FriendRequest(targetId, action), success -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!success) {
                    Toast.makeText(context, "요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                /*
                 * 성공 시 로컬 status를 변경하고 해당 아이템만 갱신함
                 * indexOf로 현재 위치를 다시 확인해 stale position 문제를 방지함
                 */
                String newStatus;
                switch (action) {
                    case "request": newStatus = "sent";    break;
                    case "cancel":  newStatus = "none";    break;
                    case "accept":  newStatus = "friends"; break;
                    case "reject":  newStatus = "none";    break;
                    case "delete":  newStatus = "none";    break;
                    default:        newStatus = "none";    break;
                }
                item.setStatus(newStatus);
                int idx = userList.indexOf(item);
                if (idx >= 0) notifyItemChanged(idx);
            });
        });
    }

    /*
     * status 값에 따라 버튼 텍스트/배경색/아이콘을 적용하는 함수임
     * FriendAdapter와 동일한 스타일을 사용함
     */
    private void applyActionButton(TextView btn, String status) {
        float density = context.getResources().getDisplayMetrics().density;
        int iconSize = (int) (density * 14);
        int iconPadding = (int) (density * 4);

        // 아이콘 잔상 초기화
        btn.setCompoundDrawables(null, null, null, null);
        btn.setCompoundDrawablePadding(0);
        btn.setIncludeFontPadding(false);
        btn.setGravity(Gravity.CENTER);

        String resolvedStatus = status != null ? status : "none";

        switch (resolvedStatus) {
            case "friends": {
                btn.setText("친구 삭제");
                btn.setBackgroundResource(R.drawable.bg_badge_btn);
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4444")));
                btn.setTextColor(Color.WHITE);

                Drawable removeIcon = ContextCompat.getDrawable(context, R.drawable.ic_friend_remove);
                if (removeIcon != null) {
                    removeIcon = removeIcon.mutate();
                    removeIcon.setBounds(0, 0, iconSize, iconSize);
                    DrawableCompat.setTint(removeIcon, Color.WHITE);
                    btn.setCompoundDrawables(removeIcon, null, null, null);
                    btn.setCompoundDrawablePadding(iconPadding);
                }
                break;
            }
            case "sent": {
                btn.setText("요청 취소");
                btn.setBackgroundResource(R.drawable.bg_badge_btn);
                btn.setBackgroundTintList(null);
                btn.setTextColor(Color.BLACK);

                Drawable returnIcon = ContextCompat.getDrawable(context, R.drawable.ic_friend_return);
                if (returnIcon != null) {
                    returnIcon = returnIcon.mutate();
                    returnIcon.setBounds(0, 0, iconSize, iconSize);
                    DrawableCompat.setTint(returnIcon, Color.BLACK);
                    btn.setCompoundDrawables(returnIcon, null, null, null);
                    btn.setCompoundDrawablePadding(iconPadding);
                }
                break;
            }
            case "received": {
                btn.setText("수락");
                btn.setBackgroundResource(R.drawable.bg_badge_btn);
                btn.setBackgroundTintList(null);
                btn.setTextColor(Color.BLACK);

                Drawable acceptIcon = ContextCompat.getDrawable(context, R.drawable.ic_friend_accept);
                if (acceptIcon != null) {
                    acceptIcon = acceptIcon.mutate();
                    acceptIcon.setBounds(0, 0, iconSize, iconSize);
                    DrawableCompat.setTint(acceptIcon, Color.BLACK);
                    btn.setCompoundDrawables(acceptIcon, null, null, null);
                    btn.setCompoundDrawablePadding(iconPadding);
                }
                break;
            }
            default: { // none
                btn.setText("친구요청");
                btn.setBackgroundResource(R.drawable.bg_badge_btn);
                btn.setBackgroundTintList(null);
                btn.setTextColor(Color.BLACK);

                Drawable reqIcon = ContextCompat.getDrawable(context, R.drawable.ic_friend_request);
                if (reqIcon != null) {
                    reqIcon = reqIcon.mutate();
                    reqIcon.setBounds(0, 0, iconSize, iconSize);
                    DrawableCompat.setTint(reqIcon, Color.BLACK);
                    btn.setCompoundDrawables(reqIcon, null, null, null);
                    btn.setCompoundDrawablePadding(iconPadding);
                }
                break;
            }
        }
    }

    /*
     * FriendActivity와 동일한 친구 삭제 확인 다이얼로그를 표시하는 함수임
     */
    private void showDeleteFriendDialog(SearchUserResponse item) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 루트 레이아웃
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        // 제목
        TextView tvTitle = new TextView(context);
        tvTitle.setText("친구 삭제");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        // 안내 문구
        TextView tvMsg = new TextView(context);
        tvMsg.setText(item.getNickname() + "님과 친구를 끊겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(12);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        // 구분선
        View divider = new View(context);
        divider.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        // 버튼 영역
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        // 취소 버튼
        TextView btnCancel = new TextView(context);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        // 삭제 확인 버튼
        TextView btnConfirm = new TextView(context);
        btnConfirm.setText("확인");
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(20));
        confirmBg.setColor(0xFFFF3B30);
        btnConfirm.setBackground(confirmBg);
        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmP.setMarginStart(dp(8));
        btnConfirm.setLayoutParams(confirmP);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            handleFriendAction(item, "delete");
        });

        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return value;
    }

    static class SearchUserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, ivBadge;
        TextView tvNickname, tvFriendCount, btnAction, btnActionSecondary;

        public SearchUserViewHolder(@NonNull View itemView) {
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
