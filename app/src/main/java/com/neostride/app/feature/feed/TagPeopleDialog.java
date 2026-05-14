package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.feed.model.TagUser;

import java.util.ArrayList;
import java.util.List;

public class TagPeopleDialog {

    public interface OnTagSelectedListener {
        void onTagSelected(int count);
    }

    private final Context context;
    private final OnTagSelectedListener listener;

    private final List<TagUser> originUserList = new ArrayList<>();
    private final List<TagUser> visibleUserList = new ArrayList<>();
    private final List<TagUser> selectedUserList = new ArrayList<>();

    private TagPeopleAdapter adapter;
    private LinearLayout layoutSelectedTags;

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
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();

            params.width = (int) (
                    context.getResources()
                            .getDisplayMetrics()
                            .widthPixels * 0.84
            );

            params.height = (int) (
                    context.getResources()
                            .getDisplayMetrics()
                            .heightPixels * 0.62
            );

            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        ImageView btnBack = dialog.findViewById(R.id.btn_back_tag);
        TextView btnDone = dialog.findViewById(R.id.btn_done_tag);
        EditText etSearchPeople = dialog.findViewById(R.id.et_search_people);
        RecyclerView rvTagPeople = dialog.findViewById(R.id.rv_tag_people);

        layoutSelectedTags = dialog.findViewById(R.id.layout_selected_tags);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        rvTagPeople.setLayoutManager(new LinearLayoutManager(context));

        adapter = new TagPeopleAdapter(
                visibleUserList,
                selectedUserList,
                user -> {
                    toggleUser(user);
                    adapter.notifyDataSetChanged();
                    renderSelectedTags();
                }
        );

        rvTagPeople.setAdapter(adapter);

        /*
         * TODO:
         * 서버 친구 목록 API가 완성되면 이 부분을 서버 응답으로 교체하면 됨
         * 지금은 화면 동작 확인용 임시 데이터임
         */
        loadTempUsers();

        etSearchPeople.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
                // 입력 전 처리는 하지 않음
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 입력 후 처리는 하지 않음
            }
        });

        btnDone.setOnClickListener(v -> {
            listener.onTagSelected(selectedUserList.size());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadTempUsers() {
        /*
         * 서버 API 연결 전까지 태그 가능한 사용자 목록을 빈 화면으로 둠
         *
         * TODO:
         * GET /api/users/taggable?keyword=
         * 서버 API 완성 후 응답 데이터를 originUserList, visibleUserList에 추가하면 됨
         */
        originUserList.clear();
        visibleUserList.clear();

        adapter.notifyDataSetChanged();
    }

    private void toggleUser(TagUser user) {
        TagUser selectedTarget = null;

        for (TagUser selectedUser : selectedUserList) {
            if (selectedUser.getUserId().equals(user.getUserId())) {
                selectedTarget = selectedUser;
                break;
            }
        }

        if (selectedTarget == null) {
            selectedUserList.add(user);
        } else {
            selectedUserList.remove(selectedTarget);
        }
    }

    private void filterUsers(String keyword) {
        String lowerKeyword = keyword.toLowerCase().trim();

        visibleUserList.clear();

        for (TagUser user : originUserList) {
            if (user.getNickname().toLowerCase().contains(lowerKeyword)) {
                visibleUserList.add(user);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void renderSelectedTags() {
        layoutSelectedTags.removeAllViews();

        for (TagUser user : selectedUserList) {
            TextView chip = new TextView(context);

            chip.setText(user.getNickname() + "  ×");
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(13);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(5), dp(10), dp(5));
            chip.setBackground(createChipBackground());

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            dp(34)
                    );

            params.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                selectedUserList.remove(user);
                adapter.notifyDataSetChanged();
                renderSelectedTags();
            });

            layoutSelectedTags.addView(chip);
        }
    }

    private GradientDrawable createChipBackground() {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dp(1), Color.WHITE);
        drawable.setCornerRadius(dp(12));

        return drawable;
    }

    private int dp(int value) {
        return (int) (
                value *
                        context.getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }
}