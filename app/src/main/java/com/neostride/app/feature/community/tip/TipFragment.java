package com.neostride.app.feature.community.tip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.repository.BadgeRepository;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.tip.model.TipItem;
import com.neostride.app.feature.community.tip.model.TipResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 팁 목록 화면 Fragment 클래스임
 * 팁 게시글 목록 조회, 카테고리 필터링, 팁 작성 화면 이동을 담당함
 */
public class TipFragment extends Fragment {

    private static final String TAG = "TipFragment";
    private static final int PAGE_SIZE = 5;

    private RecyclerView rvTipList;

    private TextView btnAll;
    private TextView btnFree;
    private TextView btnTraining;
    private TextView btnCourse;
    private TextView btnGear;

    private TipAdapter tipAdapter;

    // 서버에서 가져온 전체 팁 목록을 저장함
    private final ArrayList<TipItem> tipList = new ArrayList<>();

    // 현재 카테고리에서 필터링된 전체 목록 (페이지네이션 소스)
    private final ArrayList<TipItem> allFilteredTips = new ArrayList<>();

    // 선택된 카테고리에 따라 화면에 보여줄 팁 목록을 저장함 (어댑터에 바인딩)
    private final ArrayList<TipItem> filteredTipList = new ArrayList<>();

    // 현재 RecyclerView에 표시된 아이템 수
    private int tipDisplayedCount = 0;

    // 로딩 애니메이션
    private TextView tvLoading;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private Runnable loadingRunnable;
    private int loadingDotCount = 1;

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

        // 로딩 텍스트 뷰 연결
        tvLoading = view.findViewById(R.id.tv_loading);

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

