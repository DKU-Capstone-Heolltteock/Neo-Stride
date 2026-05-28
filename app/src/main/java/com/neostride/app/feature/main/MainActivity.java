package com.neostride.app.feature.main;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.neostride.app.BuildConfig;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.auth.LoginActivity;
import com.neostride.app.feature.community.CommunityActivity;
import com.neostride.app.feature.community.mypage.MyPageActivity;
import com.neostride.app.feature.community.mypage.model.UserProfileResponse;
import com.neostride.app.feature.community.mypage.repository.MyPageRepository;
import com.neostride.app.feature.main.coaching.CoachingFragment;
import com.neostride.app.feature.main.record.RecordFragment;
import com.neostride.app.feature.main.running.RunningFragment;
import com.neostride.app.feature.notification.NotificationActivity;
import com.neostride.app.feature.notification.model.NotificationResponse;
import com.neostride.app.feature.notification.repository.NotificationRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private LinearLayout tabRunning, tabRecord, tabCoaching, tabCommunity;
    private ImageView ivRunning, ivRecord, ivCoaching, ivCommunity;
    private TextView tvRunning, tvRecord, tvCoaching, tvCommunity;
    private ImageView btnNotification, btnProfile;
    private View badgeNotification;
    private MyPageRepository myPageRepository;

    // ── 보존된 Fragment 인스턴스 (탭 전환 시 hide/show로 재사용) ──
    //  - 측정 중 다른 탭 갔다 와도 RunningFragment 인스턴스·view·상태가 살아있도록 함
    //  - CommunityActivity는 별도 Activity라 해당 사항 없음
    private RunningFragment runningFragment;
    private RecordFragment recordFragment;
    private CoachingFragment coachingFragment;
    private Fragment activeFragment;
    private static final String TAG_RUNNING  = "f_running";
    private static final String TAG_RECORD   = "f_record";
    private static final String TAG_COACHING = "f_coaching";

    // 알림 권한 요청 런처임
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // 알림 권한 허용/거부 후 별도 처리는 현재 하지 않음
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        /*
         * ApiClient에 앱 Context를 초기화함
         * 일반 API 호출에서 토큰 접근 등에 사용됨
         */
        ApiClient.init(this);

        initViews();
        myPageRepository = new MyPageRepository();
        requestInitialPermissions();

        if (savedInstanceState == null) {
            String moveTo = getIntent().getStringExtra("move_to");
            String recordMode = getIntent().getStringExtra("record_mode");

            // 모든 Fragment를 한 번에 add + hide 후 초기 탭만 show
            setupFragments("record".equals(moveTo) ? "record" : "running", recordMode);
            updateTabUI("record".equals(moveTo) ? "record" : "running");
        } else {
            // 시스템 재생성 시 — FragmentManager가 자동 복원한 인스턴스 재참조
            FragmentManager fm = getSupportFragmentManager();
            runningFragment  = (RunningFragment)  fm.findFragmentByTag(TAG_RUNNING);
            recordFragment   = (RecordFragment)   fm.findFragmentByTag(TAG_RECORD);
            coachingFragment = (CoachingFragment) fm.findFragmentByTag(TAG_COACHING);
            // 복원이 어떤 이유로든 실패하면 새로 만들어 폴백
            if (runningFragment == null || recordFragment == null || coachingFragment == null) {
                setupFragments("running", null);
                updateTabUI("running");
            } else {
                // 보이고 있던 fragment를 activeFragment로 재설정
                if (recordFragment.isVisible())        activeFragment = recordFragment;
                else if (coachingFragment.isVisible()) activeFragment = coachingFragment;
                else                                   activeFragment = runningFragment;
            }
        }

        setTabListeners();

        // 뒤로가기 인터셉트 — 종료 확인 다이얼로그 표시 (측정 중이면 경고 문구 포함)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 상세 화면 등 백스택에 쌓인 fragment가 있으면 먼저 pop, 그 다음에 종료 다이얼로그
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }
                showExitConfirmDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * 기존에는 여기서 WearDataReceiver를 등록했음
         *
         * 하지만 WearDataReceiver와 WearListenerService가 동시에 있으면
         * 워치 데이터 수신 시 서버 저장이 중복될 수 있음
         *
         * 따라서 워치 러닝 결과 수신은 Manifest에 등록된 WearListenerService만 담당하게 함
         */
        checkUnreadNotifications();
        loadProfileButton();
    }

    private void initViews() {
        tabRunning = findViewById(R.id.tab_running);
        tabRecord = findViewById(R.id.tab_record);
        tabCoaching = findViewById(R.id.tab_coaching);
        tabCommunity = findViewById(R.id.tab_community);

        ivRunning = findViewById(R.id.iv_running);
        ivRecord = findViewById(R.id.iv_record);
        ivCoaching = findViewById(R.id.iv_coaching);
        ivCommunity = findViewById(R.id.iv_community);

        tvRunning = findViewById(R.id.tv_running);
        tvRecord = findViewById(R.id.tv_record);
        tvCoaching = findViewById(R.id.tv_coaching);
        tvCommunity = findViewById(R.id.tv_community);

        btnNotification = findViewById(R.id.btn_notification);
        btnProfile = findViewById(R.id.btn_profile);
        badgeNotification = findViewById(R.id.badge_notification);
    }

    private void setTabListeners() {
        tabRunning.setOnClickListener(v -> {
            showFragment(runningFragment);
            updateTabUI("running");
        });

        tabRecord.setOnClickListener(v -> {
            showFragment(recordFragment);
            updateTabUI("record");
        });

        tabCoaching.setOnClickListener(v -> {
            showFragment(coachingFragment);
            updateTabUI("coaching");
        });

        tabCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, NotificationActivity.class));

                if (badgeNotification != null) {
                    badgeNotification.setVisibility(View.GONE);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showProfileMenu(v));
        }
    }

    private void showProfileMenu(View anchorView) {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_profile_menu, null);

        int width = (int) (220 * getResources().getDisplayMetrics().density);

        final PopupWindow popupWindow = new PopupWindow(
                menuView,
                width,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(25);
        popupWindow.showAsDropDown(anchorView, -width + anchorView.getWidth(), 15);

        LinearLayout menuAccount = menuView.findViewById(R.id.menu_account);
        menuAccount.setOnClickListener(v -> {
            popupWindow.dismiss();
            startActivity(new Intent(MainActivity.this, com.neostride.app.feature.account.AccountActivity.class));
        });

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        menuLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLogoutConfirmDialog();
        });

        LinearLayout menuMyPage = menuView.findViewById(R.id.menu_mypage);
        menuMyPage.setOnClickListener(v -> {
            popupWindow.dismiss();
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            startActivity(intent);
        });
    }

    // ─── 앱 종료 확인 다이얼로그 (뒤로가기 시 호출) ───
    //  측정/카운트다운 진행 중이면 추가 경고 문구 표시
    private void showExitConfirmDialog() {
        boolean tracking = runningFragment != null && runningFragment.hasActiveTracking();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        int p = dp(24);
        root.setPadding(p, p, p, dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("앱 종료");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        if (tracking) {
            tvMsg.setText("측정 중인 기록이 있습니다.\n지금 종료하면 측정 기록이 삭제됩니다.\n정말 종료하시겠습니까?");
        } else {
            tvMsg.setText("정말 앱을 종료하시겠습니까?");
        }
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);
        tvMsg.setLineSpacing(dp(4), 1f);

        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgP.topMargin = dp(16);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF333333);

        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("종료");
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));

        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(20));
        confirmBg.setColor(0xFFFF3B30);
        btnConfirm.setBackground(confirmBg);

        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        confirmP.setMarginStart(dp(8));
        btnConfirm.setLayoutParams(confirmP);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // 액티비티 종료 — 측정 중이었다면 onDestroy 흐름으로 서비스도 정리됨
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

    private void showLogoutConfirmDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        int p = dp(24);
        root.setPadding(p, p, p, dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("로그아웃");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText("정말 로그아웃 하시겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);

        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgP.topMargin = dp(16);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF333333);

        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("로그아웃");
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));

        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(20));
        confirmBg.setColor(0xFFFF3B30);
        btnConfirm.setBackground(confirmBg);

        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        confirmP.setMarginStart(dp(8));
        btnConfirm.setLayoutParams(confirmP);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            TokenManager.clearTokens(MainActivity.this);
            ApiClient.resetInstance();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /*
     * 미확인 알림이 있으면 알림 아이콘에 빨간 점을 표시하는 함수임
     */
    private void checkUnreadNotifications() {
        new NotificationRepository(this).getNotifications(
                new NotificationRepository.RepositoryCallback<java.util.List<NotificationResponse>>() {
                    @Override
                    public void onSuccess(java.util.List<NotificationResponse> data) {
                        boolean hasUnread = false;

                        for (NotificationResponse n : data) {
                            if (!n.isRead()) {
                                hasUnread = true;
                                break;
                            }
                        }

                        if (badgeNotification != null) {
                            badgeNotification.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (badgeNotification != null) {
                            badgeNotification.setVisibility(View.GONE);
                        }
                    }
                }
        );
    }

    /*
     * 서버에서 프로필 사진 URL을 받아 우측 상단 btnProfile 에 원형으로 로드한다.
     */
    private void loadProfileButton() {
        if (myPageRepository == null || btnProfile == null) return;
        myPageRepository.getUserProfile(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                String photo = response.body().profilePhoto;
                if (photo == null || photo.trim().isEmpty()) return;

                // 상대 경로면 BASE_URL 붙이기
                if (!photo.startsWith("http://") && !photo.startsWith("https://")) {
                    String base = BuildConfig.BASE_URL;
                    if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                    photo = base + (photo.startsWith("/") ? photo : "/" + photo);
                }

                final String finalUrl = photo;
                runOnUiThread(() ->
                        Glide.with(MainActivity.this)
                                .load(finalUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(btnProfile)
                );
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // 실패 시 기본 아이콘 유지
            }
        });
    }

    // ─── 모든 Fragment를 컨테이너에 add + hide 후 초기 탭만 show (최초 onCreate에서 1회) ───
    //  - initialTab: "running" | "record" | "coaching"
    //  - recordMode: 외부 인텐트로 record 탭으로 진입할 때 전달되는 부가 정보 (없으면 null)
    private void setupFragments(String initialTab, String recordMode) {
        runningFragment  = new RunningFragment();
        recordFragment   = new RecordFragment();
        coachingFragment = new CoachingFragment();

        // RecordFragment 초기 진입 시 record_mode 전달
        if ("record".equals(initialTab) && recordMode != null) {
            Bundle bundle = new Bundle();
            bundle.putString("record_mode", recordMode);
            recordFragment.setArguments(bundle);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.add(R.id.fragment_container, runningFragment,  TAG_RUNNING).hide(runningFragment);
        tx.add(R.id.fragment_container, recordFragment,   TAG_RECORD).hide(recordFragment);
        tx.add(R.id.fragment_container, coachingFragment, TAG_COACHING).hide(coachingFragment);

        // 초기 탭만 show
        Fragment initial;
        switch (initialTab) {
            case "record":   initial = recordFragment;   break;
            case "coaching": initial = coachingFragment; break;
            default:         initial = runningFragment;  break;
        }
        tx.show(initial);
        tx.commit();
        activeFragment = initial;
    }

    // ─── 탭 전환: 현재 보이는 fragment를 hide하고 target을 show (인스턴스·view·상태 보존) ───
    private void showFragment(Fragment target) {
        if (target == null) return;

        // 상세 화면(replace + addToBackStack)이 떠 있으면 보존 fragment들이 detach 상태이므로
        //  탭 전환 전에 백스택을 모두 정리해 hidden 상태를 복원시킨다.
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.executePendingTransactions();
        }

        if (target == activeFragment) return;

        FragmentTransaction tx = fm.beginTransaction();
        if (activeFragment != null) tx.hide(activeFragment);
        tx.show(target);
        tx.commit();
        activeFragment = target;
    }

    private void updateTabUI(String selectedTab) {
        int defaultColor = Color.parseColor("#FFFFFF");
        int activeColor = Color.parseColor("#CCFF00");

        setTabColor(ivRunning, tvRunning, defaultColor);
        setTabColor(ivRecord, tvRecord, defaultColor);
        setTabColor(ivCoaching, tvCoaching, defaultColor);
        setTabColor(ivCommunity, tvCommunity, defaultColor);

        if ("running".equals(selectedTab)) {
            setTabColor(ivRunning, tvRunning, activeColor);
        } else if ("record".equals(selectedTab)) {
            setTabColor(ivRecord, tvRecord, activeColor);
        } else if ("coaching".equals(selectedTab)) {
            setTabColor(ivCoaching, tvCoaching, activeColor);
        } else if ("community".equals(selectedTab)) {
            setTabColor(ivCommunity, tvCommunity, activeColor);
        }
    }

    private void setTabColor(ImageView iv, TextView tv, int color) {
        if (iv != null && tv != null) {
            iv.setImageTintList(ColorStateList.valueOf(color));
            tv.setTextColor(color);
        }
    }
}