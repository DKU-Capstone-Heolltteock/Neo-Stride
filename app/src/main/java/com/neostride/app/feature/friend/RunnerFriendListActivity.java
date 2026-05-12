package com.neostride.app.feature.friend;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.friend.api.FriendApi;
import com.neostride.app.feature.friend.model.FriendRequest;
import com.neostride.app.feature.friend.repository.FriendRepository;
import com.neostride.app.feature.runnerpage.RunnerPageActivity;

public class RunnerFriendListActivity extends AppCompatActivity {

    private ImageView ivProfile, ivBadge;
    private TextView tvNickname, tvFriendCount, btnFriendAction;
    private RecyclerView rvRunnerFriends;
    private TextView tvAccessDenied;

    private int targetUserId;
    private String nickname;
    private String badgeTier;
    private int friendCount;
    private String profilePhoto;
    private boolean isFriend;
    /** "none" | "sent" | "friends" | "blocked" */
    private String friendshipStatus = "none";

    private FriendRepository friendRepository;
    private FriendAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runner_friend_list);

        // Intent 데이터 수신
        targetUserId = getIntent().getIntExtra("user_id", -1);
        nickname     = getIntent().getStringExtra("nickname");
        badgeTier    = getIntent().getStringExtra("badge_tier");
        friendCount  = getIntent().getIntExtra("friend_count", 0);
        profilePhoto = getIntent().getStringExtra("profile_photo");
        isFriend         = getIntent().getBooleanExtra("is_friend", false);
        friendshipStatus = isFriend ? "friends" : "none";

        friendRepository = new FriendRepository(ApiClient.getInstance().create(FriendApi.class));

        initViews();
        bindRunnerProfileCard();
        setupFriendList();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        ivProfile       = findViewById(R.id.iv_profile);
        ivBadge         = findViewById(R.id.iv_badge);
        tvNickname      = findViewById(R.id.tv_nickname);
        tvFriendCount   = findViewById(R.id.tv_friend_count);
        btnFriendAction = findViewById(R.id.btn_friend_action);
        rvRunnerFriends = findViewById(R.id.rv_runner_friends);
        tvAccessDenied  = findViewById(R.id.tv_access_denied);
    }

    /**
     * 최상단 러너 프로필 카드 바인딩
     */
    private void bindRunnerProfileCard() {
        // 닉네임
        if (nickname != null) tvNickname.setText(nickname);

        // 친구 수
        tvFriendCount.setText("친구 " + friendCount);

        // 배지 색상
        BadgeTier tier = BadgeTier.fromString(badgeTier);
        ivBadge.setColorFilter(tier.getColor());

        // 프로필 사진
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            Glide.with(this)
                    .load(profilePhoto)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfile);
        }

        // 액션 버튼 스타일: friendshipStatus 기반
        btnFriendAction.setEnabled(true);
        btnFriendAction.setAlpha(1f);
        btnFriendAction.setIncludeFontPadding(false);

        switch (friendshipStatus) {
            case "friends":
                setButtonStyle(btnFriendAction, "친구 삭제", R.drawable.ic_friend_remove, "#FF4444", Color.WHITE);
                btnFriendAction.setOnClickListener(v -> showDeleteFriendDialog());
                break;

            case "sent":
                setButtonStyle(btnFriendAction, "요청 취소", R.drawable.ic_friend_return, null, Color.BLACK);
                btnFriendAction.setOnClickListener(v -> {
                    if (targetUserId == -1) return;
                    friendRepository.updateStatus(new FriendRequest(targetUserId, "cancel"), success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                friendshipStatus = "none";
                                isFriend = false;
                                bindRunnerProfileCard();
                                Toast.makeText(this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "취소에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                });
                break;

            default: // "none"
                setButtonStyle(btnFriendAction, "친구요청", R.drawable.ic_friend_request, null, Color.BLACK);
                btnFriendAction.setOnClickListener(v -> {
                    if (targetUserId == -1) return;
                    friendRepository.updateStatus(new FriendRequest(targetUserId, "request"), success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                friendshipStatus = "sent";
                                bindRunnerProfileCard();
                                Toast.makeText(this, "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "친구 요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                });
                break;
        }
    }

    /**
     * 친구 목록 RecyclerView 설정
     */
    private void setupFriendList() {
        if (!isFriend || targetUserId == -1) {
            // 친구가 아닌 경우: 접근 제한 메시지 표시
            tvAccessDenied.setVisibility(View.VISIBLE);
            rvRunnerFriends.setVisibility(View.GONE);
            return;
        }

        // 친구인 경우: 상대방의 친구 목록 로드
        rvRunnerFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter();
        adapter.setOnItemClickListener((userId, nick) -> {
            Intent intent = new Intent(this, RunnerPageActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("nickname", nick);
            startActivity(intent);
        });
        // 액션 버튼 표시 없이 순수 목록만 (친구목록 탭 스타일)
        adapter.setOnActionClickListener((userId, action, nick) -> {
            String toastMsg;
            String newStatus;
            switch (action) {
                case "request": toastMsg = "친구 요청을 보냈습니다.";        newStatus = "sent";    break;
                case "cancel":  toastMsg = "요청을 취소했습니다.";           newStatus = "none";    break;
                case "delete":  toastMsg = nick + "님과 친구를 끊었습니다."; newStatus = "none";    break;
                case "unblock": toastMsg = "차단을 해제했습니다.";           newStatus = "none";    break;
                default:        toastMsg = "처리되었습니다.";                newStatus = "none";    break;
            }
            friendRepository.updateStatus(new FriendRequest(userId, action), success -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (success) adapter.updateItemStatus(userId, newStatus);
                });
            });
        });
        rvRunnerFriends.setAdapter(adapter);
        rvRunnerFriends.setVisibility(View.VISIBLE);
        tvAccessDenied.setVisibility(View.GONE);

        friendRepository.fetchUserFriendList(targetUserId, list -> {
            runOnUiThread(() -> {
                if (list != null && !list.isEmpty()) {
                    // "per_item" 모드: 각 FriendResponse.status로 버튼 결정
                    adapter.setFriendList(list, "per_item");
                } else {
                    rvRunnerFriends.setVisibility(View.GONE);
                    tvAccessDenied.setText("친구가 없습니다.");
                    tvAccessDenied.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    /**
     * 친구 삭제 확인 다이얼로그
     */
    private void showDeleteFriendDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("친구 삭제");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText((nickname != null ? nickname : "") + "님과 친구를 끊겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(12);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = new TextView(this);
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
            friendRepository.updateStatus(new FriendRequest(targetUserId, "delete"), success -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, (nickname != null ? nickname : "") + "님과 친구를 끊었습니다.", Toast.LENGTH_SHORT).show();
                        isFriend = false;
                        friendshipStatus = "none";
                        // 버튼을 친구요청으로 전환
                        bindRunnerProfileCard();
                        // 친구 목록 숨기고 접근 제한 표시
                        rvRunnerFriends.setVisibility(View.GONE);
                        tvAccessDenied.setText("친구 관계가 아닌 유저의 친구 목록은\n볼 수 없습니다.");
                        tvAccessDenied.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "친구 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    /**
     * 버튼 배경·텍스트·아이콘을 한번에 적용하는 헬퍼.
     * colorHex가 null이면 bg_badge_btn(형광) 적용, 아니면 해당 색상 GradientDrawable 적용.
     */
    private void setButtonStyle(TextView btn, String text, int iconRes, String colorHex, int textColor) {
        btn.setText(text);
        btn.setTextColor(textColor);

        if (colorHex != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            bg.setColor(Color.parseColor(colorHex));
            btn.setBackground(bg);
        } else {
            btn.setBackgroundResource(R.drawable.bg_badge_btn);
        }

        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            int size = dp(14);
            icon = icon.mutate();
            icon.setBounds(0, 0, size, size);
            DrawableCompat.setTint(icon, textColor);
            btn.setCompoundDrawables(icon, null, null, null);
            btn.setCompoundDrawablePadding(dp(4));
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
