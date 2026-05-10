package com.neostride.app.feature.feed;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.feed.model.FeedItem;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import com.neostride.app.common.network.MockFeedStorage;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.feed.api.FeedApi;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedFragment extends Fragment {

    // 피드 목록을 보여줄 RecyclerView 변수 선언함
    private RecyclerView rvFeedList;

    // 피드 목록과 RecyclerView를 연결해줄 Adapter 변수 선언함
    private FeedAdapter feedAdapter;

    // 피드 데이터를 담을 리스트 변수 선언함
    private List<FeedItem> feedItemList;

    public FeedFragment() {
        // Fragment 기본 생성자가 필요함
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

        // 피드 데이터를 담을 리스트를 초기화함
        //feedItemList = MockFeedStorage.getFeedItemList();
        //FeedApi feedApi = ApiClient.getInstance().create(FeedApi.class);



        // 실제 서버 연결 전/응답 전에는 빈 리스트로 초기화함
        feedItemList = new ArrayList<>();

        FeedApi feedApi = ApiClient.getInstance().create(FeedApi.class);

        feedApi.getFeedList().enqueue(new Callback<List<FeedUploadResponse>>() {
            @Override
            public void onResponse(
                    Call<List<FeedUploadResponse>> call,
                    Response<List<FeedUploadResponse>> response
            ) {

                if (response.isSuccessful() && response.body() != null) {

                    // 여기서 RecyclerView 데이터 갱신
                }
            }

            @Override
            public void onFailure(
                    Call<List<FeedUploadResponse>> call,
                    Throwable t
            ) {

            }
        });

        // 서버 연결 전까지는 더미 데이터를 넣지 않음
        // 나중에 서버 API가 완성되면 여기에서 피드 목록을 불러오면 됨

        // Adapter를 생성하고 빈 피드 리스트를 넘겨줌
        feedAdapter = new FeedAdapter(feedItemList);

        // RecyclerView를 세로 리스트 형태로 설정함
        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // RecyclerView에 Adapter를 연결함
        rvFeedList.setAdapter(feedAdapter);

        // 글쓰기 버튼 클릭 이벤트를 연결함
        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);

        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.neostride.app.activity.MainActivity.class);

                // 메인 화면에서 기록 탭으로 이동하라는 값 전달함
                intent.putExtra("move_to", "record");

                // 기록 화면을 피드 업로드용 선택 모드로 열기 위한 값 전달함
                intent.putExtra("record_mode", "feed_upload");

                startActivity(intent);
            });
        }
    }
}