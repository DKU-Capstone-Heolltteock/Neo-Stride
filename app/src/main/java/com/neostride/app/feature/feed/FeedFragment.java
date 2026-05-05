package com.neostride.app.feature.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

import java.util.ArrayList;
import java.util.List;
import com.neostride.app.feature.feed.model.FeedItem;
import android.widget.ImageView;

public class FeedFragment extends Fragment {

    // 피드 목록을 보여줄 RecyclerView 변수 선언함
    private RecyclerView rvFeedList;

    // 피드 목록과 RecyclerView를 연결해줄 Adapter 변수 선언함
    private FeedAdapter feedAdapter;

    // 피드 데이터를 담을 리스트 변수 선언함
    private List<FeedItem> feedItemList;

    public FeedFragment() {
        // Fragment는 기본 생성자가 필요함
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // fragment_feed.xml 레이아웃을 화면에 연결함
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // XML에 있는 RecyclerView를 Java 코드와 연결함
        rvFeedList = view.findViewById(R.id.rv_feed_list);

        // 피드 데이터를 초기화함
        feedItemList = new ArrayList<>();

        // 임시 피드 데이터를 추가함
        loadDummyFeedData();

        // Adapter를 생성하고 피드 데이터를 넘겨줌
        feedAdapter = new FeedAdapter(feedItemList);

        // RecyclerView를 세로 리스트 형태로 설정함
        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // RecyclerView에 Adapter를 연결함
        rvFeedList.setAdapter(feedAdapter);

        // 글쓰기 버튼 클릭 이벤트를 연결함
        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);

        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "피드 작성 화면으로 이동 예정", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadDummyFeedData() {
        // 서버 연결 전까지 화면 확인용으로 임시 데이터를 넣어둠
        feedItemList.add(new FeedItem(
                "JinzaYoungjae3218",
                "7분 전",
                "오운완",
                2,
                3,
                2,
                "11.8km",
                "37:49",
                "6:24/km",
                0,
                0
        ));

        // 피드가 여러 개일 때 화면이 어떻게 보이는지 확인하기 위한 임시 데이터임
        feedItemList.add(new FeedItem(
                "RunnerNeo",
                "15분 전",
                "오늘 러닝 완료",
                5,
                12,
                4,
                "5.9km",
                "28:10",
                "4:46/km",
                0,
                0
        ));
    }
}