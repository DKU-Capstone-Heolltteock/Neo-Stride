package com.neostride.app.feature.community.tip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.tip.model.TipItem;
import com.neostride.app.feature.community.tip.model.TipResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 팁 목록 화면 Fragment 클래스임
 * 팁 게시글 목록 조회, 카테고리 필터링, 팁 작성 화면 이동을 담당함
 */
public class TipFragment extends Fragment {

    private static final String TAG = "TipFragment";

    private RecyclerView rvTipList;

    private TextView btnAll;
    private TextView btnFree;
    private TextView btnTraining;
    private TextView btnCourse;
    private TextView btnGear;

    private TipAdapter tipAdapter;

    // 서버에서 가져온 전체 팁 목록을 저장함
    private final ArrayList<TipItem> tipList = new ArrayList<>();

    // 선택된 카테고리에 따라 화면에 보여줄 팁 목록을 저장함
    private final ArrayList<TipItem> filteredTipList = new ArrayList<>();

    /*
     * tipId별 좋아요 상태를 저장하는 Map임
     * TipResponse의 liked 값을 저장하고, Adapter에서 API 응답에 따라 갱신함
     */
    private final Map<Long, Boolean> likedStateMap = new HashMap<>();

    /*
     * tipId별 북마크 상태를 저장하는 Map임
     * TipResponse의 bookmarked 값을 저장하고, Adapter에서 API 응답에 따라 갱신함
     */
    private final Map<Long, Boolean> bookmarkedStateMap = new HashMap<>();

    /*
     * tipId별 좋아요 개수를 저장하는 Map임
     * TipResponse의 likeCount 값을 저장하고, Adapter에서 API 응답에 따라 갱신함
     */
    private final Map<Long, Integer> likeCountMap = new HashMap<>();

    private String selectedCategory = "전체";

    private ActivityResultLauncher<Intent> tipUploadLauncher;

    private TipRepository tipRepository;

