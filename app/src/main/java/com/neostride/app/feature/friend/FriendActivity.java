package com.neostride.app.feature.friend;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.friend.api.FriendApi;
import com.neostride.app.feature.friend.repository.FriendRepository;
import java.util.ArrayList;
import java.util.List;

public class FriendActivity extends AppCompatActivity {

    private FriendAdapter adapter;
    private FriendRepository repository;
    private List<TextView> tabs = new ArrayList<>();
    private final String[] statusKeys = {"friends", "sent", "received", "blocked"};

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
        rvFriends.setAdapter(adapter);

        // 3. UI 컴포넌트 설정 (뒤로가기 버튼 및 탭 초기화)
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        initTabs();

        // 4. 초기 진입 시 스타일 즉시 적용 (0번 인덱스: 친구목록)
        updateTabUI(0);

        // 5. 초기 데이터 로드
        loadData("friends");
    }

    /**
     * 상단 탭 버튼들을 리스트에 담고 클릭 리스너를 설정합니다.
     */
    private void initTabs() {
        tabs.add(findViewById(R.id.tab_friend_list));
        tabs.add(findViewById(R.id.tab_sent_requests));
        tabs.add(findViewById(R.id.tab_received_requests));
        tabs.add(findViewById(R.id.tab_blocked_list));

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            tabs.get(i).setOnClickListener(v -> {
                updateTabUI(index);       // UI 변경
                loadData(statusKeys[index]); // 데이터 갱신
            });
        }
    }

    /**
     * 선택된 탭은 검정색(형광색 배경), 나머지는 흰색(배경 없음)으로 스타일을 업데이트합니다.
     */
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

    /**
     * 서버로부터 상태별 데이터를 가져옵니다.
     */
    private void loadData(String status) {
        repository.fetchFriendList(status, list -> {
            if (list != null) {
                adapter.setFriendList(list, status);
            }
        });
    }
}