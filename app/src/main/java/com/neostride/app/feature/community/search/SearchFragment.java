package com.neostride.app.feature.community.search;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.feed.FeedAdapter;
import com.neostride.app.feature.community.feed.model.FeedItem;
import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.search.model.SearchUserResponse;
import com.neostride.app.feature.community.search.repository.SearchRepository;
import com.neostride.app.feature.community.tip.TipAdapter;
import com.neostride.app.feature.community.tip.model.TipItem;
import com.neostride.app.feature.community.tip.model.TipResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 커뮤니티 검색 화면 Fragment 클래스임
 *
 * 피드/팁/프로필: page(0-indexed) 기반 무한 스크롤 페이지네이션
 * - 스크롤 끝 근접 시 다음 페이지 자동 로드 후 기존 목록에 append
 * - 탭 전환 또는 keyword 변경 시 page=0으로 리셋
 * 친구 탭: 페이지네이션 없이 전체 목록 한 번에 로드
 */
public class SearchFragment extends Fragment {

    /*
     * 페이지당 항목 수
     */
    private static final int PAGE_SIZE = 10;

    /*
     * 검색창 디바운스 딜레이 (ms)
     * 타이핑 멈춘 후 이 시간이 지나야 API 요청을 보냄
     */
    private static final long SEARCH_DEBOUNCE_MS = 300;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private EditText etSearch;
    private RecyclerView rvSearch;

    private TextView tabFeed;
    private TextView tabTip;
    private TextView tabProfile;
    private TextView tabFriend;

    private View tabIndicator;
    private FrameLayout layoutMainTabContainer;

    private LinearLayout layoutTipCategory;

    private TextView btnTipAll;
    private TextView btnTipFree;
    private TextView btnTipTraining;
    private TextView btnTipCourse;
    private TextView btnTipGear;

    private SearchRepository searchRepository;

    /*
     * 현재 선택된 메인 검색 탭임
     */
    private String currentTab = "FEED";

    /*
     * 현재 선택된 팁 카테고리임
     */
    private String currentTipCategory = "ALL";

    // ──────────────────────────────────────────────────
    // 페이지네이션 상태
    // ──────────────────────────────────────────────────

    /*
     * 현재 로드된 페이지 번호 (0-indexed)
     */
    private int currentPage = 0;

    /*
     * 현재 API 요청이 진행 중이면 true — 중복 요청 방지
     */
    private boolean isLoading = false;

    /*
     * 더 불러올 데이터가 없으면 false — 스크롤 감지 중단
     */
    private boolean hasMore = true;

    // ──────────────────────────────────────────────────
    // 어댑터 & 데이터 리스트 (어댑터와 같은 참조를 공유)
    // append 시 리스트에 추가 후 notifyItemRangeInserted 호출
    // ──────────────────────────────────────────────────

    private FeedAdapter feedAdapter;
    private final List<FeedItem> feedItemList = new ArrayList<>();

    private TipAdapter tipAdapter;
    private final ArrayList<TipItem> tipItemList = new ArrayList<>();
    private final Map<Long, Boolean>  tipLikedMap       = new HashMap<>();
    private final Map<Long, Boolean>  tipBookmarkedMap   = new HashMap<>();
    private final Map<Long, Integer>  tipLikeCountMap    = new HashMap<>();

    private SearchUserAdapter userAdapter;
    private final List<SearchUserResponse> userItemList = new ArrayList<>();

    public SearchFragment() {
        // Fragment 기본 생성자가 필요함
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initRecyclerView();
        initRepository();
        initListeners();

        applyTipCategoryStyles(currentTipCategory);

        /*
         * 최초 진입 시 피드 탭으로 시작함
         */
        changeMainTab("FEED");
    }

    /*
     * XML View들을 Java 코드와 연결하는 함수임
     */
    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        rvSearch = view.findViewById(R.id.rv_search);

        tabFeed    = view.findViewById(R.id.tab_feed);
        tabTip     = view.findViewById(R.id.tab_tip);
        tabProfile = view.findViewById(R.id.tab_profile);
        tabFriend  = view.findViewById(R.id.tab_friend);

