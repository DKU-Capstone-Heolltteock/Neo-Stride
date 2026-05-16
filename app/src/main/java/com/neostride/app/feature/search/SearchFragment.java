package com.neostride.app.feature.search;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.feed.FeedAdapter;
import com.neostride.app.feature.feed.model.FeedItem;
import com.neostride.app.feature.feed.model.FeedResponse;
import com.neostride.app.feature.search.model.SearchUserResponse;
import com.neostride.app.feature.search.repository.SearchRepository;
import com.neostride.app.feature.tip.TipAdapter;
import com.neostride.app.feature.tip.model.TipItem;
import com.neostride.app.feature.tip.model.TipResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 커뮤니티 검색 화면 Fragment 클래스임
 *
 * 정석 구조로 피드/팁/프로필/친구 검색을 분리함
 * 피드 검색 결과는 기존 FeedAdapter와 item_feed.xml을 사용함
 * 팁 검색 결과는 기존 TipAdapter와 item_tip.xml을 사용함
 * 프로필/친구 검색 결과는 SearchUserAdapter와 item_search.xml을 사용함
 */
public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvSearch;

    private TextView tabFeed;
    private TextView tabTip;
    private TextView tabProfile;
    private TextView tabFriend;

    private View lineFeed;
    private View lineTip;
    private View lineProfile;
    private View lineFriend;
    private View lineFull;

    private LinearLayout layoutTipCategory;

    private TextView btnTipAll;
    private TextView btnTipFree;
    private TextView btnTipTraining;
    private TextView btnTipCourse;
    private TextView btnTipGear;

    private SearchRepository searchRepository;

    /*
     * 현재 선택된 메인 검색 탭임
     * FEED, TIP, PROFILE, FRIEND 중 하나임
     */
    private String currentTab = "FEED";

    /*
     * 현재 선택된 팁 카테고리임
     * TIP 탭에서만 의미 있음
     */
    private String currentTipCategory = "ALL";

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
         * 최초 진입 시 피드 검색 결과를 불러옴
         */
        changeMainTab("FEED");
    }

    /*
     * XML View들을 Java 코드와 연결하는 함수임
     */
    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        rvSearch = view.findViewById(R.id.rv_search);

        tabFeed = view.findViewById(R.id.tab_feed);
        tabTip = view.findViewById(R.id.tab_tip);
        tabProfile = view.findViewById(R.id.tab_profile);
        tabFriend = view.findViewById(R.id.tab_friend);

        lineFeed = view.findViewById(R.id.line_feed);
        lineTip = view.findViewById(R.id.line_tip);
        lineProfile = view.findViewById(R.id.line_profile);
        lineFriend = view.findViewById(R.id.line_friend);
        lineFull = view.findViewById(R.id.line_full);

        layoutTipCategory = view.findViewById(R.id.layout_tip_category);

        btnTipAll = view.findViewById(R.id.btn_tip_all);
        btnTipFree = view.findViewById(R.id.btn_tip_free);
        btnTipTraining = view.findViewById(R.id.btn_tip_training);
        btnTipCourse = view.findViewById(R.id.btn_tip_course);
        btnTipGear = view.findViewById(R.id.btn_tip_gear);
    }

    /*
     * RecyclerView 기본 설정을 초기화하는 함수임
     * 실제 Adapter는 탭별 검색 결과를 받은 뒤 교체함
     */
    private void initRecyclerView() {
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 입력 전 처리 없음
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                requestSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 입력 후 처리 없음
            }
        });

        tabFeed.setOnClickListener(v -> changeMainTab("FEED"));
        tabTip.setOnClickListener(v -> changeMainTab("TIP"));
        tabProfile.setOnClickListener(v -> changeMainTab("PROFILE"));
        tabFriend.setOnClickListener(v -> changeMainTab("FRIEND"));

        btnTipAll.setOnClickListener(v -> changeTipCategory("ALL"));
        btnTipFree.setOnClickListener(v -> changeTipCategory("FREE"));
        btnTipTraining.setOnClickListener(v -> changeTipCategory("TRAINING"));
        btnTipCourse.setOnClickListener(v -> changeTipCategory("COURSE"));
        btnTipGear.setOnClickListener(v -> changeTipCategory("GEAR"));
    }

    /*
     * 메인 검색 탭을 변경하는 함수임
     * 선택된 탭에 따라 UI를 갱신하고 해당 타입 검색을 다시 호출함
     */
    private void changeMainTab(String tab) {
        currentTab = tab;

        tabFeed.setTextColor(0xFFFFFFFF);
        tabTip.setTextColor(0xFFFFFFFF);
        tabProfile.setTextColor(0xFFFFFFFF);
        tabFriend.setTextColor(0xFFFFFFFF);

        lineFeed.setVisibility(View.GONE);
        lineTip.setVisibility(View.GONE);
        lineProfile.setVisibility(View.GONE);
        lineFriend.setVisibility(View.GONE);
        lineFull.setVisibility(View.GONE);

        if (tab.equals("FEED")) {
            tabFeed.setTextColor(0xFFB6FF3B);
            lineFeed.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.GONE);

        } else if (tab.equals("TIP")) {
            tabTip.setTextColor(0xFFB6FF3B);
            lineTip.setVisibility(View.VISIBLE);
            lineFull.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.VISIBLE);
            applyTipCategoryStyles(currentTipCategory);

        } else if (tab.equals("PROFILE")) {
            tabProfile.setTextColor(0xFFB6FF3B);
            lineProfile.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.GONE);

        } else if (tab.equals("FRIEND")) {
            tabFriend.setTextColor(0xFFB6FF3B);
            lineFriend.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.GONE);
        }

        requestSearch();
    }

    /*
     * 팁 카테고리를 변경하는 함수임
     */
    private void changeTipCategory(String category) {
        currentTipCategory = category;
        applyTipCategoryStyles(category);
        requestSearch();
    }

    /*
     * 팁 카테고리 버튼 스타일을 전체 갱신하는 함수임
     * 선택된 버튼만 카테고리별 색상으로 강조함
     */
    private void applyTipCategoryStyles(String selectedCategory) {
        setCategoryButtonStyle(btnTipAll, "ALL".equals(selectedCategory), "#B6FF3B");
        setCategoryButtonStyle(btnTipFree, "FREE".equals(selectedCategory), "#00E5FF");
        setCategoryButtonStyle(btnTipTraining, "TRAINING".equals(selectedCategory), "#FF3DFF");
        setCategoryButtonStyle(btnTipCourse, "COURSE".equals(selectedCategory), "#FFB300");
        setCategoryButtonStyle(btnTipGear, "GEAR".equals(selectedCategory), "#00FF85");
    }

    /*
     * 팁 카테고리 버튼 1개의 선택/비선택 스타일을 적용하는 함수임
     */
    private void setCategoryButtonStyle(TextView button, boolean isSelected, String selectedColorCode) {
        if (button == null) {
            return;
        }

        int selectedColor = Color.parseColor(selectedColorCode);
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

    /*
     * 현재 탭에 따라 알맞은 검색 API를 호출하는 함수임
     */
    private void requestSearch() {
        if (searchRepository == null || etSearch == null) {
            return;
        }

        String keyword = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString();

        if (currentTab.equals("FEED")) {
            requestFeedSearch(keyword);

        } else if (currentTab.equals("TIP")) {
            requestTipSearch(keyword);

        } else if (currentTab.equals("PROFILE")) {
            requestProfileSearch(keyword);

        } else if (currentTab.equals("FRIEND")) {
            requestFriendSearch(keyword);
        }
    }

    /*
     * 피드 검색 API를 호출하고 기존 FeedAdapter로 결과를 표시하는 함수임
     */
    private void requestFeedSearch(String keyword) {
        searchRepository.searchFeeds(keyword, new SearchRepository.FeedSearchCallback() {
            @Override
            public void onSuccess(List<FeedResponse> feedResponses) {
                if (!isAdded()) {
                    return;
                }

                List<FeedItem> feedItems = convertFeedResponsesToItems(feedResponses);
                FeedAdapter feedAdapter = new FeedAdapter(feedItems);
                rvSearch.setAdapter(feedAdapter);
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * 팁 검색 API를 호출하고 기존 TipAdapter로 결과를 표시하는 함수임
     */
    private void requestTipSearch(String keyword) {
        searchRepository.searchTips(keyword, currentTipCategory, new SearchRepository.TipSearchCallback() {
            @Override
            public void onSuccess(List<TipResponse> tipResponses) {
                if (!isAdded()) {
                    return;
                }

                ArrayList<TipItem> tipItems = convertTipResponsesToItems(tipResponses);

                Map<Long, Boolean> likedStateMap = new HashMap<>();
                Map<Long, Boolean> bookmarkedStateMap = new HashMap<>();
                Map<Long, Integer> likeCountMap = new HashMap<>();

                for (TipResponse response : tipResponses) {
                    Long tipId = response.getTipId();

                    if (tipId == null) {
                        continue;
                    }

                    likedStateMap.put(tipId, response.isLiked());
                    bookmarkedStateMap.put(tipId, response.isBookmarked());
                    likeCountMap.put(tipId, response.getLikeCount());
                }

                TipAdapter tipAdapter = new TipAdapter(
                        tipItems,
                        likedStateMap,
                        bookmarkedStateMap,
                        likeCountMap
                );

                rvSearch.setAdapter(tipAdapter);
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * 프로필 검색 API를 호출하고 SearchUserAdapter로 결과를 표시하는 함수임
     */
    private void requestProfileSearch(String keyword) {
        searchRepository.searchProfiles(keyword, new SearchRepository.UserSearchCallback() {
            @Override
            public void onSuccess(List<SearchUserResponse> userResponses) {
                if (!isAdded()) {
                    return;
                }

                SearchUserAdapter adapter = new SearchUserAdapter(userResponses, "PROFILE");
                rvSearch.setAdapter(adapter);
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * 친구 검색 API를 호출하고 SearchUserAdapter로 결과를 표시하는 함수임
     */
    private void requestFriendSearch(String keyword) {
        searchRepository.searchFriends(keyword, new SearchRepository.UserSearchCallback() {
            @Override
            public void onSuccess(List<SearchUserResponse> userResponses) {
                if (!isAdded()) {
                    return;
                }

                SearchUserAdapter adapter = new SearchUserAdapter(userResponses, "FRIEND");
                rvSearch.setAdapter(adapter);
            }

            @Override
            public void onFailure(String message) {
                showError(message);
            }
        });
    }

    /*
     * FeedResponse 목록을 FeedAdapter가 사용하는 FeedItem 목록으로 변환하는 함수임
     */
    private List<FeedItem> convertFeedResponsesToItems(List<FeedResponse> responses) {
        List<FeedItem> items = new ArrayList<>();

        if (responses == null) {
            return items;
        }

        for (FeedResponse response : responses) {
            items.add(new FeedItem(
                    response.getFeedId(),
                    response.getWriterId(),
                    getSafeText(response.getProfileImageUrl(), ""),
                    getSafeText(response.getNickname(), "알 수 없음"),
                    getSafeText(response.getCreatedAt(), "방금 전"),
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
                    response.getImageUrls() == null ? new ArrayList<>() : response.getImageUrls()
            ));
        }

        return items;
    }

    /*
     * TipResponse 목록을 TipAdapter가 사용하는 TipItem 목록으로 변환하는 함수임
     */
    private ArrayList<TipItem> convertTipResponsesToItems(List<TipResponse> responses) {
        ArrayList<TipItem> items = new ArrayList<>();

        if (responses == null) {
            return items;
        }

        for (TipResponse response : responses) {
            items.add(new TipItem(
                    response.getTipId(),
                    response.getWriterId(),
                    getSafeText(response.getNickname(), "알 수 없음"),
                    getSafeText(response.getProfileImageUrl(), ""),
                    getSafeText(response.getCategory(), "FREE"),
                    getSafeText(response.getTitle(), ""),
                    getSafeText(response.getContent(), ""),
                    response.isBadgeOwned(),
                    response.isGpsVisible(),
                    response.getImageUrls() == null ? new ArrayList<>() : response.getImageUrls(),
                    getSafeText(response.getRouteMapImageUrl(), ""),
                    response.getLikeCount(),
                    response.getCommentCount(),
                    getSafeText(response.getCreatedAt(), "방금 전")
            ));
        }

        return items;
    }

    /*
     * 오류 메시지를 Toast로 표시하고 RecyclerView를 비우는 함수임
     */
    private void showError(String message) {
        if (!isAdded()) {
            return;
        }

        rvSearch.setAdapter(null);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /*
     * null 또는 빈 문자열일 때 기본값을 반환하는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}