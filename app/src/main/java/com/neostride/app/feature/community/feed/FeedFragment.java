package com.neostride.app.feature.community.feed;

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
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.feed.model.FeedItem;
import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.feed.repository.FeedRepository;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.mypage.repository.MyPageRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                (record, routeMapUri, address) -> {
                    // picker는 뒤에 유지 — FeedUploadDialog 위에 열림
                    // routeMapUri: FeedRecordDetailDialog 에서 캡처한 지도 스냅샷 (null 가능)
                    // address: 피드 업로드에서는 사용하지 않음
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
     * 피드 API가 is_bookmarked를 정확히 반환하지 않는 문제를 보완하기 위해
     * 북마크 목록을 병렬로 조회하여 bookmarked 상태를 교차 설정함
     */
    private void loadFeedList() {
        // 4-API 병렬: 전체 피드 + 북마크 + 좋아요 + 댓글
        // 목록 API가 isBookmarked/isLiked/isCommented를 per-user로 정확히 안 내려줄 수 있어 교차 확인함
        final List<FeedResponse>[] feedResponseHolder = new List[]{null};
        final Set<Long> bookmarkedIds = new HashSet<>();
        final Set<Long> likedIds = new HashSet<>();
        final Set<Long> commentedIds = new HashSet<>();
        final int[] pending = {4};
        final String[] errorMsg = {null};

        Runnable onAllDone = () -> {
            if (!isAdded()) return;
            if (errorMsg[0] != null && feedResponseHolder[0] == null) {
                Toast.makeText(requireContext(), errorMsg[0], Toast.LENGTH_SHORT).show();
                return;
            }
            List<FeedResponse> data = feedResponseHolder[0] != null
                    ? feedResponseHolder[0] : new ArrayList<>();
            feedItemList.clear();
            for (FeedResponse feedResponse : data) {
                FeedItem item = convertResponseToFeedItem(feedResponse);
                Long feedId = feedResponse.getFeedId();
                if (feedId != null) {
                    // 교차 확인: 목록 API값 OR 별도 목록에 있으면 true
                    if (bookmarkedIds.contains(feedId)) item.setBookmarked(true);
                    if (likedIds.contains(feedId))      item.setLiked(true);
                    if (commentedIds.contains(feedId))  item.setCommented(true);
                }
                feedItemList.add(item);
            }
            feedAdapter.notifyDataSetChanged();
        };

        // API 1: 피드 목록
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedResponse>>() {
            @Override
            public void onSuccess(List<FeedResponse> data) {
                feedResponseHolder[0] = data;
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onError(String message) {
                errorMsg[0] = message;
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        MyPageRepository myPageRepo = new MyPageRepository();

        // API 2: 내가 북마크한 피드 목록 (isBookmarked 교차 확인용)
        myPageRepo.getBookmarkedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call,
                                   Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (CommunityContentResponse f : response.body())
                        bookmarkedIds.add((long) f.contentId);
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        // API 3: 내가 좋아요한 피드 목록 (isLiked 교차 확인용)
        myPageRepo.getLikedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call,
                                   Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (CommunityContentResponse f : response.body())
                        likedIds.add((long) f.contentId);
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        // API 4: 내가 댓글 단 피드 목록 (isCommented 교차 확인용)
        myPageRepo.getCommentedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call,
                                   Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (CommunityContentResponse f : response.body())
                        commentedIds.add((long) f.contentId);
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--pending[0] == 0) onAllDone.run();
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
        return TimeFormatter.format(isoTime);
    }

    private String getSafeText(String value) {
        return value != null ? value : "";
    }

    private String getSafeText(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
}
