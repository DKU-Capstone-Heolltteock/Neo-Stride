package com.neostride.app.feature.tip;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

import java.util.ArrayList;
import java.util.List;

public class TipUploadActivity extends AppCompatActivity {

    private TextView btnFree, btnTraining, btnCourse, btnGear;
    private ImageView btnBack;
    private TextView btnDone;
    private ImageView btnGps, btnAddPhoto;
    private LinearLayout layoutSelectedPhotos;

    private EditText etTitle, etContent;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<String[]> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_upload);

        btnFree = findViewById(R.id.btn_category_free);
        btnTraining = findViewById(R.id.btn_category_training);
        btnCourse = findViewById(R.id.btn_category_course);
        btnGear = findViewById(R.id.btn_category_gear);

        btnBack = findViewById(R.id.btn_back);
        btnDone = findViewById(R.id.btn_done);

        btnGps = findViewById(R.id.btn_gps);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        layoutSelectedPhotos = findViewById(R.id.layout_selected_photos);

        etTitle = findViewById(R.id.et_tip_title);
        etContent = findViewById(R.id.et_tip_content);

        initPhotoPicker();

        selectCategory(btnFree, false);

        btnFree.setOnClickListener(v -> selectCategory(btnFree, false));
        btnTraining.setOnClickListener(v -> selectCategory(btnTraining, false));
        btnGear.setOnClickListener(v -> selectCategory(btnGear, false));
        btnCourse.setOnClickListener(v -> selectCategory(btnCourse, true));

        btnGps.setOnClickListener(v ->
                Toast.makeText(this, "GPS 기능 준비중입니다", Toast.LENGTH_SHORT).show()
        );

        btnBack.setOnClickListener(v -> finish());

        btnAddPhoto.setOnClickListener(v -> {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(this, "사진은 최대 3장까지 선택할 수 있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            photoPickerLauncher.launch(new String[]{"image/*"});
        });

        btnDone.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "팁 작성 완료 예정", Toast.LENGTH_SHORT).show();
        });
    }

    private void initPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        return;
                    }

                    addSelectedImages(uris);
                    renderSelectedPhotos();
                }
        );
    }

    private void addSelectedImages(List<Uri> uris) {
        for (Uri uri : uris) {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(this, "사진은 최대 3장까지만 추가됩니다", Toast.LENGTH_SHORT).show();
                break;
            }

            if (!selectedImageUris.contains(uri)) {
                selectedImageUris.add(uri);
            }
        }
    }

    private void selectCategory(TextView selectedButton, boolean isCourse) {
        btnFree.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);

        btnGps.setVisibility(isCourse ? View.VISIBLE : View.GONE);
    }

    private void renderSelectedPhotos() {
        layoutSelectedPhotos.removeAllViews();

        if (selectedImageUris.isEmpty()) {
            layoutSelectedPhotos.setVisibility(View.GONE);
            return;
        }

        layoutSelectedPhotos.setVisibility(View.VISIBLE);

        for (Uri uri : selectedImageUris) {
            FrameLayout photoBox = new FrameLayout(this);

            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(dp(78), dp(78));
            boxParams.setMargins(0, 0, dp(12), 0);
            photoBox.setLayoutParams(boxParams);

            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(uri);

            TextView btnRemove = new TextView(this);
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(dp(24), dp(24));
            removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setGravity(android.view.Gravity.CENTER);
            btnRemove.setText("×");
            btnRemove.setTextColor(0xFF000000);
            btnRemove.setTextSize(16);
            btnRemove.setTypeface(null, android.graphics.Typeface.BOLD);
            btnRemove.setBackgroundResource(R.drawable.bg_badge_btn);

            btnRemove.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                renderSelectedPhotos();
            });

            photoBox.addView(imageView);
            photoBox.addView(btnRemove);

            layoutSelectedPhotos.addView(photoBox);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}