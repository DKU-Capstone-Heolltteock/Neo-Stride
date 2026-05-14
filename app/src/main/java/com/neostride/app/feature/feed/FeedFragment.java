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

//  커뮤니티 피드 화면 Fragment
//  <p>
//  - FeedRepository를 통해 피드 목록을 받아와 RecyclerView에 표시한다.
//  - onResume 시 목록을 재조회하여 새로 작성된 피드를 즉시 반영한다.

public class FeedFragment extends Fragment {

    private RecyclerView rvFeedList;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItemList;
    private FeedRepository feedRepository;

    public FeedFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFeedList = view.findViewById(R.id.rv_feed_list);
        feedRepository = new FeedRepository();
        feedItemList = new ArrayList<>();

        feedAdapter = new FeedAdapter(feedItemList);
        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFeedList.setAdapter(feedAdapter);

        loadFeedList();

        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);
        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> {
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
        // 피드 작성 후 돌아왔을 때 새 피드 즉시 반영
        if (feedRepository != null && feedAdapter != null) {
            loadFeedList();
        }
    }

    // ─── 피드 목록 조회 ───
    private void loadFeedList() {
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedUploadResponse>>() {
            @Override
            public void onSuccess(List<FeedUploadResponse> data) {
                if (!isAdded()) return;

                feedItemList.clear();
                for (FeedUploadResponse response : data) {
                    feedItemList.add(convertResponseToFeedItem(response));
                }
                feedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── FeedUploadResponse → FeedItem 변환 ───
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

    private String getSafeText(String value) {
        return value != null ? value : "";
    }

    private String getSafeText(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
}