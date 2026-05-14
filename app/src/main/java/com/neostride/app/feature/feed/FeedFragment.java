package com.neostride.app.feature.feed;

import android.content.Intent;
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
import com.neostride.app.activity.MainActivity;
import com.neostride.app.feature.feed.model.FeedItem;
import com.neostride.app.feature.feed.model.FeedUploadResponse;
import com.neostride.app.feature.feed.repository.FeedRepository;

import java.util.ArrayList;
import java.util.List;

/*
 * 커뮤니티 피드 화면을 담당하는 Fragment 클래스임
 * FeedRepository를 통해 MockFeedServer 또는 실제 서버에서 피드 목록을 받아와 RecyclerView에 표시함
 */
public class FeedFragment extends Fragment {

    // 피드 목록을 보여줄 RecyclerView 변수 선언함
    private RecyclerView rvFeedList;

    // 피드 목록과 RecyclerView를 연결해줄 Adapter 변수 선언함
    private FeedAdapter feedAdapter;

    // 피드 데이터를 담을 리스트 변수 선언함
    private List<FeedItem> feedItemList;

    // 피드 데이터 처리를 담당하는 Repository 변수 선언함
    private FeedRepository feedRepository;

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

        // Repository를 생성함
        feedRepository = new FeedRepository();

        // 피드 데이터를 담을 리스트를 빈 리스트로 초기화함
        feedItemList = new ArrayList<>();

        // Adapter를 생성하고 피드 리스트를 넘겨줌
        feedAdapter = new FeedAdapter(feedItemList);

        // RecyclerView를 세로 리스트 형태로 설정함
        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // RecyclerView에 Adapter를 연결함
        rvFeedList.setAdapter(feedAdapter);

        // 피드 목록을 불러옴
        loadFeedList();

        // 글쓰기 버튼 클릭 이벤트를 연결함
        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);

        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);

                // 메인 화면에서 기록 탭으로 이동하라는 값 전달함
                intent.putExtra("move_to", "record");

                // 기록 화면을 피드 업로드용 선택 모드로 열기 위한 값 전달함
                intent.putExtra("record_mode", "feed_upload");

                startActivity(intent);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 피드 화면으로 다시 돌아왔을 때 MockFeedServer에 새로 추가된 피드가 있으면 다시 반영함
        if (feedRepository != null && feedAdapter != null) {
            loadFeedList();
        }
    }

    /*
     * 피드 목록을 조회하는 함수임
     * Mock 모드에서는 MockFeedServer 데이터를 받아오고,
     * 서버 모드에서는 실제 서버 데이터를 받아옴
     */
    private void loadFeedList() {
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedUploadResponse>>() {
            @Override
            public void onSuccess(List<FeedUploadResponse> data) {
                // Fragment가 이미 화면에서 사라진 경우 UI 작업을 하지 않음
                if (!isAdded()) {
                    return;
                }

                // 기존 피드 목록을 비움
                feedItemList.clear();

                // Repository에서 받은 응답 목록을 화면용 FeedItem 목록으로 변환함
                for (FeedUploadResponse feedResponse : data) {
                    FeedItem feedItem = convertResponseToFeedItem(feedResponse);
                    feedItemList.add(feedItem);
                }

                // RecyclerView에 데이터 변경을 알림
                feedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                // Fragment가 이미 화면에서 사라진 경우 UI 작업을 하지 않음
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /*
     * 서버 응답 DTO 또는 Mock 응답 DTO를 RecyclerView에서 사용할 FeedItem 객체로 변환하는 함수임
     */
    private FeedItem convertResponseToFeedItem(FeedUploadResponse response) {
        return new FeedItem(
                getSafeText(response.getProfileImageUrl()),
                getSafeText(response.getNickname(), "알 수 없음"),
                getSafeText(response.getCreatedAt(), "방금 전"),
                getSafeText(response.getTitle()),
                getSafeText(response.getContent()),
                response.getTaggedCount(),
                response.getLikeCount(),
                response.getCommentCount(),
                getSafeText(response.getDistance(), "0.00 km"),
                getSafeText(response.getDuration(), "00:00"),
                getSafeText(response.getPace(), "0:00/km"),
                response.isMapVisible(),
                response.getRouteMapImageUri(),
                response.getImageUrls()
        );
    }

    /*
     * null 값을 빈 문자열로 바꿔주는 함수임
     */
    private String getSafeText(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    /*
     * null 또는 빈 문자열일 때 기본값을 넣어주는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }
}