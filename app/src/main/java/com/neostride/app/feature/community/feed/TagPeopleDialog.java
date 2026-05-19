package com.neostride.app.feature.community.feed;

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
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.community.feed.model.TagUser;
import com.neostride.app.feature.community.feed.repository.FeedRepository;

import java.util.ArrayList;
import java.util.List;

/*
 * 피드 업로드 시 태그할 친구를 선택하는 다이얼로그 클래스임
 * 친구 목록 API를 통해 태그 가능한 사용자 목록을 불러오고,
 * 선택된 사용자를 칩 형태로 표시함
 */
public class TagPeopleDialog {

    public interface OnTagSelectedListener {
        void onTagSelected(List<TagUser> selectedUsers);
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

        // 친구 목록 API를 호출해서 태그 가능한 사용자 목록을 불러옴
        loadFriendUsers();

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
            listener.onTagSelected(new ArrayList<>(selectedUserList));
            dialog.dismiss();
        });
        dialog.show();
    }

    /*
     * 태그 가능한 친구 목록을 서버 또는 Mock 서버에서 불러오는 함수임
     */
    private void loadFriendUsers() {
        FeedRepository feedRepository = new FeedRepository(context);

        feedRepository.getFriendList(new FeedRepository.RepositoryCallback<List<TagUser>>() {
            @Override
            public void onSuccess(List<TagUser> data) {
                originUserList.clear();
                visibleUserList.clear();

                if (data != null) {
                    originUserList.addAll(data);
                    visibleUserList.addAll(data);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                ).show();

                originUserList.clear();
                visibleUserList.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    /*
     * 사용자를 선택하거나 선택 해제하는 함수임
     */
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

    /*
     * 검색어에 따라 사용자 목록을 필터링하는 함수임
     */
    private void filterUsers(String keyword) {
        String lowerKeyword = keyword.toLowerCase().trim();

        visibleUserList.clear();

        for (TagUser user : originUserList) {
            if (user.getNickname() != null
                    && user.getNickname().toLowerCase().contains(lowerKeyword)) {
                visibleUserList.add(user);
            }
        }

        adapter.notifyDataSetChanged();
    }

    /*
     * 선택된 사용자들을 상단 칩 형태로 다시 그리는 함수임
     */
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

    /*
     * 선택된 사용자 칩 배경을 만드는 함수임
     */
    private GradientDrawable createChipBackground() {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dp(1), Color.WHITE);
        drawable.setCornerRadius(dp(12));

        return drawable;
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
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