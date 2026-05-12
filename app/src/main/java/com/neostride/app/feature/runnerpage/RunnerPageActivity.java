package com.neostride.app.feature.runnerpage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.friend.RunnerFriendListActivity;
import com.neostride.app.feature.friend.api.FriendApi;
import com.neostride.app.feature.friend.model.FriendRequest;
import com.neostride.app.feature.friend.repository.FriendRepository;
import com.neostride.app.feature.mypage.MyFeedAdapter;
import com.neostride.app.feature.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.runnerpage.model.RunnerProfileResponse;
import com.neostride.app.feature.runnerpage.repository.RunnerPageRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RunnerPageActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView ivProfile, ivBadge, ivFriendRequestIcon;
    private TextView tvUsername, tvFriends, tvStatusMessage, tvBlockedMessage, tvFriendRequestLabel, btnMoreOptions;
    private android.widget.LinearLayout btnFriendRequest;
    private TabLayout tabLayout;
    private RecyclerView rvRunnerFeeds;

    private int targetUserId;
    private boolean isBlocked;
    private boolean isFriend;
    private String runnerNickname = "";
    private String runnerBadgeTier = "none";
    private String runnerProfilePhoto = "";
    private int runnerFriendCount = 0;
    private RunnerPageRepository repository;
    private FriendRepository friendRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_runner_page);

        targetUserId = getIntent().getIntExtra("user_id", -1);
        isBlocked    = getIntent().getBooleanExtra("is_blocked", false);
        isFriend     = getIntent().getBooleanExtra("is_friend", false);
        String nicknameHint = getIntent().getStringExtra("nickname");

        repository       = new RunnerPageRepository();
        friendRepository = new FriendRepository(ApiClient.getInstance().create(FriendApi.class));

        initViews();

        // 닉네임 힌트 미리 표시 (API 응답 전 빠른 피드백)
        if (nicknameHint != null) {
            tvUsername.setText(nicknameHint);
            runnerNickname = nicknameHint;
            updateFeedTabTitle(0);
        }

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        fetchRunnerData();
    }

    private void initViews() {
        btnBack          = findViewById(R.id.btn_back);
        ivProfile        = findViewById(R.id.iv_profile);
        ivBadge          = findViewById(R.id.iv_badge);
        tvUsername       = findViewById(R.id.tv_username);
        tvFriends        = findViewById(R.id.tv_friends);
        tvFriends.setOnClickListener(v -> {
            Intent intent = new Intent(this, RunnerFriendListActivity.class);
            intent.putExtra("user_id", targetUserId);
            intent.putExtra("nickname", runnerNickname);
            intent.putExtra("badge_tier", runnerBadgeTier);
            intent.putExtra("friend_count", runnerFriendCount);
            intent.putExtra("profile_photo", runnerProfilePhoto);
            intent.putExtra("is_friend", isFriend);
            startActivity(intent);
        });
        tvStatusMessage  = findViewById(R.id.tv_status_message);
        btnFriendRequest    = findViewById(R.id.btn_friend_request);
        ivFriendRequestIcon = findViewById(R.id.iv_friend_request_icon);
        tvFriendRequestLabel= findViewById(R.id.tv_friend_request_label);
        btnMoreOptions      = findViewById(R.id.btn_more_options);
        tabLayout           = findViewById(R.id.tab_layout);
        rvRunnerFeeds       = findViewById(R.id.rv_runner_feeds);
        tvBlockedMessage    = findViewById(R.id.tv_blocked_message);

        btnMoreOptions.setOnClickListener(v -> showMoreOptionsDialog());

        btnFriendRequest.setOnClickListener(v -> sendFriendRequest());
    }

    private void setupRecyclerView() {
        rvRunnerFeeds.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchRunnerData() {
        if (targetUserId == -1) return;

        // 차단된 사용자: 버튼을 '차단 해제'로 교체, 피드 목록 숨기고 안내 문구 표시
        if (isBlocked) {
            ivFriendRequestIcon.setVisibility(android.view.View.VISIBLE);
            ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_unlock);
            tvFriendRequestLabel.setText("차단 해제");
            btnFriendRequest.setOnClickListener(v ->
                friendRepository.updateStatus(new FriendRequest(targetUserId, "unblock"), success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            isBlocked = false;
                            restoreUnblockedUI();
                        } else {
                            Toast.makeText(this, "차단 해제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
            );
            rvRunnerFeeds.setVisibility(android.view.View.GONE);
            tvBlockedMessage.setVisibility(android.view.View.VISIBLE);
        }

        // 프로필 조회
        repository.getRunnerProfile(targetUserId, new Callback<RunnerProfileResponse>() {
            @Override
            public void onResponse(Call<RunnerProfileResponse> call, Response<RunnerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateProfileUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<RunnerProfileResponse> call, Throwable t) {
                Log.e("RunnerPage", "프로필 로드 실패: " + t.getMessage());
            }
        });

        // 배지 조회
        repository.getRunnerBadge(targetUserId, badgeResponse -> {
            runnerBadgeTier = badgeResponse.tier != null ? badgeResponse.tier : "none";
            BadgeTier tier = BadgeTier.fromString(badgeResponse.tier);
            if (ivBadge != null && ivBadge.getDrawable() != null) {
                Drawable d = DrawableCompat.wrap(ivBadge.getDrawable()).mutate();
                DrawableCompat.setTint(d, tier.getColor());
                runOnUiThread(() -> ivBadge.setImageDrawable(d));
            }
        });

        // 피드 조회 (차단된 사용자는 스킵)
        if (isBlocked) return;
        loadRunnerFeeds();
    }

    private void loadRunnerFeeds() {
        repository.getRunnerFeeds(targetUserId, new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CommunityContentResponse> feeds = response.body();
                    updateFeedTabTitle(feeds.size());
                    MyFeedAdapter adapter = new MyFeedAdapter(feeds);
                    adapter.setOnProfileClickListener((userId, nickname) -> {
                        int myId = TokenManager.getUserId(RunnerPageActivity.this);
                        if (userId == myId) return;
                        if (userId == targetUserId) return;
                        Intent intent = new Intent(RunnerPageActivity.this, RunnerPageActivity.class);
                        intent.putExtra("user_id", userId);
                        intent.putExtra("nickname", nickname);
                        startActivity(intent);
                    });
                    rvRunnerFeeds.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("RunnerPage", "피드 로드 실패: " + t.getMessage());
            }
        });
    }

    /**
     * ••• 더보기 PopupWindow (차단 / 신고) — 버튼 바로 아래에 드롭다운으로 표시
     */
    private void showMoreOptionsDialog() {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_runner_more_options, null);

        int width = (int) (160 * getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(25);
        popup.showAsDropDown(btnMoreOptions, -width + btnMoreOptions.getWidth(), 8);

        menuView.findViewById(R.id.menu_block).setOnClickListener(v -> {
            popup.dismiss();
            if (isBlocked) {
                Toast.makeText(this, "이미 차단한 유저입니다.", Toast.LENGTH_SHORT).show();
            } else {
                showBlockConfirmDialog();
            }
        });
        menuView.findViewById(R.id.menu_report).setOnClickListener(v -> {
            popup.dismiss();
            Toast.makeText(this, "신고 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 차단 확인 다이얼로그
     */
    private void showBlockConfirmDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("차단하기");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText("상대방의 피드와 댓글을 볼 수 없으며 친구 요청도 불가합니다.\n정말 " + runnerNickname + "님을 차단하시겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(14);
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
        btnConfirm.setText("차단");
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
            friendRepository.updateStatus(new FriendRequest(targetUserId, "block"), success -> {
                runOnUiThread(() -> {
                    if (success) {
                        isBlocked = true;
                        // 차단 UI 적용
                        ivFriendRequestIcon.setVisibility(View.VISIBLE);
                        ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_unlock);
                        tvFriendRequestLabel.setText("차단 해제");
                        btnFriendRequest.setOnClickListener(vv ->
                            friendRepository.updateStatus(new FriendRequest(targetUserId, "unblock"), s -> {
                                runOnUiThread(() -> { if (s) { isBlocked = false; restoreUnblockedUI(); } });
                            })
                        );
                        rvRunnerFeeds.setVisibility(View.GONE);
                        tvBlockedMessage.setVisibility(View.VISIBLE);
                        Toast.makeText(this, runnerNickname + "님을 차단했습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "차단에 실패했습니다.", Toast.LENGTH_SHORT).show();
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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    /**
     * 차단 해제 성공 후 러너페이지를 정상 상태로 복원합니다.
     */
    private void sendFriendRequest() {
        if (targetUserId == -1) return;
        friendRepository.updateStatus(new FriendRequest(targetUserId, "request"), success -> {
            runOnUiThread(() -> {
                if (success) {
                    // 요청 취소 버튼으로 전환
                    ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_return);
                    tvFriendRequestLabel.setText("요청 취소");
                    btnFriendRequest.setOnClickListener(v -> cancelFriendRequest());
                    Toast.makeText(this, "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "친구 요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void cancelFriendRequest() {
        if (targetUserId == -1) return;
        friendRepository.updateStatus(new FriendRequest(targetUserId, "cancel"), success -> {
            runOnUiThread(() -> {
                if (success) {
                    // 친구요청 버튼으로 복원
                    ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_request);
                    tvFriendRequestLabel.setText("친구요청");
                    btnFriendRequest.setOnClickListener(v -> sendFriendRequest());
                    Toast.makeText(this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "취소에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void restoreUnblockedUI() {
        // 버튼을 친구요청으로 복원
        ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_request);
        tvFriendRequestLabel.setText("친구요청");
        btnFriendRequest.setOnClickListener(v -> sendFriendRequest());
        // 피드 영역 복원
        tvBlockedMessage.setVisibility(android.view.View.GONE);
        rvRunnerFeeds.setVisibility(android.view.View.VISIBLE);
        // 피드 데이터 로드
        loadRunnerFeeds();
    }

    private void updateFeedTabTitle(int count) {
        TabLayout.Tab feedTab = tabLayout.getTabAt(0);
        if (feedTab != null) {
            feedTab.setText(runnerNickname + " 님의 피드 " + count);
        }
    }

    private void updateProfileUI(RunnerProfileResponse data) {
        if (data.nickname != null) {
            tvUsername.setText(data.nickname);
            runnerNickname = data.nickname;
        }
        runnerFriendCount = data.friendCount != null ? data.friendCount : 0;
        tvFriends.setText("친구 " + runnerFriendCount);
        // 백엔드 응답에 is_friend가 있으면 우선 적용 (Intent로 받은 값보다 정확)
        if (data.isFriend != null) isFriend = data.isFriend;
        tvStatusMessage.setText(data.statusMessage != null ? data.statusMessage : "");

        // 피드 탭 카운트 (postCount 기준 먼저 갱신, 피드 API 응답 오면 덮어씀)
        if (data.postCount != null) updateFeedTabTitle(data.postCount);

        if (data.profilePhoto != null && !data.profilePhoto.isEmpty()) {
            runnerProfilePhoto = data.profilePhoto;
            Glide.with(this)
                    .load(data.profilePhoto)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfile);
        }
    }
}
