package com.neostride.app.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.auth.LoginActivity;
import com.neostride.app.feature.coaching.CoachingFragment;
import com.neostride.app.feature.mypage.MyPageActivity;
import com.neostride.app.feature.notification.NotificationFragment;
import com.neostride.app.feature.record.RecordFragment;
import com.neostride.app.feature.running.RunningFragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout tabRunning, tabRecord, tabCoaching, tabCommunity;
    private ImageView ivRunning, ivRecord, ivCoaching, ivCommunity;
    private TextView tvRunning, tvRecord, tvCoaching, tvCommunity;
    private ImageView btnNotification, btnProfile;

    // 알림 권한 요청 런처 (Android 13+)
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // 알림 권한 처리 후 배터리 최적화 확인
                requestBatteryOptimizationIfNeeded();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ApiClient.init(this);

        initViews();
        requestInitialPermissions(); // 앱 최초 실행 시 권한 일괄 요청

        if (savedInstanceState == null) {
            String moveTo = getIntent().getStringExtra("move_to");
            String recordMode = getIntent().getStringExtra("record_mode");

            if ("record".equals(moveTo)) {
                RecordFragment recordFragment = new RecordFragment();

                Bundle bundle = new Bundle();
                bundle.putString("record_mode", recordMode);
                recordFragment.setArguments(bundle);

                replaceFragment(recordFragment);
                updateTabUI("record");
            } else {
                replaceFragment(new RunningFragment());
                updateTabUI("running");
            }
        }

        setTabListeners();
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
    }

    private void setTabListeners() {
        tabRunning.setOnClickListener(v -> {
            replaceFragment(new RunningFragment());
            updateTabUI("running");
        });

        tabRecord.setOnClickListener(v -> {
            replaceFragment(new RecordFragment());
            updateTabUI("record");
        });

        tabCoaching.setOnClickListener(v -> {
            replaceFragment(new CoachingFragment());
            updateTabUI("coaching");
        });

        tabCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                replaceFragment(new NotificationFragment());
                updateTabUI("none");
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

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);

        menuLogout.setOnClickListener(v -> {
            TokenManager.clearTokens(MainActivity.this);
            ApiClient.resetInstance();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // XML에서 마이페이지 레이아웃 아이디(menu_mypage)를 찾습니다.
        LinearLayout menuMyPage = menuView.findViewById(R.id.menu_mypage);

        // 클릭 시 실행될 동작을 설정합니다.
        menuMyPage.setOnClickListener(v -> {
            popupWindow.dismiss(); // 팝업을 먼저 닫고
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            startActivity(intent); // 화면 이동
        });
    }

    // 앱 실행 시 필요한 권한 일괄 요청
    private void requestInitialPermissions() {
        // 1. 알림 권한 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                return; // 알림 권한 요청 후 콜백에서 배터리 최적화 요청
            }
        }
        // 알림 권한이 이미 있으면 바로 배터리 최적화 확인
        requestBatteryOptimizationIfNeeded();
    }

    // 배터리 최적화 제외 요청 (아직 제외되지 않은 경우만)
    private void requestBatteryOptimizationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void updateTabUI(String selectedTab) {
        int defaultColor = Color.parseColor("#FFFFFF");
        int activeColor = Color.parseColor("#CCFF00");

        setTabColor(ivRunning, tvRunning, defaultColor);
        setTabColor(ivRecord, tvRecord, defaultColor);
        setTabColor(ivCoaching, tvCoaching, defaultColor);
        setTabColor(ivCommunity, tvCommunity, defaultColor);

        if (selectedTab.equals("running")) {
            setTabColor(ivRunning, tvRunning, activeColor);
        } else if (selectedTab.equals("record")) {
            setTabColor(ivRecord, tvRecord, activeColor);
        } else if (selectedTab.equals("coaching")) {
            setTabColor(ivCoaching, tvCoaching, activeColor);
        } else if (selectedTab.equals("community")) {
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