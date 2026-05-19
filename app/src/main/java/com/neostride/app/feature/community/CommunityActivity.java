package com.neostride.app.feature.community;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.View;

import com.neostride.app.R;
import com.neostride.app.feature.community.feed.FeedFragment;
import com.neostride.app.feature.notification.NotificationActivity;
import com.neostride.app.feature.notification.model.NotificationResponse;
import com.neostride.app.feature.notification.repository.NotificationRepository;
import com.neostride.app.feature.community.tip.TipFragment;
import com.neostride.app.feature.community.search.SearchFragment;
import com.neostride.app.feature.community.event.EventFragment;
import com.neostride.app.feature.community.mypage.MyPageActivity;

public class CommunityActivity extends AppCompatActivity {

    private CardView tabBackToMain;
    private LinearLayout tabFeed, tabTip, tabSearch, tabEvent;
    private ImageView ivFeed, ivTip, ivSearch, ivEvent;
    private TextView tvFeed, tvTip, tvSearch, tvEvent;
    private ImageView btnNotification; //알림버튼 변수 선언
    private ImageView btnMyPage; //마이페이지 변수 선언
    private View badgeNotification; //미확인 알림 빨간 점

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

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
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

        btnNotification   = findViewById(R.id.btn_notification);
        btnMyPage         = findViewById(R.id.btn_mypage);
        badgeNotification = findViewById(R.id.badge_notification);
    }

    private void setTabListeners() {
        // 뒤로가기 버튼 기능 (현재 액티비티 종료 -> 메인으로 돌아감)
        if (tabBackToMain != null) tabBackToMain.setOnClickListener(v -> finish());

        // 알림 버튼 클릭 시 알림 액티비티로 이동 + 빨간 점 숨기기
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationActivity.class));
                if (badgeNotification != null) {
                    badgeNotification.setVisibility(View.GONE);
                }
            });
        }
        // 마이페이지 버튼 클릭 시 액티비티 교체
        if (btnMyPage != null) {
            btnMyPage.setOnClickListener(v -> {
                Intent intent = new Intent(this, MyPageActivity.class);
                startActivity(intent);
            });
        }

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

    /*
     * 미확인 알림이 있으면 알림 아이콘에 빨간 점을 표시하는 함수임
     * read == false 인 알림이 하나라도 있으면 점을 보여줌
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
                        // 조회 실패 시 점 표시 안 함
                        if (badgeNotification != null) {
                            badgeNotification.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setTabColor(ImageView iv, TextView tv, int color) {
        if (iv != null && tv != null) {
            iv.setImageTintList(ColorStateList.valueOf(color));
            tv.setTextColor(color);
        }
    }
}