        // 스크롤 끝 근처에서 다음 PAGE_SIZE개 추가 로드
        rvTipList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rvTipList.getLayoutManager();
                if (lm == null) return;
                if (lm.findLastVisibleItemPosition() >= tipDisplayedCount - 2
                        && tipDisplayedCount < allFilteredTips.size()) {
                    loadMoreTips();
                }
            }
        });

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

        // 글쓰기 버튼 클릭 시 배지 등급 확인 후 팁 업로드 화면으로 이동함
        View btnWriteTip = view.findViewById(R.id.btn_write_tip);
        if (btnWriteTip != null) {
            btnWriteTip.setOnClickListener(v -> {
                BadgeService badgeService = ApiClient.getInstance().create(BadgeService.class);
                BadgeRepository badgeRepository = new BadgeRepository(badgeService);
                badgeRepository.fetchBadgeDetail(badgeResponse -> {
                    requireActivity().runOnUiThread(() -> {
                        String tier = badgeResponse != null ? badgeResponse.tier : "none";
                        if (tier == null || tier.equalsIgnoreCase("none")) {
                            Toast.makeText(requireContext(),
                                    "팁 게시판은 브론즈 이상 배지 보유자만 작성할 수 있습니다.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(requireContext(), TipUploadActivity.class);
                            tipUploadLauncher.launch(intent);
                        }
                    });
                });
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
     *
     * [개선된 로직]
     * - API 1(팁 목록)이 도착하는 즉시 첫 PAGE_SIZE개를 화면에 표시함
     * - API 2~3(좋아요·댓글 상태)는 백그라운드에서 완료 후
     *   이미 표시된 아이템의 상태만 업데이트함 (화면이 멈추지 않음)
     * - 스크롤 끝 근처에서 다음 PAGE_SIZE개를 추가로 표시함
     */
    private void loadTipList() {
        // 상태 초기화
        tipList.clear();
        likedStateMap.clear();
        bookmarkedStateMap.clear();
        likeCountMap.clear();
        allFilteredTips.clear();
        filteredTipList.clear();
        tipDisplayedCount = 0;
        if (tipAdapter != null) tipAdapter.notifyDataSetChanged();
        startLoadingAnimation();

        // liked·commented 교차 확인용 (API 2, 3에서 채워짐)
        final Set<Long> likedIds     = new HashSet<>();
        final Set<Long> commentedIds = new HashSet<>();
        final int[] statePending     = {2}; // liked + commented

        // 상태 API 2개가 완료되면 현재 표시된 아이템 상태 갱신
        Runnable onStatesReady = () -> {
            if (!isAdded()) return;
            for (TipItem item : filteredTipList) {
                Long id = item.getTipId();
                if (id != null) {
                    if (likedIds.contains(id))     likedStateMap.put(id, true);
                    if (commentedIds.contains(id)) item.setCommented(true);
                }
            }
            // tipList 전체에도 반영 (이후 스크롤로 추가되는 아이템 대비)
            for (TipItem item : tipList) {
                Long id = item.getTipId();
                if (id != null) {
                    if (likedIds.contains(id))     likedStateMap.put(id, true);
                    if (commentedIds.contains(id)) item.setCommented(true);
                }
            }
            if (tipAdapter != null) tipAdapter.notifyDataSetChanged();
        };

        // API 1: 팁 전체 목록 — 도착 즉시 첫 PAGE_SIZE개 표시
        tipRepository.getTips(new TipRepository.TipListCallback() {
            @Override
            public void onSuccess(List<TipResponse> response) {
                if (!isAdded()) return;
                stopLoadingAnimation();
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
                applyFilter(); // 첫 PAGE_SIZE개 즉시 표시
            }
            @Override
            public void onFailure(String message) {
                stopLoadingAnimation();
                Log.e(TAG, "loadTipList failure = " + message);
                if (isAdded()) Toast.makeText(requireContext(), "팁 목록을 불러오지 못했습니다: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        // API 2: 내가 좋아요한 팁 목록 (isLiked 교차 확인용)
        tipRepository.getLikedTips(new retrofit2.Callback<List<TipResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TipResponse>> call, retrofit2.Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (TipResponse t : response.body())
                        if (t.getTipId() != null) likedIds.add(t.getTipId());
                if (--statePending[0] == 0) onStatesReady.run();
            }
            @Override
            public void onFailure(retrofit2.Call<List<TipResponse>> call, Throwable t) {
                if (--statePending[0] == 0) onStatesReady.run();
            }
        });

        // API 3: 내가 댓글 단 팁 목록 (isCommented 교차 확인용)
        tipRepository.getCommentedTips(new retrofit2.Callback<List<TipResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<TipResponse>> call, retrofit2.Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null)
                    for (TipResponse t : response.body())
                        if (t.getTipId() != null) commentedIds.add(t.getTipId());
                if (--statePending[0] == 0) onStatesReady.run();
            }
            @Override
            public void onFailure(retrofit2.Call<List<TipResponse>> call, Throwable t) {
                if (--statePending[0] == 0) onStatesReady.run();
            }
        });
    }

    /*
     * 스크롤 시 다음 PAGE_SIZE개를 filteredTipList에 추가하는 함수임
     */
    private void loadMoreTips() {
        int start = tipDisplayedCount;
        int end = Math.min(start + PAGE_SIZE, allFilteredTips.size());
        if (start >= end) return;
        for (int i = start; i < end; i++) {
            filteredTipList.add(allFilteredTips.get(i));
        }
        tipDisplayedCount = end;
        if (tipAdapter != null) tipAdapter.notifyItemRangeInserted(start, end - start);
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
     * 선택된 버튼은 카테고리별 네온색으로 채워지고(불 켜짐),
     * 선택되지 않은 버튼은 검은 배경으로 꺼진 상태로 표시함
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
     * 검은 배경에 흰 텍스트로 "꺼진" 상태를 표시함
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
     * 네온 테두리 + 동일 색상 텍스트로 "불 켜진" 효과를 줌
     */
    private void setSelectedCategoryButton(TextView button, String colorCode) {
        if (button == null) {
            return;
        }

        button.setBackground(makeSelectedCategoryBackground(colorCode));
        button.setTextColor(Color.parseColor(colorCode));
        button.setTypeface(null, Typeface.BOLD);
    }

    /*
     * 선택된 카테고리 버튼의 네온 테두리 배경을 만드는 함수임
     * 배경은 어둡고 테두리가 네온 색상으로 빛나는 형태임
     */
    private GradientDrawable makeSelectedCategoryBackground(String colorCode) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#1A1A1A"));
        drawable.setStroke(dp(2), Color.parseColor(colorCode));
        drawable.setCornerRadius(dp(18));

        return drawable;
    }

    /*
     * 선택되지 않은 카테고리 버튼의 검은 배경을 만드는 함수임
     * 불이 꺼진 상태처럼 어두운 pill 모양으로 표시함
     */
    private GradientDrawable makeUnselectedCategoryBackground() {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#1A1A1A"));
        drawable.setStroke(dp(1), Color.parseColor("#333333"));
        drawable.setCornerRadius(dp(18));

        return drawable;
    }

    /*
     * 선택된 카테고리에 맞게 리스트를 필터링하고 첫 PAGE_SIZE개만 표시하는 함수임
     * 카테고리가 바뀌거나 데이터가 새로 로드되면 페이지네이션을 처음부터 다시 시작함
     */
    private void applyFilter() {
        // 필터링된 전체 목록 재구성
        allFilteredTips.clear();
        if (selectedCategory.equals("전체")) {
            allFilteredTips.addAll(tipList);
        } else {
            for (TipItem item : tipList) {
                if (item.getCategory() != null
                        && convertCategoryToKorean(item.getCategory()).equals(selectedCategory)) {
                    allFilteredTips.add(item);
                }
            }
        }

        // 첫 PAGE_SIZE개만 화면에 표시 (카테고리 변경 시 페이지네이션 리셋)
        tipDisplayedCount = Math.min(PAGE_SIZE, allFilteredTips.size());
        filteredTipList.clear();
        filteredTipList.addAll(allFilteredTips.subList(0, tipDisplayedCount));

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