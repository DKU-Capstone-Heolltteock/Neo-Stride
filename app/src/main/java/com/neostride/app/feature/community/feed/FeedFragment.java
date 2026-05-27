package com.neostride.app.feature.community.feed;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    private static final int PAGE_SIZE = 5;

    private RecyclerView rvFeedList;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItemList;
    private FeedRepository feedRepository;

    // 서버에서 받아온 전체 피드 데이터 버퍼 (클라이언트 페이지네이션용)
    private List<FeedResponse> allFeedResponses = new ArrayList<>();

    // 북마크·좋아요·댓글 상태 (getFeedList 이후 비동기로 채워짐)
    private final Set<Long> feedBookmarkedIds = new HashSet<>();
    private final Set<Long> feedLikedIds      = new HashSet<>();
    private final Set<Long> feedCommentedIds  = new HashSet<>();

    // 현재 RecyclerView에 표시된 아이템 수
    private int displayedCount = 0;

    // 상태 API 3개가 모두 완료됐는지 여부
    private boolean feedStatesLoaded = false;

    // 빈 목록 안내 문구
    private TextView tvEmptyState;

    // 로딩 애니메이션
    private TextView tvLoading;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private Runnable loadingRunnable;
    private int loadingDotCount = 1;

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

        rvFeedList    = view.findViewById(R.id.rv_feed_list);
        tvLoading     = view.findViewById(R.id.tv_loading);
        tvEmptyState  = view.findViewById(R.id.tv_empty_state);
        feedRepository = new FeedRepository(requireContext());
        feedItemList = new ArrayList<>();
        feedAdapter = new FeedAdapter(feedItemList);

        rvFeedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFeedList.setAdapter(feedAdapter);

        // 스크롤 끝 근처에서 다음 PAGE_SIZE개 추가 로드
        rvFeedList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return; // 위로 스크롤은 무시
                LinearLayoutManager lm = (LinearLayoutManager) rvFeedList.getLayoutManager();
                if (lm == null) return;
                // 마지막 보이는 아이템이 현재 표시된 수의 2개 이내면 미리 로드
                if (lm.findLastVisibleItemPosition() >= displayedCount - 2
                        && displayedCount < allFeedResponses.size()) {
                    loadMoreFeeds();
                }
            }
        });

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
     *
     * [개선된 로직]
     * - API 1(피드 목록)이 도착하는 즉시 첫 PAGE_SIZE개를 화면에 표시함
     * - API 2~4(북마크·좋아요·댓글 상태)는 백그라운드에서 완료 후
     *   이미 표시된 아이템의 상태만 업데이트함 (화면이 멈추지 않음)
     * - 스크롤 끝 근처에서 다음 PAGE_SIZE개를 추가로 표시함
     */
    private void loadFeedList() {
        // 상태 초기화
        allFeedResponses = new ArrayList<>();
        feedBookmarkedIds.clear();
        feedLikedIds.clear();
        feedCommentedIds.clear();
        displayedCount = 0;
        feedStatesLoaded = false;
        feedItemList.clear();
        if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
        startLoadingAnimation();

        // 상태 API 3개(북마크·좋아요·댓글)가 모두 완료되면 이미 표시된 아이템 상태 갱신
        final int[] statePending = {3};
        Runnable onStatesReady = () -> {
            if (!isAdded()) return;
            feedStatesLoaded = true;
            for (FeedItem item : feedItemList) {
                Long id = item.getFeedId();
                if (id != null) {
                    if (feedBookmarkedIds.contains(id)) item.setBookmarked(true);
                    if (feedLikedIds.contains(id))      item.setLiked(true);
                    if (feedCommentedIds.contains(id))  item.setCommented(true);
                }
            }
            if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
        };

        // API 1: 피드 목록 — 도착 즉시 첫 PAGE_SIZE개 표시
        feedRepository.getFeedList(new FeedRepository.RepositoryCallback<List<FeedResponse>>() {
            @Override
            public void onSuccess(List<FeedResponse> data) {
                if (!isAdded()) return;
                stopLoadingAnimation();
                allFeedResponses = data != null ? data : new ArrayList<>();
                // 동시에 여러 번 loadFeedList()가 호출됐을 때 중복 방지
                feedItemList.clear();
                int count = Math.min(PAGE_SIZE, allFeedResponses.size());
                for (int i = 0; i < count; i++) {
                    feedItemList.add(convertResponseToFeedItem(allFeedResponses.get(i)));
                }
                displayedCount = count;
                if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
                // 빈 목록 안내
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(allFeedResponses.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onError(String message) {
                stopLoadingAnimation();
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
                        feedBookmarkedIds.add((long) f.contentId);
                if (--statePending[0] == 0) onStatesReady.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--statePending[0] == 0) onStatesReady.run();
            }
        });

        // API 3: 내가 좋아요한 피드 목록 (isLiked 교차 확인용)
        myPageRepo.getLikedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call,
                                   Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (CommunityContentResponse f : response.body())
                        feedLikedIds.add((long) f.contentId);
                if (--statePending[0] == 0) onStatesReady.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--statePending[0] == 0) onStatesReady.run();
            }
        });

        // API 4: 내가 댓글 단 피드 목록 (isCommented 교차 확인용)
        myPageRepo.getCommentedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call,
                                   Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (CommunityContentResponse f : response.body())
                        feedCommentedIds.add((long) f.contentId);
                if (--statePending[0] == 0) onStatesReady.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                if (--statePending[0] == 0) onStatesReady.run();
            }
        });
    }

    /*
     * 스크롤 시 다음 PAGE_SIZE개를 feedItemList에 추가하는 함수임
     * 상태 API가 완료됐으면 북마크·좋아요·댓글 상태도 함께 반영함
     */
    private void loadMoreFeeds() {
        int start = displayedCount;
        int end = Math.min(start + PAGE_SIZE, allFeedResponses.size());
        if (start >= end) return;
        for (int i = start; i < end; i++) {
            FeedItem item = convertResponseToFeedItem(allFeedResponses.get(i));
            Long id = item.getFeedId();
            if (id != null && feedStatesLoaded) {
                if (feedBookmarkedIds.contains(id)) item.setBookmarked(true);
                if (feedLikedIds.contains(id))      item.setLiked(true);
                if (feedCommentedIds.contains(id))  item.setCommented(true);
            }
            feedItemList.add(item);
        }
        displayedCount = end;
        if (feedAdapter != null) feedAdapter.notifyItemRangeInserted(start, end - start);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
    }

    /*
     * 로딩 중 텍스트 애니메이션을 시작하는 함수임
     * "로딩중." → "로딩중.." → "로딩중..." 순서로 500ms마다 전환함
     */
    private void startLoadingAnimation() {
        if (tvLoading == null) return;
        loadingDotCount = 1;
        tvLoading.setVisibility(View.VISIBLE);
        if (loadingRunnable != null) loadingHandler.removeCallbacks(loadingRunnable);
        loadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvLoading == null || tvLoading.getVisibility() != View.VISIBLE) return;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < loadingDotCount; i++) dots.append(".");
                tvLoading.setText("로딩중" + dots);
                loadingDotCount = (loadingDotCount % 3) + 1;
                loadingHandler.postDelayed(this, 500);
            }
        };
        loadingHandler.post(loadingRunnable);
    }

    /*
     * 로딩 중 텍스트 애니메이션을 중지하고 숨기는 함수임
     */
    private void stopLoadingAnimation() {
        if (loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
            loadingRunnable = null;
        }
        if (tvLoading != null) tvLoading.setVisibility(View.GONE);
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
