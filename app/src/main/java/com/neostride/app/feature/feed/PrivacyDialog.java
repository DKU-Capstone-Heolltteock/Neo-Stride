package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.neostride.app.R;

public class PrivacyDialog extends Dialog {

    public interface OnPrivacySelectedListener {
        void onSelected(String privacy);
    }

    private final OnPrivacySelectedListener listener;
    private final String currentPrivacy;

    public PrivacyDialog(
            @NonNull Context context,
            String currentPrivacy,
            OnPrivacySelectedListener listener
    ) {
        super(context);
        this.listener = listener;
        this.currentPrivacy = currentPrivacy;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_privacy);

        RadioGroup radioGroup = findViewById(R.id.radio_group_privacy);

        RadioButton radioPrivate = findViewById(R.id.radio_private);
        RadioButton radioFriend = findViewById(R.id.radio_friend);
        RadioButton radioBadge = findViewById(R.id.radio_badge);
        RadioButton radioPublic = findViewById(R.id.radio_public);

        TextView btnDone = findViewById(R.id.btn_done);

        // 현재 선택 반영
        switch (currentPrivacy) {
            case "나만 보기":
                radioPrivate.setChecked(true);
                break;

            case "친구":
                radioFriend.setChecked(true);
                break;

            case "배지홀더":
                radioBadge.setChecked(true);
                break;

            case "전체":
                radioPublic.setChecked(true);
                break;
        }

        btnDone.setOnClickListener(v -> {

            String selected = "친구";

            int checkedId = radioGroup.getCheckedRadioButtonId();

            if (checkedId == R.id.radio_private) {
                selected = "나만 보기";
            }
            else if (checkedId == R.id.radio_friend) {
                selected = "친구";
            }
            else if (checkedId == R.id.radio_badge) {
                selected = "배지홀더";
            }
            else if (checkedId == R.id.radio_public) {
                selected = "전체";
            }

            listener.onSelected(selected);

            dismiss();
        });
    }
}