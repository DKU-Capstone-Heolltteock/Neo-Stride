package com.neostride.app.feature.friend;

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

        // 3. UI 컴포넌트 설정
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        initTabs();

        // 4. 초기 실행 시 '친구목록' 데이터 로드
        loadData("friends");
    }

    /**
     * 상단 탭 버튼들을 초기화하고 클릭 리스너를 설정합니다.
     */
    private void initTabs() {
        tabs.add(findViewById(R.id.tab_friend_list));
        tabs.add(findViewById(R.id.tab_sent_requests));
        tabs.add(findViewById(R.id.tab_received_requests));
        tabs.add(findViewById(R.id.tab_blocked_list));

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            tabs.get(i).setOnClickListener(v -> {
                updateTabUI(index);       // 선택된 탭 강조
                loadData(statusKeys[index]); // 해당 상태의 데이터 로드
            });
        }
    }

    /**
     * 선택된 탭의 배경색과 글자색을 변경합니다.
     */
    private void updateTabUI(int selectedIndex) {
        for (int i = 0; i < tabs.size(); i++) {
            if (i == selectedIndex) {
                // 선택됨: bg_badge_btn (초록색 배경) 적용 및 검은색 글자
                tabs.get(i).setBackgroundResource(R.drawable.bg_badge_btn);
                tabs.get(i).setTextColor(0xFF000000);
            } else {
                // 미선택: 배경 제거 및 흰색 글자
                tabs.get(i).setBackground(null);
                tabs.get(i).setTextColor(0xFFFFFFFF);
            }
        }
    }

    /**
     * 서버(혹은 MockServer)로부터 친구 리스트 데이터를 가져와 어댑터에 전달합니다.
     */
    private void loadData(String status) {
        repository.fetchFriendList(status, list -> {
            if (list != null) {
                adapter.setFriendList(list, status);
            }
        });
    }
}