package com.neostride.app.feature.community.tip;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.main.record.RecordFragment;

/*
 * 팁 업로드에서 GPS 기록을 선택하기 위한 전용 Activity임
 */
public class TipRecordSelectActivity extends AppCompatActivity {

    public static final String EXTRA_TIP_MODE = "tip_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_record_select);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.tip_record_container,
                            RecordFragment.newInstance(true)
                    )
                    .commit();
        }
    }
}