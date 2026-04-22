package com.neostride.app.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;
import com.neostride.app.feature.running.RunningFragment;
import com.neostride.app.feature.record.RecordFragment;
import com.neostride.app.feature.coaching.CoachingFragment;
import com.neostride.app.feature.notification.NotificationFragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout tabRunning, tabRecord, tabCoaching, tabCommunity;
    private ImageView ivRunning, ivRecord, ivCoaching, ivCommunity;
    private TextView tvRunning, tvRecord, tvCoaching, tvCommunity;
    private ImageView btnNotification, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initViews();

        if (savedInstanceState == null) {
            replaceFragment(new RunningFragment());
            updateTabUI("running");
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
        tabRunning.setOnClickListener(v -> { replaceFragment(new RunningFragment()); updateTabUI("running"); });
        tabRecord.setOnClickListener(v -> { replaceFragment(new RecordFragment()); updateTabUI("record"); });
        tabCoaching.setOnClickListener(v -> { replaceFragment(new CoachingFragment()); updateTabUI("coaching"); });

        // 메인 하단 탭의 커뮤니티는 기존처럼 액티비티 이동 유지
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

    // 프로필 메뉴 팝업 생성 및 표시
    private void showProfileMenu(View anchorView) {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_profile_menu, null);

        int width = (int) (220 * getResources().getDisplayMetrics().density);
        final PopupWindow popupWindow = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(25);

        // 팝업 표시 위치 설정
        popupWindow.showAsDropDown(anchorView, -width + anchorView.getWidth(), 15);
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

        if (selectedTab.equals("running")) setTabColor(ivRunning, tvRunning, activeColor);
        else if (selectedTab.equals("record")) setTabColor(ivRecord, tvRecord, activeColor);
        else if (selectedTab.equals("coaching")) setTabColor(ivCoaching, tvCoaching, activeColor);
        else if (selectedTab.equals("community")) setTabColor(ivCommunity, tvCommunity, activeColor);
    }

    private void setTabColor(ImageView iv, TextView tv, int color) {
        if (iv != null && tv != null) {
            iv.setImageTintList(ColorStateList.valueOf(color));
            tv.setTextColor(color);
        }
    }
}