    public TipFragment() {
        // Fragment 기본 생성자가 필요함
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_tip, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Repository를 초기화함
        tipRepository = new TipRepository();

        // 업로드 결과를 받기 위한 런처를 초기화함
        initUploadLauncher();

        // RecyclerView를 초기화함
        rvTipList = view.findViewById(R.id.rv_tip_list);
        rvTipList.setLayoutManager(new LinearLayoutManager(requireContext()));

        tipAdapter = new TipAdapter(
                filteredTipList,
                likedStateMap,
                bookmarkedStateMap,
                likeCountMap
        );
        rvTipList.setAdapter(tipAdapter);

        // 카테고리 버튼을 연결함
        btnAll = view.findViewById(R.id.btn_tip_all);
        btnFree = view.findViewById(R.id.btn_tip_free);
        btnTraining = view.findViewById(R.id.btn_tip_training);
        btnCourse = view.findViewById(R.id.btn_tip_course);
        btnGear = view.findViewById(R.id.btn_tip_gear);

        // 카테고리 클릭 이벤트를 설정함
        btnAll.setOnClickListener(v -> selectCategory(btnAll, "전체"));
        btnFree.setOnClickListener(v -> selectCategory(btnFree, "자유"));
        btnTraining.setOnClickListener(v -> selectCategory(btnTraining, "훈련"));
        btnCourse.setOnClickListener(v -> selectCategory(btnCourse, "코스"));
        btnGear.setOnClickListener(v -> selectCategory(btnGear, "장비"));

        // 글쓰기 버튼 클릭 시 팁 업로드 화면으로 이동함
        View btnWriteTip = view.findViewById(R.id.btn_write_tip);
        if (btnWriteTip != null) {
            btnWriteTip.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TipUploadActivity.class);
                tipUploadLauncher.launch(intent);
            });
        }

        // "마이페이지 배지 확인" 버튼 → 배지 화면으로 직접 이동
        View btnGoMy = view.findViewById(R.id.btn_go_my);
        if (btnGoMy != null) {
            btnGoMy.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(),
                            com.neostride.app.feature.badge.BadgeActivity.class)));
        }

        // 기본 카테고리를 전체로 선택함
        selectCategory(btnAll, "전체");

        // 서버 또는 목서버에서 팁 목록을 조회함
        loadTipList();
    }

    /*
     * 상세 화면에서 좋아요/북마크를 변경하고 돌아왔을 때
     * 목록도 목서버의 최신 상태를 다시 반영하도록 갱신함
     */
    @Override
    public void onResume() {
        super.onResume();

        if (tipRepository != null && tipAdapter != null) {
            loadTipList();
        }
    }

    /*
     * 팁 업로드 결과를 받기 위한 런처 초기화 함수임
     * 업로드 성공 후 돌아오면 서버 목록을 다시 조회함
     */
    private void initUploadLauncher() {
        tipUploadLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(requireContext(), "팁이 등록되었습니다", Toast.LENGTH_SHORT).show();

                        // 업로드 후 최신 목록을 서버에서 다시 조회함
                        loadTipList();
                    }
                }
        );
    }

    /*
     * 서버에서 팁 목록을 조회하는 함수임
     * 피드 목록과 동일하게 List<TipResponse> 형태로 응답을 받음
     */
    private void loadTipList() {
        tipRepository.getTips(new TipRepository.TipListCallback() {
            @Override
            public void onSuccess(List<TipResponse> response) {
                Log.d(TAG, "loadTipList success");

                tipList.clear();

                /*
                 * 서버/목서버에서 다시 목록을 불러오므로
                 * 상태 Map도 서버 응답 기준으로 다시 정리함
                 */
                likedStateMap.clear();
                bookmarkedStateMap.clear();
                likeCountMap.clear();

                if (response != null) {
                    for (TipResponse serverTip : response) {
                        TipItem item = convertToTipItem(serverTip);
                        tipList.add(item);

                        Long tipId = serverTip.getTipId();

                        if (tipId != null) {
                            likedStateMap.put(tipId, serverTip.isLiked());
                            bookmarkedStateMap.put(tipId, serverTip.isBookmarked());
                            likeCountMap.put(tipId, serverTip.getLikeCount());
                        }
                    }
                }

                applyFilter();
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "loadTipList failure = " + message);

                Toast.makeText(
                        requireContext(),
                        "팁 목록을 불러오지 못했습니다: " + message,
                        Toast.LENGTH_SHORT
                ).show();

                tipList.clear();
                likedStateMap.clear();
                bookmarkedStateMap.clear();
                likeCountMap.clear();

                applyFilter();
            }
        });
    }

    /*
     * 서버 응답 DTO를 화면 표시용 TipItem으로 변환하는 함수임
     */
    private TipItem convertToTipItem(TipResponse serverTip) {
        TipItem item = new TipItem(
                serverTip.getTipId(),
                serverTip.getWriterId(),
                serverTip.getNickname(),
                serverTip.getProfileImageUrl(),
                serverTip.getCategory(),
                serverTip.getTitle(),
                serverTip.getContent(),
                serverTip.isBadgeOwned(),
                serverTip.getBadgeType(),
                serverTip.isGpsVisible(),
                serverTip.getImageUrls(),
                serverTip.getRouteMapImageUrl(),
                serverTip.getLikeCount(),
                serverTip.getCommentCount(),
                formatTime(serverTip.getCreatedAt())
        );
        item.setCommented(serverTip.isCommented());
        item.setMine(serverTip.isMine());
        return item;
    }

    /*
     * ISO 시간 문자열을 화면 표시용으로 변환 (오늘 내: 상대 시간, 이전: 절대 날짜)
     */
    private String formatTime(String isoTime) {
        return TimeFormatter.format(isoTime);
    }

    /*
     * 카테고리 선택 처리 함수임
     * 선택된 카테고리는 각자 다른 네온색 배경으로 표시함
     */
    private void selectCategory(TextView selectedButton, String category) {
        btnAll.setSelected(false);
        btnFree.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);

        selectedCategory = category;

        updateCategoryButtonStyle(category);

        applyFilter();
    }

    /*
     * 카테고리 버튼 전체 스타일을 갱신하는 함수임
     * 선택된 버튼은 카테고리별 네온색으로 표시하고,
     * 선택되지 않은 버튼은 어두운 배경으로 표시함
     */
    private void updateCategoryButtonStyle(String category) {
        setUnselectedCategoryButton(btnAll);
        setUnselectedCategoryButton(btnFree);
        setUnselectedCategoryButton(btnTraining);
        setUnselectedCategoryButton(btnCourse);
        setUnselectedCategoryButton(btnGear);

        switch (category) {
            case "전체":
                setSelectedCategoryButton(btnAll, "#CCFF00");
                break;

            case "자유":
                setSelectedCategoryButton(btnFree, "#00E5FF");
                break;

            case "훈련":
                setSelectedCategoryButton(btnTraining, "#FF3DFF");
                break;

            case "코스":
                setSelectedCategoryButton(btnCourse, "#FFB300");
                break;

            case "장비":
                setSelectedCategoryButton(btnGear, "#00FF85");
                break;
        }
    }

    /*
     * 선택되지 않은 카테고리 버튼 스타일을 적용하는 함수임
     */
    private void setUnselectedCategoryButton(TextView button) {
        if (button == null) {
            return;
        }

        button.setBackground(makeUnselectedCategoryBackground());
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
    }

    /*
     * 선택된 카테고리 버튼 스타일을 적용하는 함수임
     */
    private void setSelectedCategoryButton(TextView button, String colorCode) {
        if (button == null) {
            return;
        }

        button.setBackground(makeSelectedCategoryBackground(colorCode));
        button.setTextColor(Color.BLACK);
        button.setTypeface(null, Typeface.BOLD);
    }

    /*
     * 선택된 카테고리 버튼의 둥근 네온 배경을 만드는 함수임
     */
    private GradientDrawable makeSelectedCategoryBackground(String colorCode) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(colorCode));
        drawable.setCornerRadius(dp(18));

        return drawable;
    }

    /*
     * 선택되지 않은 카테고리 버튼의 둥근 어두운 배경을 만드는 함수임
     */
    private GradientDrawable makeUnselectedCategoryBackground() {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#2A2A2A"));
        drawable.setStroke(dp(1), Color.parseColor("#555555"));
        drawable.setCornerRadius(dp(18));

        return drawable;
    }

    /*
     * 선택된 카테고리에 맞게 리스트를 필터링하는 함수임
     */
    private void applyFilter() {
        filteredTipList.clear();

        if (selectedCategory.equals("전체")) {
            filteredTipList.addAll(tipList);
        } else {
            for (TipItem item : tipList) {
                if (item.getCategory() != null
                        && convertCategoryToKorean(item.getCategory()).equals(selectedCategory)) {
                    filteredTipList.add(item);
                }
            }
        }

        if (tipAdapter != null) {
            tipAdapter.notifyDataSetChanged();
        }
    }

    /*
     * 서버 카테고리 값을 화면의 한글 카테고리 값으로 변환하는 함수임
     * 서버에서 이미 한글로 오면 그대로 반환함
     */
    private String convertCategoryToKorean(String category) {
        if (category == null) {
            return "자유";
        }

        switch (category) {
            case "FREE":
                return "자유";

            case "TRAINING":
                return "훈련";

            case "COURSE":
                return "코스";

            case "GEAR":
                return "장비";

            default:
                return category;
        }
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}