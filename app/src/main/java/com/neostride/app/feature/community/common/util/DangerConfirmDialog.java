package com.neostride.app.feature.community.common.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neostride.app.R;

public final class DangerConfirmDialog {

    private DangerConfirmDialog() { /* no instances */ }

    public static void show(
            Context context,
            String title,
            String message,
            String confirmText,
            Runnable onConfirm
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(context);
        tvMsg.setText(message);
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(14);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(context, 12);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        View divider = new View(context);
        divider.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
        divP.topMargin = dp(context, 20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(context, 16);
        btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = new TextView(context);
        btnConfirm.setText(confirmText);
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(context, 20), dp(context, 10), dp(context, 20), dp(context, 10));
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(context, 20));
        confirmBg.setColor(0xFFFF3B30);
        btnConfirm.setBackground(confirmBg);
        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmP.setMarginStart(dp(context, 8));
        btnConfirm.setLayoutParams(confirmP);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        btnRow.addView(btnConfirm);

        root.addView(btnRow);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}