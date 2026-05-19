package com.neostride.app.feature.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
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
 * - 글쓰기 버튼을 누르면 기록 선택 다이얼로그를 통해 러닝 기록을 선택 후 피드를 업로드함
 */
public class FeedFragment extends Fragment {

    private RecyclerView rvFeedList;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItemList;
    private FeedRepository feedRepository;

    // 현재 열린 FeedUploadDialog 참조 (사진 picker 결과 전달용)
    private FeedUploadDialog currentFeedUploadDialog;

    // 사진 선택 런처 — onCreate 에서 등록해야 함
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    public FeedFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 사진 선택 런처 등록 (최대 3장)
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(3),
                uris -> {
                    if (currentFeedUploadDialog != null && uris != null && !uris.isEmpty()) {
                        currentFeedUploadDialog.addSelectedImages(new ArrayList<>(uris));
                    }
                }
        );
    }

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
        feedRepository = new FeedRepository(requireContext());
        feedItemList = new ArrayList<>();
        feedAdapter = new FeedAdapter(feedItemList);

        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFeedList.setAdapter(feedAdapter);

        loadFeedList();

        // 글쓰기 버튼 — 기록 선택 다이얼로그 열기
        View btnWriteFeed = view.findViewById(R.id.btn_write_feed);
        if (btnWriteFeed != null) {
            btnWriteFeed.setOnClickListener(v -> openRecordPickerDialog());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (feedRepository != null && feedAdapter != null) {
            loadFeedList();
        }
    }

    /*
     * 기록 선택 다이얼로그를 열고, 기록 선택 시 FeedUploadDialog 를 이어서 표시함
     */
    private void openRecordPickerDialog() {
        // picker 참조를 람다에서 쓰기 위해 배열로 감쌈
        FeedRecordPickerDialog[] pickerRef = new FeedRecordPickerDialog[1];

        pickerRef[0] = new FeedRecordPickerDialog(
                requireContext(),
                (record, routeMapUri) -> {
                    // picker는 뒤에 유지 — FeedUploadDialog 위에 열림
                    // routeMapUri: FeedRecordDetailDialog 에서 캡처한 지도 스냅샷 (null 가능)
                    currentFeedUploadDialog = new FeedUploadDialog(
                            requireContext(),
                            record,
                            routeMapUri,
                            () -> photoPickerLauncher.launch(
                                    new PickVisualMediaRequest.Builder()
                                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                            .build()
                            ),
                            feedResponse -> {
                                // 업로드 성공 시 picker까지 함께 닫고 목록 갱신
                                pickerRef[0].dismiss();
                                loadFeedList();
                            }
                    );
                    currentFeedUploadDialog.show();
                }
        );
        pickerRef[0].show();
    }

    /*
     * 피드 목록을 조회하는 함수임
     */
    private void loadFeedList() {
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedResponse>>() {
            @Override
            public void onSuccess(List<FeedResponse> data) {
                if (!isAdded()) return;

                feedItemList.clear();
                for (FeedResponse feedResponse : data) {
                    feedItemList.add(convertResponseToFeedItem(feedResponse));
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

    /*
     * 서버 응답 DTO를 RecyclerView에서 사용할 FeedItem 객체로 변환하는 함수임
     */
    private FeedItem convertResponseToFeedItem(FeedResponse response) {
        FeedItem item = new FeedItem(
                response.getFeedId(),
                response.getWriterId(),
                getSafeText(response.getProfileImageUrl()),
                getSafeText(response.getNickname(), "알 수 없음"),
                response.isBadgeOwned(),
                response.getBadgeType(),
                formatTime(response.getCreatedAt()),
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
                response.getImageUrls(),
                response.isLiked(),
                response.isBookmarked(),
                response.isCommented(),
                response.isTagged()
        );
        item.setMine(response.isMine());
        return item;
    }

    private String formatTime(String isoTime) {
        return com.neostride.app.feature.community.common.util.TimeFormatter.format(isoTime);
    }

    private String getSafeText(String value) {
        return value != null ? value : "";
    }

    private String getSafeText(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
}
