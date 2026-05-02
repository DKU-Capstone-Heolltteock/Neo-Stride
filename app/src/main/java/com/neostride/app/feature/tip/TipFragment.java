package com.neostride.app.feature.tip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

public class TipFragment extends Fragment {

    private RecyclerView rvTipList;

    private TextView btnAll;
    private TextView btnTraining;
    private TextView btnCourse;
    private TextView btnGear;

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
        // fragment_tip.xml 레이아웃을 화면에 연결함
        return inflater.inflate(R.layout.fragment_tip, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // 팁 리스트 RecyclerView를 연결함
        rvTipList = view.findViewById(R.id.rv_tip_list);
        rvTipList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 카테고리 버튼 역할을 하는 TextView들을 연결함
        btnAll = view.findViewById(R.id.btn_tip_all);
        btnTraining = view.findViewById(R.id.btn_tip_training);
        btnCourse = view.findViewById(R.id.btn_tip_course);
        btnGear = view.findViewById(R.id.btn_tip_gear);

        // 처음 화면에서는 전체 버튼이 선택되도록 설정함
        selectCategory(btnAll);

        btnAll.setOnClickListener(v -> {
            selectCategory(btnAll);
            Toast.makeText(requireContext(), "전체 팁", Toast.LENGTH_SHORT).show();
        });

        btnTraining.setOnClickListener(v -> {
            selectCategory(btnTraining);
            Toast.makeText(requireContext(), "훈련 팁", Toast.LENGTH_SHORT).show();
        });

        btnCourse.setOnClickListener(v -> {
            selectCategory(btnCourse);
            Toast.makeText(requireContext(), "코스 팁", Toast.LENGTH_SHORT).show();
        });

        btnGear.setOnClickListener(v -> {
            selectCategory(btnGear);
            Toast.makeText(requireContext(), "장비 팁", Toast.LENGTH_SHORT).show();
        });

        View btnWriteTip = view.findViewById(R.id.btn_write_tip);
        if (btnWriteTip != null) {
            btnWriteTip.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "팁 작성 화면으로 이동 예정", Toast.LENGTH_SHORT).show();
            });
        }

        ImageView ivNotification = view.findViewById(R.id.iv_tip_notification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "알림 화면으로 이동", Toast.LENGTH_SHORT).show();
            });
        }

        ImageView ivMyPage = view.findViewById(R.id.iv_tip_mypage);
        if (ivMyPage != null) {
            ivMyPage.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "마이페이지 이동", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // 카테고리 중 하나만 선택되도록 처리함
    private void selectCategory(TextView selectedButton) {
        btnAll.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);
    }
}