        tabIndicator         = view.findViewById(R.id.tab_indicator);
        layoutMainTabContainer = view.findViewById(R.id.layout_main_tab_container);

        layoutTipCategory = view.findViewById(R.id.layout_tip_category);

        btnTipAll      = view.findViewById(R.id.btn_tip_all);
        btnTipFree     = view.findViewById(R.id.btn_tip_free);
        btnTipTraining = view.findViewById(R.id.btn_tip_training);
        btnTipCourse   = view.findViewById(R.id.btn_tip_course);
        btnTipGear     = view.findViewById(R.id.btn_tip_gear);
    }

    /*
     * RecyclerView 기본 설정 + 무한 스크롤 리스너를 초기화하는 함수임
     */
    private void initRecyclerView() {
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));

        rvSearch.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                /*
                 * 아래 방향 스크롤이 아니거나 로딩 중이거나 더 이상 데이터 없으면 무시함
                 * 친구 탭은 페이지네이션이 없으므로 무시함
                 */
                if (dy <= 0 || isLoading || !hasMore || "FRIEND".equals(currentTab)) return;

                LinearLayoutManager lm = (LinearLayoutManager) rvSearch.getLayoutManager();
                if (lm == null) return;

                int total       = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();

                /*
                 * 마지막에서 3번째 아이템이 보이면 다음 페이지 요청함
                 */
                if (lastVisible >= total - 3) {
                    loadNextPage();
                }
            }
        });
    }

    /*
     * 검색 Repository를 초기화하는 함수임
     */
    private void initRepository() {
        searchRepository = new SearchRepository(requireContext());
    }

    /*
     * 검색창, 메인 탭, 팁 카테고리 버튼 이벤트를 설정하는 함수임
     */
    private void initListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                /*
                 * 글자 입력마다 즉시 요청하지 않고 300ms 동안 추가 입력이 없을 때만 요청함
                 * 이전에 예약된 요청이 있으면 취소하고 새로 예약함
                 */
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                debounceRunnable = () -> resetAndLoad();
                debounceHandler.postDelayed(debounceRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        tabFeed.setOnClickListener(v    -> changeMainTab("FEED"));
        tabTip.setOnClickListener(v     -> changeMainTab("TIP"));
        tabProfile.setOnClickListener(v -> changeMainTab("PROFILE"));
        tabFriend.setOnClickListener(v  -> changeMainTab("FRIEND"));

        btnTipAll.setOnClickListener(v      -> changeTipCategory("ALL"));
        btnTipFree.setOnClickListener(v     -> changeTipCategory("FREE"));
        btnTipTraining.setOnClickListener(v -> changeTipCategory("TRAINING"));
        btnTipCourse.setOnClickListener(v   -> changeTipCategory("COURSE"));
        btnTipGear.setOnClickListener(v     -> changeTipCategory("GEAR"));
    }

    /*
     * 메인 검색 탭을 변경하는 함수임
     */
    private void changeMainTab(String tab) {
        currentTab = tab;

        tabFeed.setTextColor(0xFFFFFFFF);
        tabTip.setTextColor(0xFFFFFFFF);
        tabProfile.setTextColor(0xFFFFFFFF);
        tabFriend.setTextColor(0xFFFFFFFF);

        if (tab.equals("FEED")) {
            tabFeed.setTextColor(0xFFB6FF3B);
            slideIndicatorToTab(0, true);
            layoutTipCategory.setVisibility(View.GONE);

        } else if (tab.equals("TIP")) {
            tabTip.setTextColor(0xFFB6FF3B);
            slideIndicatorToTab(1, true);
            layoutTipCategory.setVisibility(View.VISIBLE);
            applyTipCategoryStyles(currentTipCategory);

        } else if (tab.equals("PROFILE")) {
            tabProfile.setTextColor(0xFFB6FF3B);
            slideIndicatorToTab(2, true);
            layoutTipCategory.setVisibility(View.GONE);

        } else if (tab.equals("FRIEND")) {
            tabFriend.setTextColor(0xFFB6FF3B);
            slideIndicatorToTab(3, true);
            layoutTipCategory.setVisibility(View.GONE);
        }

        resetAndLoad();
    }

    /*
     * 탭 인디케이터를 해당 탭 인덱스 위치로 슬라이드하는 함수임
     */
    private void slideIndicatorToTab(int tabIndex, boolean animate) {
        if (tabIndicator == null || layoutMainTabContainer == null) return;

        int containerWidth = layoutMainTabContainer.getWidth();
        if (containerWidth == 0) {
            tabIndicator.post(() -> slideIndicatorToTab(tabIndex, false));
            return;
        }

        float tabWidth      = containerWidth / 4f;
        float indicatorWidth = dp(32);
        float targetX       = tabIndex * tabWidth + (tabWidth - indicatorWidth) / 2f;

        if (animate) {
            tabIndicator.animate()
                    .translationX(targetX)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            tabIndicator.setTranslationX(targetX);
        }
    }

    /*
     * 팁 카테고리를 변경하는 함수임
     */
    private void changeTipCategory(String category) {
        currentTipCategory = category;
        applyTipCategoryStyles(category);
        resetAndLoad();
    }

    /*
     * 팁 카테고리 버튼 스타일을 전체 갱신하는 함수임
     */
    private void applyTipCategoryStyles(String selectedCategory) {
        setCategoryButtonStyle(btnTipAll,      "ALL".equals(selectedCategory),      "#B6FF3B");
        setCategoryButtonStyle(btnTipFree,     "FREE".equals(selectedCategory),     "#00E5FF");
        setCategoryButtonStyle(btnTipTraining, "TRAINING".equals(selectedCategory), "#FF3DFF");
        setCategoryButtonStyle(btnTipCourse,   "COURSE".equals(selectedCategory),   "#FFB300");
        setCategoryButtonStyle(btnTipGear,     "GEAR".equals(selectedCategory),     "#00FF85");
    }

    /*
     * 팁 카테고리 버튼 1개의 선택/비선택 스타일을 적용하는 함수임
     */
    private void setCategoryButtonStyle(TextView button, boolean isSelected, String selectedColorCode) {
        if (button == null) return;

        int selectedColor   = Color.parseColor(selectedColorCode);
        int unselectedColor = Color.parseColor("#E8E8E8");

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(18));

        if (isSelected) {
            drawable.setColor(selectedColor);
            drawable.setStroke(dp(1), selectedColor);
            button.setTextColor(Color.parseColor("#111111"));
        } else {
            drawable.setColor(unselectedColor);
            drawable.setStroke(dp(1), unselectedColor);
            button.setTextColor(Color.parseColor("#111111"));
        }

        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(drawable);
    }

    // ──────────────────────────────────────────────────
    // 페이지네이션 핵심 로직
    // ──────────────────────────────────────────────────

    /*
     * 페이지 상태를 초기화하고 첫 페이지를 새로 로드하는 함수임
     * 탭 전환, keyword 변경, 카테고리 변경 시 호출함
     */
    private void resetAndLoad() {
        currentPage = 0;
        isLoading   = false;
        hasMore     = true;

        /*
         * 각 탭의 데이터 리스트를 비우고 어댑터 참조도 초기화함
         * 다음 load 시 새 어댑터를 생성해 RecyclerView에 연결함
         */
        feedItemList.clear();
        tipItemList.clear();
        tipLikedMap.clear();
        tipBookmarkedMap.clear();
        tipLikeCountMap.clear();
        userItemList.clear();

        feedAdapter = null;
        tipAdapter  = null;
        userAdapter = null;

        rvSearch.setAdapter(null);

        loadPage();
    }

    /*
     * 스크롤 끝에서 다음 페이지를 요청하는 함수임
     */
    private void loadNextPage() {
        currentPage++;
        loadPage();
    }

    /*
     * 현재 탭과 페이지 번호로 API를 호출하는 함수임
     * isLoading 플래그로 중복 요청을 막음
     */
    private void loadPage() {
        if (isLoading || !hasMore) return;
        isLoading = true;

        String keyword = etSearch.getText() == null ? "" : etSearch.getText().toString();

        switch (currentTab) {
            case "FEED":    loadFeedPage(keyword);    break;
            case "TIP":     loadTipPage(keyword);     break;
            case "PROFILE": loadProfilePage(keyword); break;
            case "FRIEND":  loadFriendPage(keyword);  break;
        }
    }

    /*
     * 피드 페이지를 로드하고 기존 목록에 append하는 함수임
     */
    private void loadFeedPage(String keyword) {
        searchRepository.searchFeeds(keyword, currentPage, PAGE_SIZE, new SearchRepository.FeedSearchCallback() {
            @Override
            public void onSuccess(List<FeedResponse> feedResponses) {
                if (!isAdded()) return;

                List<FeedItem> newItems = convertFeedResponsesToItems(feedResponses);
                int insertStart = feedItemList.size();
                feedItemList.addAll(newItems);

                if (feedAdapter == null) {
                    /*
                     * 첫 페이지: 새 어댑터 생성 후 RecyclerView에 연결함
                     */
                    feedAdapter = new FeedAdapter(feedItemList);
                    rvSearch.setAdapter(feedAdapter);
                } else {
                    feedAdapter.notifyItemRangeInserted(insertStart, newItems.size());
                }

                hasMore   = newItems.size() >= PAGE_SIZE;
                isLoading = false;
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * 팁 페이지를 로드하고 기존 목록에 append하는 함수임
     */
    private void loadTipPage(String keyword) {
        searchRepository.searchTips(keyword, currentTipCategory, currentPage, PAGE_SIZE, new SearchRepository.TipSearchCallback() {
            @Override
            public void onSuccess(List<TipResponse> tipResponses) {
                if (!isAdded()) return;

                ArrayList<TipItem> newItems = convertTipResponsesToItems(tipResponses);
                int insertStart = tipItemList.size();
                tipItemList.addAll(newItems);

                /*
                 * 좋아요/북마크/likeCount 맵에도 새 항목을 추가함
                 */
                for (TipResponse response : tipResponses) {
                    Long tipId = response.getTipId();
                    if (tipId == null) continue;
                    tipLikedMap.put(tipId, response.isLiked());
                    tipBookmarkedMap.put(tipId, response.isBookmarked());
                    tipLikeCountMap.put(tipId, response.getLikeCount());
                }

                if (tipAdapter == null) {
                    tipAdapter = new TipAdapter(tipItemList, tipLikedMap, tipBookmarkedMap, tipLikeCountMap);
                    rvSearch.setAdapter(tipAdapter);
                } else {
                    tipAdapter.notifyItemRangeInserted(insertStart, newItems.size());
                }

                hasMore   = newItems.size() >= PAGE_SIZE;
                isLoading = false;
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * 프로필 페이지를 로드하고 기존 목록에 append하는 함수임
     * keyword 없으면 배지 기준 top 프로필, 있으면 keyword 검색
     */
    private void loadProfilePage(String keyword) {
        SearchRepository.UserSearchCallback callback = new SearchRepository.UserSearchCallback() {
            @Override
            public void onSuccess(List<SearchUserResponse> userResponses) {
                if (!isAdded()) return;

                int insertStart = userItemList.size();
                userItemList.addAll(userResponses);

                if (userAdapter == null) {
                    userAdapter = new SearchUserAdapter(userItemList, "PROFILE");
                    rvSearch.setAdapter(userAdapter);
                } else {
                    userAdapter.notifyItemRangeInserted(insertStart, userResponses.size());
                }

                hasMore   = userResponses.size() >= PAGE_SIZE;
                isLoading = false;
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        };

        if (keyword.isEmpty()) {
            searchRepository.getTopProfiles(currentPage, PAGE_SIZE, callback);
        } else {
            searchRepository.searchProfiles(keyword, currentPage, PAGE_SIZE, callback);
        }
    }

    /*
     * 친구 목록을 로드하는 함수임
     * 친구 탭은 페이지네이션 없이 전체 목록을 한 번에 불러옴
     */
    private void loadFriendPage(String keyword) {
        SearchRepository.UserSearchCallback callback = new SearchRepository.UserSearchCallback() {
            @Override
            public void onSuccess(List<SearchUserResponse> userResponses) {
                if (!isAdded()) return;

                userItemList.clear();
                userItemList.addAll(userResponses);

                userAdapter = new SearchUserAdapter(userItemList, "FRIEND");
                rvSearch.setAdapter(userAdapter);

                /*
                 * 친구 탭은 한 번에 전체 로드하므로 hasMore를 false로 설정함
                 */
                hasMore   = false;
                isLoading = false;
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        };

        if (keyword.isEmpty()) {
            searchRepository.getMyFriends(callback);
        } else {
            searchRepository.searchFriends(keyword, callback);
        }
    }

    // ──────────────────────────────────────────────────
    // 데이터 변환 헬퍼
    // ──────────────────────────────────────────────────

    /*
     * FeedResponse 목록을 FeedItem 목록으로 변환하는 함수임
     */
    private List<FeedItem> convertFeedResponsesToItems(List<FeedResponse> responses) {
        List<FeedItem> items = new ArrayList<>();
        if (responses == null) return items;

        for (FeedResponse response : responses) {
            FeedItem feedItem = new FeedItem(
                    response.getFeedId(),
                    response.getWriterId(),
                    getSafeText(response.getProfileImageUrl(), ""),
                    getSafeText(response.getNickname(), "알 수 없음"),
                    response.isBadgeOwned(),
                    response.getBadgeType(),
                    formatTime(response.getCreatedAt()),
                    getSafeText(response.getTitle(), ""),
                    getSafeText(response.getContent(), ""),
                    response.getTaggedCount(),
                    response.getLikeCount(),
                    response.getCommentCount(),
                    getSafeText(response.getDistance(), "0.00 km"),
                    getSafeText(response.getDuration(), "00:00"),
                    getSafeText(response.getPace(), "-"),
                    response.isMapVisible(),
                    getSafeText(response.getRouteMapImageUri(), ""),
                    response.getImageUrls() == null ? new ArrayList<>() : response.getImageUrls(),
                    response.isLiked(),
                    response.isBookmarked(),
                    response.isCommented(),
                    response.isTagged()
            );
            feedItem.setMine(response.isMine());
            items.add(feedItem);
        }
        return items;
    }

    /*
     * TipResponse 목록을 TipItem 목록으로 변환하는 함수임
     */
    private ArrayList<TipItem> convertTipResponsesToItems(List<TipResponse> responses) {
        ArrayList<TipItem> items = new ArrayList<>();
        if (responses == null) return items;

        for (TipResponse response : responses) {
            TipItem tipItem = new TipItem(
                    response.getTipId(),
                    response.getWriterId(),
                    getSafeText(response.getNickname(), "알 수 없음"),
                    getSafeText(response.getProfileImageUrl(), ""),
                    getSafeText(response.getCategory(), "FREE"),
                    getSafeText(response.getTitle(), ""),
                    getSafeText(response.getContent(), ""),
                    response.isBadgeOwned(),
                    response.getBadgeType(),
                    response.isGpsVisible(),
                    response.getImageUrls() == null ? new ArrayList<>() : response.getImageUrls(),
                    getSafeText(response.getRouteMapImageUrl(), ""),
                    response.getLikeCount(),
                    response.getCommentCount(),
                    formatTime(response.getCreatedAt())
            );
            tipItem.setCommented(response.isCommented());
            tipItem.setMine(response.isMine());
            items.add(tipItem);
        }
        return items;
    }

    // ──────────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /*
         * Fragment가 종료될 때 대기 중인 디바운스 콜백을 제거해 메모리 누수를 방지함
         */
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }

    /*
     * 오류 메시지를 Toast로 표시하고 로딩 플래그를 해제하는 함수임
     */
    private void showError(String message) {
        if (!isAdded()) return;
        isLoading = false;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return value;
    }

    private String formatTime(String isoTime) {
        return TimeFormatter.format(isoTime);
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
