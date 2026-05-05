package com.neostride.app.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;
import com.neostride.app.feature.feed.FeedFragment;
import com.neostride.app.feature.notification.NotificationFragment;
import com.neostride.app.feature.tip.TipFragment;
import com.neostride.app.feature.search.SearchFragment;
import com.neostride.app.feature.event.EventFragment;
//import com.neostride.app.feature.mypage.MyPageActivity;

public class CommunityActivity extends AppCompatActivity {

    private CardView tabBackToMain;
    private LinearLayout tabFeed, tabTip, tabSearch, tabEvent;
    private ImageView ivFeed, ivTip, ivSearch, ivEvent;
    private TextView tvFeed, tvTip, tvSearch, tvEvent;
    private ImageView btnNotification; //알림버튼 변수 선언
    private ImageView btnMyPage; //마이페이지 변수 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);

        initViews();

        if (savedInstanceState == null) {
            replaceFragment(new FeedFragment());
            updateTabUI("feed");
        }

        setTabListeners();
    }

    private void initViews() {
        tabBackToMain = findViewById(R.id.tab_back_to_main);
        tabFeed = findViewById(R.id.tab_feed);
        tabTip = findViewById(R.id.tab_tip);
        tabSearch = findViewById(R.id.tab_search);
        tabEvent = findViewById(R.id.tab_event);

        ivFeed = findViewById(R.id.iv_feed);
        ivTip = findViewById(R.id.iv_tip);
        ivSearch = findViewById(R.id.iv_search);
        ivEvent = findViewById(R.id.iv_event);

        tvFeed = findViewById(R.id.tv_feed);
        tvTip = findViewById(R.id.tv_tip);
        tvSearch = findViewById(R.id.tv_search);
        tvEvent = findViewById(R.id.tv_event);

//        btnNotification = findViewById(R.id.btn_notification);
//        btnMyPage = findViewById(R.id.btn_mypage);
    }

    private void setTabListeners() {
        // 뒤로가기 버튼 기능 (현재 액티비티 종료 -> 메인으로 돌아감)
        if (tabBackToMain != null) tabBackToMain.setOnClickListener(v -> finish());

        // 알림 버튼 클릭 시 프래그먼트 교체
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                replaceFragment(new NotificationFragment());
                updateTabUI("none"); // 하단 탭들의 하이라이트를 모두 끔
            });
        }
//        // 마이페이지 버튼 클릭 시 액티비티 교체
//        if (btnMyPage != null) {
//            btnMyPage.setOnClickListener(v -> {
//                Intent intent = new Intent(this, MyPageActivity.class);
//                startActivity(intent);
//            });
//        }

        if (tabFeed != null) tabFeed.setOnClickListener(v -> {
            replaceFragment(new FeedFragment());
            updateTabUI("feed");
        });

        if (tabTip != null) tabTip.setOnClickListener(v -> {
            replaceFragment(new TipFragment());
            updateTabUI("tip");
        });

        if (tabSearch != null) tabSearch.setOnClickListener(v -> {
            replaceFragment(new SearchFragment());
            updateTabUI("search");
        });

        if (tabEvent != null) tabEvent.setOnClickListener(v -> {
            replaceFragment(new EventFragment());
            updateTabUI("event");
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.community_container, fragment)
                .commit();
    }

    private void updateTabUI(String selectedTab) {
        int defaultColor = Color.parseColor("#FFFFFF");
        int activeColor = Color.parseColor("#CCFF00");

        setTabColor(ivFeed, tvFeed, defaultColor);
        setTabColor(ivTip, tvTip, defaultColor);
        setTabColor(ivSearch, tvSearch, defaultColor);
        setTabColor(ivEvent, tvEvent, defaultColor);

        switch (selectedTab) {
            case "feed": setTabColor(ivFeed, tvFeed, activeColor); break;
            case "tip": setTabColor(ivTip, tvTip, activeColor); break;
            case "search": setTabColor(ivSearch, tvSearch, activeColor); break;
            case "event": setTabColor(ivEvent, tvEvent, activeColor); break;
        }
    }

    private void setTabColor(ImageView iv, TextView tv, int color) {
        if (iv != null && tv != null) {
            iv.setImageTintList(ColorStateList.valueOf(color));
            tv.setTextColor(color);
        }
    }
}