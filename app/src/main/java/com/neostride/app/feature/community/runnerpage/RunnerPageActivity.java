package com.neostride.app.feature.community.runnerpage;

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
import com.neostride.app.feature.community.friend.RunnerFriendListActivity;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.mypage.MyPostsAdapter;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.tip.model.TipResponse;

import java.util.ArrayList;
import com.neostride.app.feature.community.runnerpage.model.RunnerProfileResponse;
import com.neostride.app.feature.community.runnerpage.repository.RunnerPageRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//  다른 러너의 프로필 페이지 Activity
//  <p>
//  - 닉네임·배지·상태메시지·친구 수 등 프로필 정보를 표시한다.
//  - 친구 요청·취소·삭제, 차단·차단 해제 기능을 제공한다.
//  - 해당 러너의 피드 목록을 탭 RecyclerView로 표시한다.

public class RunnerPageActivity extends AppCompatActivity {

    // ── UI 뷰 ──
    private ImageButton btnBack;
    private ImageView ivProfile, ivBadge, ivFriendRequestIcon;
    private TextView tvUsername, tvFriends, tvStatusMessage, tvBlockedMessage, tvFriendRequestLabel, btnMoreOptions;
    private TextView tvEmptyState;
    private android.widget.LinearLayout btnFriendRequest;
    private TabLayout tabLayout;
    private RecyclerView rvRunnerFeeds;

    // ── 상태 ──
    private int targetUserId;
    private boolean isBlocked;
    private boolean isFriend;
    // 친구 상태: "none" | "sent" | "friends"
    private String friendshipStatus = "none";
    // 쓴 글 필터: "all" | "feed" | "tip"
    private String currentPostFilter = "all";
    private String runnerNickname = "";
    private String runnerBadgeTier = "none";
    private String runnerProfilePhoto = "";
    private int runnerFriendCount = 0;

