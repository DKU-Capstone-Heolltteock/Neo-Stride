package com.neostride.app.feature.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.search.model.SearchItem;

import java.util.ArrayList;
import java.util.List;

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

    private SearchAdapter adapter;

    private final List<SearchItem> originalList = new ArrayList<>();
    private final List<SearchItem> filteredList = new ArrayList<>();

    private String currentTab = "FEED";
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

        adapter = new SearchAdapter(filteredList);

        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setAdapter(adapter);

        loadDummyData();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 입력 전 처리 없음
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
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

        changeMainTab("FEED");
    }

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

        } else if (tab.equals("PROFILE")) {
            tabProfile.setTextColor(0xFFB6FF3B);
            lineProfile.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.GONE);

        } else if (tab.equals("FRIEND")) {
            tabFriend.setTextColor(0xFFB6FF3B);
            lineFriend.setVisibility(View.VISIBLE);
            layoutTipCategory.setVisibility(View.GONE);
        }

        filterList(etSearch.getText().toString());
    }

    private void changeTipCategory(String category) {
        currentTipCategory = category;

        resetTipCategoryStyle();

        if (category.equals("ALL")) {
            setSelectedCategory(btnTipAll);
        } else if (category.equals("FREE")) {
            setSelectedCategory(btnTipFree);
        } else if (category.equals("TRAINING")) {
            setSelectedCategory(btnTipTraining);
        } else if (category.equals("COURSE")) {
            setSelectedCategory(btnTipCourse);
        } else if (category.equals("GEAR")) {
            setSelectedCategory(btnTipGear);
        }

        filterList(etSearch.getText().toString());
    }

    private void resetTipCategoryStyle() {
        btnTipAll.setBackgroundResource(R.drawable.bg_tip_category_unselected);
        btnTipFree.setBackgroundResource(R.drawable.bg_tip_category_unselected);
        btnTipTraining.setBackgroundResource(R.drawable.bg_tip_category_unselected);
        btnTipCourse.setBackgroundResource(R.drawable.bg_tip_category_unselected);
        btnTipGear.setBackgroundResource(R.drawable.bg_tip_category_unselected);
    }

    private void setSelectedCategory(TextView selectedButton) {
        selectedButton.setBackgroundResource(R.drawable.bg_tip_category_selected);
    }

    private void filterList(String keyword) {
        filteredList.clear();

        String lowerKeyword = keyword.toLowerCase();

        for (SearchItem item : originalList) {
            boolean matchTab = item.getType().equals(currentTab);
            boolean matchKeyword = item.getTitle().toLowerCase().contains(lowerKeyword);

            boolean matchTipCategory = true;

            if (currentTab.equals("TIP") && !currentTipCategory.equals("ALL")) {
                matchTipCategory = item.getCategory().equals(currentTipCategory);
            }

            if (matchTab && matchKeyword && matchTipCategory) {
                filteredList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void loadDummyData() {
        originalList.clear();

        originalList.add(new SearchItem("JinzaYoungjae3218", "오운완 피드", "FEED", "NONE"));
        originalList.add(new SearchItem("GosuRunner444", "훈련, 인터벌로 해보세요!", "TIP", "TRAINING"));
        originalList.add(new SearchItem("onlyRunning1234", "단대 근처 러닝 장소 추천해요", "TIP", "COURSE"));
        originalList.add(new SearchItem("자유롭게 러닝 얘기해요", "자유 게시글", "TIP", "FREE"));
        originalList.add(new SearchItem("러닝화 추천", "장비 게시글", "TIP", "GEAR"));
        originalList.add(new SearchItem("RunningLover", "친구 999+", "PROFILE", "NONE"));
        originalList.add(new SearchItem("walkingphobia", "친구 999+", "PROFILE", "NONE"));
        originalList.add(new SearchItem("CrazyRun", "친구 999+", "PROFILE", "NONE"));
        originalList.add(new SearchItem("UngCheon1004", "친구 5", "FRIEND", "NONE"));
        originalList.add(new SearchItem("YoonHyeon7942", "친구 7", "FRIEND", "NONE"));

        filterList("");
    }
}