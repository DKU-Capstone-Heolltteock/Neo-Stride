package com.neostride.app.feature.community.friend;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;
import java.util.ArrayList;
import java.util.List;


//  친구 관리 화면 Activity
//  <p>
//  - 친구 목록 / 보낸 요청 / 받은 요청 / 차단 목록 탭으로 구성된다.
//  - 각 탭 전환 시 서버에서 상태별 데이터를 조회하여 RecyclerView에 표시한다.
//  - 친구 삭제 시 확인 다이얼로그를 제공한다.

public class FriendActivity extends AppCompatActivity {

    // ── 어댑터 및 레포지터리 ──
    private FriendAdapter adapter;
    private FriendRepository repository;

    // ── 탭 상태 ──
    private List<TextView> tabs = new ArrayList<>();
    private final String[] statusKeys = {"friends", "sent", "received", "blocked"};
    private int currentTabIndex = 0;
    private View badgeReceivedRequests;
    private TextView tvEmptyState;

    private int getCurrentTabIndex() { return currentTabIndex; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        // 1. API 및 Repository 초기화
        FriendApi api = ApiClient.getInstance().create(FriendApi.class);
        repository = new FriendRepository(api);

        // 2. RecyclerView 설정
        RecyclerView rvFriends = findViewById(R.id.rv_friends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter();
        adapter.setOnActionClickListener((userId, action, nickname) -> {
            if ("delete".equals(action)) {
                showDeleteFriendDialog(userId, nickname);
                return;
            }
            String toastMsg;
            switch (action) {
                case "cancel":  toastMsg = "요청을 취소했습니다.";     break;
                case "accept":  toastMsg = "친구 요청을 수락했습니다."; break;
                case "reject":  toastMsg = "친구 요청을 거절했습니다."; break;
                case "unblock": toastMsg = "차단을 해제했습니다.";     break;
                default:        toastMsg = "처리되었습니다.";          break;
            }
            repository.updateStatus(new FriendRequest(userId, action), success -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (success) loadData(statusKeys[getCurrentTabIndex()]);
                });
            });
        });
        adapter.setOnItemClickListener((userId, nickname) -> {
            Intent intent = new Intent(this, RunnerPageActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("nickname", nickname);
            intent.putExtra("is_blocked", "blocked".equals(statusKeys[getCurrentTabIndex()]));
            intent.putExtra("is_friend", "friends".equals(statusKeys[getCurrentTabIndex()]));
            startActivity(intent);
        });
        rvFriends.setAdapter(adapter);

        // 3. UI 컴포넌트 설정 (뒤로가기 버튼 및 탭 초기화)
        badgeReceivedRequests = findViewById(R.id.badge_received_requests);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        initTabs();

        // 4. 초기 진입 시 스타일 적용 — EXTRA_OPEN_TAB 있으면 해당 탭으로 이동
        String openTab = getIntent().getStringExtra("EXTRA_OPEN_TAB");
        if ("received".equals(openTab)) {
            currentTabIndex = 2;
        }
        updateTabUI(currentTabIndex);

        // 5. 초기 데이터 로드
        loadData(statusKeys[currentTabIndex]);
        // 받은 요청 배지 초기 체크 (친구목록 탭에서도 빨간 점 표시)
        repository.fetchFriendList("received", list -> {
            if (list != null && !list.isEmpty()) {
                runOnUiThread(() -> badgeReceivedRequests.setVisibility(android.view.View.VISIBLE));
            }
        });
    }


     // 상단 탭 버튼들을 리스트에 담고 클릭 리스너를 설정합니다.
    private void initTabs() {
        tabs.add(findViewById(R.id.tab_friend_list));
        tabs.add(findViewById(R.id.tab_sent_requests));
        tabs.add(findViewById(R.id.tab_received_requests));
        tabs.add(findViewById(R.id.tab_blocked_list));

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            tabs.get(i).setOnClickListener(v -> {
                currentTabIndex = index;
                updateTabUI(index);
                loadData(statusKeys[index]);
            });
        }
    }


     // 선택된 탭은 검정색(형광색 배경), 나머지는 흰색(배경 없음)으로 스타일을 업데이트합니다.
    private void updateTabUI(int selectedIndex) {
        for (int i = 0; i < tabs.size(); i++) {
            TextView tab = tabs.get(i);

            if (i == selectedIndex) {
                // 선택됨: 형광색 배경 + 검정색 글자 & 아이콘
                tab.setBackgroundResource(R.drawable.bg_badge_btn);
                tab.setTextColor(Color.BLACK);
                tab.setCompoundDrawableTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                // 미선택: 배경 제거 + 흰색 글자 & 아이콘
                tab.setBackground(null);
                tab.setTextColor(Color.WHITE);
                tab.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
            }
        }
    }


     // 서버로부터 상태별 데이터를 가져옵니다.
    private void loadData(String status) {
        // 탭별 빈 상태 안내 문구
        final String emptyMsg;
        switch (status) {
            case "sent":     emptyMsg = "보낸 친구 요청이 없어요"; break;
            case "received": emptyMsg = "아직 받은 친구 요청이 없어요"; break;
            case "blocked":  emptyMsg = "차단한 사용자가 없어요"; break;
            default:         emptyMsg = "아직 친구가 없어요\n다른 러너를 찾아 친구 요청을 보내보세요"; break;
        }

        repository.fetchFriendList(status, list -> {
            runOnUiThread(() -> {
                if (list != null && !list.isEmpty()) {
                    adapter.setFriendList(list, status);
                    if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    // 받은 요청 탭 빨간 점
                    if ("received".equals(status)) {
                        badgeReceivedRequests.setVisibility(View.VISIBLE);
                    }
                } else {
                    // 빈 목록 → 어댑터 초기화 + 안내 문구 표시
                    adapter.setFriendList(new ArrayList<>(), status);
                    if (tvEmptyState != null) {
                        tvEmptyState.setText(emptyMsg);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                    if ("received".equals(status)) {
                        badgeReceivedRequests.setVisibility(View.GONE);
                    }
                }
            });
        });
    }


     // 친구 삭제 확인 다이얼로그를 표시합니다.
    private void showDeleteFriendDialog(int userId, String nickname) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 루트 레이아웃
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        // 제목
        TextView tvTitle = new TextView(this);
        tvTitle.setText("친구 삭제");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        // 안내 문구
        TextView tvMsg = new TextView(this);
        tvMsg.setText(nickname + "님과 친구를 끊겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(12);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        // 구분선
        View divider = new View(this);
        divider.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        // 버튼 영역
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        // 취소 버튼
        TextView btnCancel = new TextView(this);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        // 삭제 확인 버튼
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
            repository.updateStatus(new FriendRequest(userId, "delete"), success -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, nickname + "님과 친구를 끊었습니다.", Toast.LENGTH_SHORT).show();
                    if (success) loadData(statusKeys[getCurrentTabIndex()]);
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
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    // ─── dp 값을 픽셀로 변환 ───
    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}