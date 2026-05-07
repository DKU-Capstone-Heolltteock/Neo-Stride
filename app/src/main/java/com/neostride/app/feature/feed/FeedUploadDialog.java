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
import com.neostride.app.feature.running.model.RunningRecordResponse;

import java.util.Locale;

public class FeedUploadDialog {

    private final Context context;
    private final RunningRecordResponse recordData;

    public FeedUploadDialog(Context context, RunningRecordResponse recordData) {
        this.context = context;
        this.recordData = recordData;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_feed_upload);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.84);
            params.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.62);
            params.gravity = Gravity.CENTER;
            params.y = -40;

            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.65f);
        }

        TextView btnComplete = dialog.findViewById(R.id.btn_complete_upload);

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private String formatTime(int seconds) {
        return String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60);
    }
}