    // ── 레포지터리 ──
    private RunnerPageRepository repository;
    private FriendRepository friendRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_runner_page);

        targetUserId     = getIntent().getIntExtra("user_id", -1);
        isBlocked        = getIntent().getBooleanExtra("is_blocked", false);
        isFriend         = getIntent().getBooleanExtra("is_friend", false);
        friendshipStatus = isFriend ? "friends" : "none";
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

    // ─── 뷰 참조 초기화 및 기본 클릭 리스너 등록 ───
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
        tvEmptyState        = findViewById(R.id.tv_empty_state);

        btnMoreOptions.setOnClickListener(v -> showMoreOptionsDialog());
    }

    // ─── 피드 RecyclerView 레이아웃 매니저 설정 ───
    private void setupRecyclerView() {
        rvRunnerFeeds.setLayoutManager(new LinearLayoutManager(this));
    }

    // ─── 차단 상태 분기 후 프로필·배지·피드를 서버에서 조회 ───
    private void fetchRunnerData() {
        if (targetUserId == -1) return;

        // 차단된 사용자: 버튼을 '차단 해제'로 교체, 피드 목록 숨기고 안내 문구 표시
        if (isBlocked) {
            ivFriendRequestIcon.setVisibility(android.view.View.VISIBLE);
            ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_unlock);
            ivFriendRequestIcon.setColorFilter(Color.BLACK);
            tvFriendRequestLabel.setText("차단 해제");
            tvFriendRequestLabel.setTextColor(Color.BLACK);
            btnFriendRequest.setBackgroundResource(R.drawable.bg_badge_btn);
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
        } else {
            updateFriendButton();
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

        // 배지 조회 (언랭이면 숨김)
        repository.getRunnerBadge(targetUserId, badgeResponse -> {
            runnerBadgeTier = badgeResponse.tier != null ? badgeResponse.tier : "none";
            BadgeTier tier = BadgeTier.fromString(badgeResponse.tier);
            if (ivBadge != null) {
                if (tier.isNone()) {
                    runOnUiThread(() -> ivBadge.setVisibility(View.GONE));
                } else if (ivBadge.getDrawable() != null) {
                    Drawable d = DrawableCompat.wrap(ivBadge.getDrawable()).mutate();
                    DrawableCompat.setTint(d, tier.getColor());
                    runOnUiThread(() -> {
                        ivBadge.setVisibility(View.VISIBLE);
                        ivBadge.setImageDrawable(d);
                    });
                }
            }
        });

        // 피드+팁 조회 (차단된 사용자는 스킵)
        if (isBlocked) return;
        loadRunnerPostsAll();
    }

    // ─── 필터 클릭 콜백 ───
    private void onFilterClick(String filter) {
        currentPostFilter = filter;
        switch (filter) {
            case "feed": loadRunnerFeedsOnly(); break;
            case "tip":  loadRunnerTipsOnly();  break;
            default:     loadRunnerPostsAll();  break;
        }
    }

    // ─── 전체(피드+팁) 병렬 조회 ───
    private void loadRunnerPostsAll() {
        rvRunnerFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        final List<CommunityContentResponse> feedResult = new ArrayList<>();
        final List<TipResponse> tipResult = new ArrayList<>();
        final int[] pending = {2};

        Runnable onBothDone = () -> runOnUiThread(() -> {
            List<MyPostsAdapter.PostItem> combined = new ArrayList<>();
            for (CommunityContentResponse f : feedResult) combined.add(new MyPostsAdapter.PostItem(f));
            for (TipResponse t : tipResult)               combined.add(new MyPostsAdapter.PostItem(t));
            MyPostsAdapter adapter = new MyPostsAdapter(this, combined, "all", this::onFilterClick, false);
            adapter.setOnBlockAction(this::showBlockConfirmDialog);
            rvRunnerFeeds.setAdapter(adapter);
            if (combined.isEmpty()) {
                if (tvEmptyState != null) {
                    tvEmptyState.setText("아직 작성한 게시글이 없어요");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            }
        });

        repository.getRunnerFeeds(targetUserId, new Callback<List<CommunityContentResponse>>() {
            @Override public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> res) {
                if (res.isSuccessful() && res.body() != null) feedResult.addAll(res.body());
                if (--pending[0] == 0) onBothDone.run();
            }
            @Override public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("RunnerPage", "피드 로드 실패: " + t.getMessage());
                if (--pending[0] == 0) onBothDone.run();
            }
        });

        repository.getRunnerTips(targetUserId, new Callback<List<TipResponse>>() {
            @Override public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> res) {
                if (res.isSuccessful() && res.body() != null) tipResult.addAll(res.body());
                if (--pending[0] == 0) onBothDone.run();
            }
            @Override public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                Log.e("RunnerPage", "팁 로드 실패: " + t.getMessage());
                if (--pending[0] == 0) onBothDone.run();
            }
        });
    }

    // ─── 피드만 조회 ───
    private void loadRunnerFeedsOnly() {
        rvRunnerFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        repository.getRunnerFeeds(targetUserId, new Callback<List<CommunityContentResponse>>() {
            @Override public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> res) {
                runOnUiThread(() -> {
                    List<MyPostsAdapter.PostItem> items = new ArrayList<>();
                    if (res.isSuccessful() && res.body() != null)
                        for (CommunityContentResponse f : res.body()) items.add(new MyPostsAdapter.PostItem(f));
                    if (items.isEmpty() && tvEmptyState != null) {
                        tvEmptyState.setText("작성한 피드가 없어요");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                    MyPostsAdapter adapter = new MyPostsAdapter(RunnerPageActivity.this, items, "feed", RunnerPageActivity.this::onFilterClick, false);
                    adapter.setOnBlockAction(RunnerPageActivity.this::showBlockConfirmDialog);
                    rvRunnerFeeds.setAdapter(adapter);
                });
            }
            @Override public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("RunnerPage", "피드 로드 실패: " + t.getMessage());
            }
        });
    }

    // ─── 팁만 조회 ───
    private void loadRunnerTipsOnly() {
        rvRunnerFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        repository.getRunnerTips(targetUserId, new Callback<List<TipResponse>>() {
            @Override public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> res) {
                runOnUiThread(() -> {
                    List<MyPostsAdapter.PostItem> items = new ArrayList<>();
                    if (res.isSuccessful() && res.body() != null)
                        for (TipResponse t : res.body()) items.add(new MyPostsAdapter.PostItem(t));
                    if (items.isEmpty() && tvEmptyState != null) {
                        tvEmptyState.setText("작성한 팁이 없어요");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                    MyPostsAdapter adapter = new MyPostsAdapter(RunnerPageActivity.this, items, "tip", RunnerPageActivity.this::onFilterClick, false);
                    adapter.setOnBlockAction(RunnerPageActivity.this::showBlockConfirmDialog);
                    rvRunnerFeeds.setAdapter(adapter);
                });
            }
            @Override public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                Log.e("RunnerPage", "팁 로드 실패: " + t.getMessage());
            }
        });
    }


    // ••• 더보기 PopupWindow (차단 / 신고) — 버튼 바로 아래에 드롭다운으로 표시

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


    // 차단 확인 다이얼로그

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
        tvMsg.setText("차단하면 상대방의 글과 댓글이 나에게 보이지 않으며,\n상대방 글에 남긴 좋아요·북마크·댓글은 삭제됩니다.\n정말 " + runnerNickname + "님을 차단하시겠습니까?");
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

    // ─── dp 단위를 픽셀로 변환 ───
    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    // ─── 친구 요청 전송 후 버튼 상태를 "sent"로 전환 ───
    private void sendFriendRequest() {
        if (targetUserId == -1) return;
        friendRepository.updateStatus(new FriendRequest(targetUserId, "request"), success -> {
            runOnUiThread(() -> {
                if (success) {
                    friendshipStatus = "sent";
                    updateFriendButton();
                    Toast.makeText(this, "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "친구 요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ─── 보낸 친구 요청을 취소하고 버튼 상태를 "none"으로 복원 ───
    private void cancelFriendRequest() {
        if (targetUserId == -1) return;
        friendRepository.updateStatus(new FriendRequest(targetUserId, "cancel"), success -> {
            runOnUiThread(() -> {
                if (success) {
                    friendshipStatus = "none";
                    isFriend = false;
                    updateFriendButton();
                    Toast.makeText(this, "요청을 취소했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "취소에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ─── 차단 해제 후 친구 버튼·피드 목록 UI를 원상태로 복원 ───
    private void restoreUnblockedUI() {
        friendshipStatus = isFriend ? "friends" : "none";
        updateFriendButton();
        tvBlockedMessage.setVisibility(android.view.View.GONE);
        rvRunnerFeeds.setVisibility(android.view.View.VISIBLE);
        currentPostFilter = "all";
        loadRunnerPostsAll();
    }

    // friendshipStatus 값에 따라 친구 버튼 아이콘·텍스트·배경·클릭 일괄 적용
    private void updateFriendButton() {
        switch (friendshipStatus) {
            case "friends":
                // 친구 삭제 버튼 (빨간색)
                android.graphics.drawable.GradientDrawable redBg = new android.graphics.drawable.GradientDrawable();
                redBg.setCornerRadius(dp(20));
                redBg.setColor(Color.parseColor("#FF4444"));
                btnFriendRequest.setBackground(redBg);
                ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_remove);
                ivFriendRequestIcon.setColorFilter(Color.WHITE);
                tvFriendRequestLabel.setText("친구 삭제");
                tvFriendRequestLabel.setTextColor(Color.WHITE);
                btnFriendRequest.setOnClickListener(v -> showDeleteFriendDialog());
                break;

            case "sent":
                // 요청 취소 버튼 (형광)
                btnFriendRequest.setBackgroundResource(R.drawable.bg_badge_btn);
                ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_return);
                ivFriendRequestIcon.setColorFilter(Color.BLACK);
                tvFriendRequestLabel.setText("요청 취소");
                tvFriendRequestLabel.setTextColor(Color.BLACK);
                btnFriendRequest.setOnClickListener(v -> cancelFriendRequest());
                break;

            default: // "none"
                // 친구 요청 버튼 (형광)
                btnFriendRequest.setBackgroundResource(R.drawable.bg_badge_btn);
                ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_request);
                ivFriendRequestIcon.setColorFilter(Color.BLACK);
                tvFriendRequestLabel.setText("친구요청");
                tvFriendRequestLabel.setTextColor(Color.BLACK);
                btnFriendRequest.setOnClickListener(v -> sendFriendRequest());
                break;
        }
    }


    // 친구 삭제 확인 다이얼로그
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
        tvMsg.setText(runnerNickname + "님과 친구를 끊겠습니까?");
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
        btnConfirm.setText("삭제");
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        android.graphics.drawable.GradientDrawable confirmBg = new android.graphics.drawable.GradientDrawable();
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
                        isFriend = false;
                        friendshipStatus = "none";
                        updateFriendButton();
                        Toast.makeText(this, runnerNickname + "님과 친구를 끊었습니다.", Toast.LENGTH_SHORT).show();
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

    // ─── 탭 타이틀을 "닉네임님이 쓴 글 N" 형식으로 갱신 ───
    private void updateFeedTabTitle(int count) {
        TabLayout.Tab feedTab = tabLayout.getTabAt(0);
        if (feedTab != null) {
            feedTab.setText(runnerNickname + "님이 쓴 글 " + count);
        }
    }

    // ─── 서버 프로필 응답으로 닉네임·친구 수·상태메시지·사진 UI 갱신 ───
    private void updateProfileUI(RunnerProfileResponse data) {
        if (data.nickname != null) {
            tvUsername.setText(data.nickname);
            runnerNickname = data.nickname;
        }
        runnerFriendCount = data.friendCount != null ? data.friendCount : 0;
        tvFriends.setText("친구 " + runnerFriendCount);
        // 백엔드 응답에 is_blocked가 있으면 우선 적용 (Intent로 받은 값보다 정확)
        // 백엔드에서 is_blocked 필드를 내려줘야 동작함
        if (data.isBlocked != null && data.isBlocked && !isBlocked) {
            isBlocked = true;
            ivFriendRequestIcon.setVisibility(View.VISIBLE);
            ivFriendRequestIcon.setImageResource(R.drawable.ic_friend_unlock);
            ivFriendRequestIcon.setColorFilter(Color.BLACK);
            tvFriendRequestLabel.setText("차단 해제");
            tvFriendRequestLabel.setTextColor(Color.BLACK);
            btnFriendRequest.setBackgroundResource(R.drawable.bg_badge_btn);
            btnFriendRequest.setOnClickListener(v ->
                friendRepository.updateStatus(new FriendRequest(targetUserId, "unblock"), success -> {
                    runOnUiThread(() -> { if (success) { isBlocked = false; restoreUnblockedUI(); }
                    else Toast.makeText(this, "차단 해제에 실패했습니다.", Toast.LENGTH_SHORT).show(); });
                })
            );
            rvRunnerFeeds.setVisibility(View.GONE);
            tvBlockedMessage.setVisibility(View.VISIBLE);
            return; // 차단 상태면 이하 친구 버튼 갱신 불필요
        }

        // 백엔드 응답에 is_friend가 있으면 우선 적용 (Intent로 받은 값보다 정확)
        if (data.isFriend != null) {
            isFriend = data.isFriend;
            // 아직 요청 취소 상태가 아닐 때만 덮어씀 (sent 상태는 유지)
            if (!"sent".equals(friendshipStatus)) {
                friendshipStatus = isFriend ? "friends" : "none";
                if (!isBlocked) updateFriendButton();
            }
        }
        tvStatusMessage.setText(data.statusMessage != null ? data.statusMessage : "");

        // 탭 카운트 (postCount + tipCount 합산, API 응답 오면 갱신됨)
        int feedCount = (data.postCount != null) ? data.postCount : 0;
        int tipCount  = (data.tipCount  != null) ? data.tipCount  : 0;
        updateFeedTabTitle(feedCount + tipCount);

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
