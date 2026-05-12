package com.neostride.app.feature.tip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.neostride.app.feature.tip.model.TipItem;

import java.util.ArrayList;

public class TipFragment extends Fragment {

    private RecyclerView rvTipList;

    private TextView btnAll;
    private TextView btnFree;
    private TextView btnTraining;
    private TextView btnCourse;
    private TextView btnGear;

    private TipAdapter tipAdapter;
    private final ArrayList<TipItem> tipList = new ArrayList<>();
    private final ArrayList<TipItem> filteredTipList = new ArrayList<>();

    private String selectedCategory = "전체";

    private ActivityResultLauncher<Intent> tipUploadLauncher;

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

        initUploadLauncher();

        rvTipList = view.findViewById(R.id.rv_tip_list);
        rvTipList.setLayoutManager(new LinearLayoutManager(requireContext()));

        tipAdapter = new TipAdapter(filteredTipList);
        rvTipList.setAdapter(tipAdapter);

        btnAll = view.findViewById(R.id.btn_tip_all);
        btnFree = view.findViewById(R.id.btn_tip_free);
        btnTraining = view.findViewById(R.id.btn_tip_training);
        btnCourse = view.findViewById(R.id.btn_tip_course);
        btnGear = view.findViewById(R.id.btn_tip_gear);

        addDummyTips();

        selectCategory(btnAll, "전체");

        btnAll.setOnClickListener(v -> selectCategory(btnAll, "전체"));
        btnFree.setOnClickListener(v -> selectCategory(btnFree, "자유"));
        btnTraining.setOnClickListener(v -> selectCategory(btnTraining, "훈련"));
        btnCourse.setOnClickListener(v -> selectCategory(btnCourse, "코스"));
        btnGear.setOnClickListener(v -> selectCategory(btnGear, "장비"));

        View btnWriteTip = view.findViewById(R.id.btn_write_tip);
        if (btnWriteTip != null) {
            btnWriteTip.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TipUploadActivity.class);
                tipUploadLauncher.launch(intent);
            });
        }
    }

    /*
     * 팁 업로드 결과를 받기 위한 런처 초기화 함수임
     */
    private void initUploadLauncher() {
        tipUploadLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    String title = data.getStringExtra("title");
                    String content = data.getStringExtra("content");
                    String category = data.getStringExtra("category");
                    boolean gpsVisible = data.getBooleanExtra("gpsVisible", false);

                    ArrayList<Uri> imageUris =
                            data.getParcelableArrayListExtra("imageUris");

                    TipItem newTip = new TipItem(
                            "zinza",
                            category,
                            title,
                            content,
                            true,
                            gpsVisible,
                            imageUris,
                            0,
                            0
                    );

                    tipList.add(0, newTip);

                    applyFilter();

                    Toast.makeText(requireContext(), "팁이 등록되었습니다", Toast.LENGTH_SHORT).show();
                }
        );
    }

    /*
     * 임시 팁 데이터를 추가하는 함수임
     */
    private void addDummyTips() {
        tipList.add(new TipItem(
                "zinza",
                "훈련",
                "초보 러너는 처음부터 빠르게 뛰면 안 됩니다",
                "처음에는 속도보다 꾸준함이 중요합니다. 5분 뛰고 2분 걷는 방식으로 시작하면 부상 위험을 줄일 수 있습니다.",
                true,
                false,
                new ArrayList<>(),
                12,
                3
        ));

        tipList.add(new TipItem(
                "zinza",
                "코스",
                "야간 러닝 코스 추천",
                "가로등이 많고 사람이 적당히 있는 코스를 선택하는 것이 좋습니다. 너무 어두운 길은 피하는 것이 안전합니다.",
                true,
                true,
                new ArrayList<>(),
                8,
                1
        ));
    }

    /*
     * 카테고리 선택 처리 함수임
     */
    private void selectCategory(TextView selectedButton, String category) {
        btnAll.setSelected(false);
        btnFree.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);

        selectedCategory = category;

        applyFilter();
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
                if (item.getCategory().equals(selectedCategory)) {
                    filteredTipList.add(item);
                }
            }
        }

        tipAdapter.notifyDataSetChanged();
    }
}