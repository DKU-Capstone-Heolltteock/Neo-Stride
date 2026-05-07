package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.neostride.app.R;

public class TagPeopleDialog {

    public interface OnTagSelectedListener {
        void onTagSelected(int count);
    }

    private final Context context;
    private final OnTagSelectedListener listener;

    public TagPeopleDialog(
            Context context,
            OnTagSelectedListener listener
    ) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {

        Dialog dialog = new Dialog(context);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_tag_people);

        Window window = dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width =
                    (int) (
                            context.getResources()
                                    .getDisplayMetrics()
                                    .widthPixels * 0.84
                    );

            params.height =
                    (int) (
                            context.getResources()
                                    .getDisplayMetrics()
                                    .heightPixels * 0.62
                    );

            params.gravity = Gravity.CENTER;

            window.setAttributes(params);
        }

        TextView btnDone =
                dialog.findViewById(R.id.btn_done_tag);

        btnDone.setOnClickListener(v -> {

            // 임시로 2명 선택 처리
            listener.onTagSelected(2);

            dialog.dismiss();
        });

        dialog.show();
    }
}