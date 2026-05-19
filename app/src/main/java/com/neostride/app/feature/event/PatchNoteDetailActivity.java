package com.neostride.app.feature.event;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

import java.util.List;

public class PatchNoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VERSION = "extra_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patch_note_detail);

        String targetVersion = getIntent().getStringExtra(EXTRA_VERSION);

        // 버전으로 해당 패치노트 찾기
        PatchNote note = null;
        for (PatchNote n : PatchNote.getAll()) {
            if (n.version.equals(targetVersion)) {
                note = n;
                break;
            }
        }

        if (note == null) {
            finish();
            return;
        }

        // 뒤로가기
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // 헤더 버전
        TextView tvHeaderVersion = findViewById(R.id.tv_header_version);
        tvHeaderVersion.setText(note.version);

        // 본문 버전 / 날짜 / 요약
        TextView tvVersion = findViewById(R.id.tv_detail_version);
        tvVersion.setText(note.version);

        TextView tvDate = findViewById(R.id.tv_detail_date);
        tvDate.setText(note.date);

        TextView tvSummary = findViewById(R.id.tv_detail_summary);
        tvSummary.setText(note.summary);

        // 신규 섹션
        bindSection(
                findViewById(R.id.section_new),
                findViewById(R.id.layout_new_items),
                note.newFeatures
        );

        // 개선 섹션
        bindSection(
                findViewById(R.id.section_improve),
                findViewById(R.id.layout_improve_items),
                note.improvements
        );

        // 수정 섹션
        bindSection(
                findViewById(R.id.section_fix),
                findViewById(R.id.layout_fix_items),
                note.bugFixes
        );
    }

    /*
     * 섹션에 항목이 있으면 VISIBLE로 전환하고 bullet 항목을 동적으로 추가함
     */
    private void bindSection(LinearLayout section, LinearLayout itemContainer, List<String> items) {
        if (items == null || items.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);
        itemContainer.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int bottomMargin = (int) (8 * density);

        for (String text : items) {
            TextView tv = new TextView(this);
            tv.setText("•  " + text);
            tv.setTextColor(0xFFCCCCCC);
            tv.setTextSize(13);
            tv.setLineSpacing(0, 1.4f);
            tv.setTypeface(null, Typeface.NORMAL);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = bottomMargin;
            tv.setLayoutParams(lp);

            itemContainer.addView(tv);
        }
    }
}
