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
import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.feed.repository.FeedRepository;

import java.util.ArrayList;
import java.util.List;

/*
 * 커뮤니티 피드 화면 Fragment임
 *
 * - FeedRepository를 통해 피드 목록을 받아와 RecyclerView에 표시함
 * - onResume 시 목록을 재조회하여 새로 작성된 피드를 즉시 반영함
 */
public class FeedFragment extends Fragment {

    private RecyclerView rvFeedList;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItemList;
    private FeedRepository feedRepository;

    public FeedFragment() {
        // Fragment 기본 생성자임
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 피드 Fragment 레이아웃을 화면에 붙임
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // RecyclerView를 XML에서 찾아옴
        rvFeedList = view.findViewById(R.id.rv_feed_list);

        // Context가 필요한 Repository를 생성함
        feedRepository = new FeedRepository(requireContext());

        // 피드 데이터를 담을 리스트를 빈 리스트로 초기화함
        feedItemList = new ArrayList<>();

        // RecyclerView 어댑터를 생성함
        feedAdapter = new FeedAdapter(feedItemList);

        // 피드 목록을 세로 리스트 형태로 표시함
        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // RecyclerView에 어댑터를 연결함
        rvFeedList.setAdapter(feedAdapter);

        // 피드 목록을 서버 또는 Mock 서버에서 불러옴
        loadFeedList();

        // 피드 작성 버튼을 가져옴
        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);

        // 피드 작성 버튼이 존재할 경우 클릭 이벤트를 설정함
        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> {
                // MainActivity로 이동하면서 기록 화면의 피드 업로드 모드로 전환하도록 값 전달함
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra("move_to", "record");
                intent.putExtra("record_mode", "feed_upload");
                startActivity(intent);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 피드 작성 후 돌아왔을 때 새 피드가 바로 반영되도록 다시 조회함
        if (feedRepository != null && feedAdapter != null) {
            loadFeedList();
        }
    }

    /*
     * 피드 목록을 조회하는 함수임
     */
    private void loadFeedList() {
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedResponse>>() {
            @Override
            public void onSuccess(List<FeedResponse> data) {
                // Fragment가 이미 화면에서 사라진 경우 UI 작업을 하지 않음
                if (!isAdded()) {
                    return;
                }

                // 기존 피드 목록을 비움
                feedItemList.clear();

                // Repository에서 받은 응답 목록을 화면용 FeedItem 목록으로 변환함
                for (FeedResponse feedResponse : data) {
                    FeedItem feedItem = convertResponseToFeedItem(feedResponse);
                    feedItemList.add(feedItem);
                }

                // RecyclerView에 데이터 변경을 알림
                feedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                // Fragment가 이미 화면에서 사라진 경우 Toast를 띄우지 않음
                if (!isAdded()) {
                    return;
                }

                // 피드 목록 조회 실패 메시지를 표시함
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * 서버 응답 DTO 또는 Mock 응답 DTO를 RecyclerView에서 사용할 FeedItem 객체로 변환하는 함수임
     */
    private FeedItem convertResponseToFeedItem(FeedResponse response) {
        return new FeedItem(
                response.getFeedId(),
                response.getWriterId(),
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
     * 문자열이 null이면 빈 문자열로 바꿔주는 함수임
     */
    private String getSafeText(String value) {
        return value != null ? value : "";
    }

    /*
     * 문자열이 null이거나 비어 있으면 기본값으로 바꿔주는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
}