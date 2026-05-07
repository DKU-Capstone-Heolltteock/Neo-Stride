package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neostride.app.R;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import java.util.ArrayList;
import java.util.List;

public class FeedUploadDialog {

    private final Context context;
    private final RunningRecordResponse recordData;
    private final Runnable openPhotoPicker;

    private Dialog dialog;
    private LinearLayout layoutSelectedPhotos;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();

    public FeedUploadDialog(
            Context context,
            RunningRecordResponse recordData,
            Runnable openPhotoPicker
    ) {
        this.context = context;
        this.recordData = recordData;
        this.openPhotoPicker = openPhotoPicker;
    }

    public void show() {

        // 다이얼로그 생성
        dialog = new Dialog(context);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // dialog_feed_upload.xml 연결
        dialog.setContentView(R.layout.dialog_feed_upload);



        Window window = dialog.getWindow();

        if (window != null) {

            // 배경 투명 처리
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();

            // 다이얼로그 크기 설정
            params.width =
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.84);

            params.height =
                    (int) (context.getResources().getDisplayMetrics().heightPixels * 0.62);

            // 화면 중앙 표시
            params.gravity = Gravity.CENTER;

            // 살짝 위로 올림
            params.y = -40;

            window.setAttributes(params);

            // 뒤 배경 어둡게
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            window.setDimAmount(0.65f);
        }

        // XML 연결
        ImageView btnAddPhoto =
                dialog.findViewById(R.id.btn_add_photo);

        TextView btnComplete =
                dialog.findViewById(R.id.btn_complete_upload);

        layoutSelectedPhotos =
                dialog.findViewById(R.id.layout_selected_photos);

        LinearLayout layoutTagPeople =
                dialog.findViewById(R.id.layout_tag_people);

        TextView tvTagCount =
                dialog.findViewById(R.id.tv_tag_count);

        // 사진 추가 버튼
        btnAddPhoto.setOnClickListener(v -> {

            if (selectedImageUris.size() >= 3) {

                Toast.makeText(
                        context,
                        "사진은 최대 3장까지 선택할 수 있습니다",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            openPhotoPicker.run();
        });

        layoutTagPeople.setOnClickListener(v -> {

            TagPeopleDialog tagDialog =
                    new TagPeopleDialog(context, selectedCount -> {

                        tvTagCount.setVisibility(android.view.View.VISIBLE);

                        tvTagCount.setText(String.valueOf(selectedCount));
                    });

            tagDialog.show();
        });

        // 완료 버튼
        btnComplete.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    // 선택된 사진 추가
    public void addSelectedImages(List<Uri> uris) {

        for (Uri uri : uris) {

            if (selectedImageUris.size() >= 3) {

                Toast.makeText(
                        context,
                        "사진은 최대 3장까지만 추가됩니다",
                        Toast.LENGTH_SHORT
                ).show();

                break;
            }

            if (!selectedImageUris.contains(uri)) {
                selectedImageUris.add(uri);
            }
        }

        renderSelectedPhotos();
    }

    // 선택된 사진 화면에 출력
    private void renderSelectedPhotos() {

        if (layoutSelectedPhotos == null) return;

        layoutSelectedPhotos.removeAllViews();

        // 사진 없으면 영역 숨김
        if (selectedImageUris.isEmpty()) {

            layoutSelectedPhotos.setVisibility(android.view.View.GONE);

            return;
        }

        layoutSelectedPhotos.setVisibility(android.view.View.VISIBLE);

        // 사진 반복 출력
        for (Uri uri : selectedImageUris) {

            // 사진 박스
            FrameLayout photoBox = new FrameLayout(context);

            LinearLayout.LayoutParams boxParams =
                    new LinearLayout.LayoutParams(dp(78), dp(78));

            boxParams.setMargins(0, 0, dp(12), 0);

            photoBox.setLayoutParams(boxParams);

            // 사진 이미지
            ImageView imageView = new ImageView(context);

            imageView.setLayoutParams(
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    )
            );

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            imageView.setImageURI(uri);

            // X 삭제 버튼
            TextView btnRemove = new TextView(context);

            FrameLayout.LayoutParams removeParams =
                    new FrameLayout.LayoutParams(dp(24), dp(24));

            removeParams.gravity = Gravity.TOP | Gravity.END;

            btnRemove.setLayoutParams(removeParams);

            btnRemove.setGravity(Gravity.CENTER);

            // 팁 업로드와 동일한 스타일
            btnRemove.setText("×");

            btnRemove.setTextColor(0xFF000000);

            btnRemove.setTextSize(16);

            btnRemove.setTypeface(null, Typeface.BOLD);

            btnRemove.setBackgroundResource(R.drawable.bg_badge_btn);

            // X 클릭 시 사진 제거
            btnRemove.setOnClickListener(v -> {

                selectedImageUris.remove(uri);

                renderSelectedPhotos();
            });

            // View 추가
            photoBox.addView(imageView);

            photoBox.addView(btnRemove);

            layoutSelectedPhotos.addView(photoBox);
        }
    }

    // dp 변환 함수
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