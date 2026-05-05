package com.neostride.app.feature.tip;

import android.content.Intent;
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
    private TextView btnFree;
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
        return inflater.inflate(R.layout.fragment_tip, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        rvTipList = view.findViewById(R.id.rv_tip_list);
        rvTipList.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnAll = view.findViewById(R.id.btn_tip_all);
        btnFree = view.findViewById(R.id.btn_tip_free);
        btnTraining = view.findViewById(R.id.btn_tip_training);
        btnCourse = view.findViewById(R.id.btn_tip_course);
        btnGear = view.findViewById(R.id.btn_tip_gear);

        selectCategory(btnAll);

        btnAll.setOnClickListener(v -> {
            selectCategory(btnAll);
            Toast.makeText(requireContext(), "전체 팁", Toast.LENGTH_SHORT).show();
        });

        btnFree.setOnClickListener(v -> {
            selectCategory(btnFree);
            Toast.makeText(requireContext(), "자유 팁", Toast.LENGTH_SHORT).show();
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
                Intent intent = new Intent(requireContext(), TipUploadActivity.class);
                startActivity(intent);
            });
        }
    }

    private void selectCategory(TextView selectedButton) {
        btnAll.setSelected(false);
        btnFree.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);
    }
}