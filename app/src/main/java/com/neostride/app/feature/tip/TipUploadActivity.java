package com.neostride.app.feature.tip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.tip.model.TipUploadRequest;
import com.neostride.app.feature.tip.model.TipUploadResponse;
import com.neostride.app.feature.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class TipUploadActivity extends AppCompatActivity {

    private TextView btnFree, btnTraining, btnCourse, btnGear;
    private ImageView btnBack;
    private TextView btnDone;

    private ImageView btnGps;
    private ImageView btnAddPhoto;

    private HorizontalScrollView scrollSelectedPhotos;
    private LinearLayout layoutSelectedPhotos;

    private EditText etTitle;
    private EditText etContent;

    private String selectedCategory = "자유";

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();

    private ActivityResultLauncher<String[]> photoPickerLauncher;

    private ActivityResultLauncher<Intent> gpsRecordLauncher;
    private boolean gpsSelected = false;
    private String selectedRouteMapUri = null;

    private TipRepository tipRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tip_upload);

        getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.dimAmount = 0.7f;
        getWindow().setAttributes(params);

        getWindow().setGravity(Gravity.CENTER);

        btnFree = findViewById(R.id.btn_category_free);
        btnTraining = findViewById(R.id.btn_category_training);
        btnCourse = findViewById(R.id.btn_category_course);
        btnGear = findViewById(R.id.btn_category_gear);

        btnBack = findViewById(R.id.btn_back);
        btnDone = findViewById(R.id.btn_done);

        btnGps = findViewById(R.id.btn_gps);
        btnAddPhoto = findViewById(R.id.btn_add_photo);

        scrollSelectedPhotos = findViewById(R.id.scroll_selected_photos);
        layoutSelectedPhotos = findViewById(R.id.layout_selected_photos);

        etTitle = findViewById(R.id.et_tip_title);
        etContent = findViewById(R.id.et_tip_content);

        tipRepository = new TipRepository();

        initPhotoPicker();
        initGpsRecordLauncher();

        selectCategory(btnFree, "자유", false);

        btnFree.setOnClickListener(v -> selectCategory(btnFree, "자유", false));
        btnTraining.setOnClickListener(v -> selectCategory(btnTraining, "훈련", false));
        btnGear.setOnClickListener(v -> selectCategory(btnGear, "장비", false));
        btnCourse.setOnClickListener(v -> selectCategory(btnCourse, "코스", true));

        btnGps.setOnClickListener(v -> {
            Intent intent = new Intent(this, TipRecordSelectActivity.class);
            gpsRecordLauncher.launch(intent);
        });

        btnBack.setOnClickListener(v -> finish());

        btnAddPhoto.setOnClickListener(v -> {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(this, "사진은 최대 3장까지 선택할 수 있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            photoPickerLauncher.launch(new String[]{"image/*"});
        });

        btnDone.setOnClickListener(v -> uploadTip());
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

    private void initGpsRecordLauncher() {
        gpsRecordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    gpsSelected = data.getBooleanExtra("gpsSelected", false);
                    selectedRouteMapUri = data.getStringExtra("routeMapUri");

                    if (gpsSelected) {
                        Toast.makeText(this, "GPS 기록이 등록되었습니다", Toast.LENGTH_SHORT).show();
                        btnGps.setAlpha(1.0f);
                        renderSelectedPhotos();
                    }
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

                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                    // 일부 기기에서는 권한 유지가 실패할 수 있으므로 앱 실행 중 표시만 유지함
                }
            }
        }
    }

    private void selectCategory(
            TextView selectedButton,
            String category,
            boolean isCourse
    ) {
        btnFree.setSelected(false);
        btnTraining.setSelected(false);
        btnCourse.setSelected(false);
        btnGear.setSelected(false);

        selectedButton.setSelected(true);

        selectedCategory = category;

        btnGps.setVisibility(isCourse ? View.VISIBLE : View.GONE);
    }

    private void uploadTip() {
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

        /*
         * 서버로 보낼 이미지 URL 문자열 리스트를 생성함
         * 현재는 실제 이미지 업로드 서버가 없으므로 로컬 Uri 문자열을 임시로 보냄
         */
        ArrayList<String> imageUrlStrings = new ArrayList<>();

        for (Uri uri : selectedImageUris) {
            imageUrlStrings.add(uri.toString());
        }

        /*
         * 화면에서 선택한 한글 카테고리를 서버가 받을 값으로 변환함
         * 자유 -> FREE
         * 훈련 -> TRAINING
         * 코스 -> COURSE
         * 장비 -> GEAR
         */
        String serverCategory = convertCategory(selectedCategory);

        /*
         * GPS 경로 이미지가 없으면 null 대신 빈 문자열로 보냄
         * null을 그대로 보내면 서버에서 처리 방식에 따라 오류가 날 수 있음
         */
        String routeMapImageUrl = selectedRouteMapUri == null
                ? ""
                : selectedRouteMapUri;

        /*
         * 팁 업로드 요청 DTO를 생성함
         */
        TipUploadRequest request = new TipUploadRequest(
                serverCategory,
                title,
                content,
                gpsSelected,
                routeMapImageUrl,
                imageUrlStrings
        );

        /*
         * 임시 로그를 출력함
         * 완료 버튼 클릭 시 서버로 전송되는 request 형태를 Logcat에서 확인하기 위함
         */
        Log.e("TipUploadCheck",
                "request = {"
                        + "\"category\":\"" + serverCategory + "\", "
                        + "\"title\":\"" + title + "\", "
                        + "\"content\":\"" + content + "\", "
                        + "\"gpsVisible\":" + gpsSelected + ", "
                        + "\"routeMapImageUrl\":\"" + routeMapImageUrl + "\", "
                        + "\"imageUrls\":" + imageUrlStrings
                        + "}"
        );

        /*
         * Repository를 통해 팁 업로드 API를 호출함
         */
        tipRepository.uploadTip(
                request,
                new TipRepository.TipUploadCallback() {
                    @Override
                    public void onSuccess(TipUploadResponse response) {
                        Toast.makeText(
                                TipUploadActivity.this,
                                "팁 업로드 성공",
                                Toast.LENGTH_SHORT
                        ).show();

                        /*
                         * 업로드 성공 결과를 이전 화면인 TipFragment로 전달함
                         * TipFragment는 이 결과를 받고 목록을 다시 조회할 수 있음
                         */
                        setResult(Activity.RESULT_OK);

                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                TipUploadActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }
    private String convertCategory(String category) {
        switch (category) {
            case "훈련":
                return "TRAINING";

            case "코스":
                return "COURSE";

            case "장비":
                return "GEAR";

            default:
                return "FREE";
        }
    }

    private void renderSelectedPhotos() {
        layoutSelectedPhotos.removeAllViews();

        if (!gpsSelected && selectedImageUris.isEmpty()) {
            scrollSelectedPhotos.setVisibility(View.GONE);
            return;
        }

        scrollSelectedPhotos.setVisibility(View.VISIBLE);

        if (gpsSelected && selectedRouteMapUri != null) {
            addGpsThumbnail();
        }

        for (Uri uri : selectedImageUris) {
            addPhotoThumbnail(uri);
        }
    }

    private void addGpsThumbnail() {
        FrameLayout gpsBox = new FrameLayout(this);

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(dp(78), dp(78));
        boxParams.setMargins(0, 0, dp(12), 0);
        gpsBox.setLayoutParams(boxParams);

        ImageView gpsImage = new ImageView(this);
        gpsImage.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        gpsImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        gpsImage.setImageURI(Uri.parse(selectedRouteMapUri));

        TextView btnRemoveGps = new TextView(this);
        FrameLayout.LayoutParams removeParams =
                new FrameLayout.LayoutParams(dp(24), dp(24));
        removeParams.gravity = Gravity.TOP | Gravity.END;

        btnRemoveGps.setLayoutParams(removeParams);
        btnRemoveGps.setGravity(Gravity.CENTER);
        btnRemoveGps.setText("×");
        btnRemoveGps.setTextColor(0xFFFFFFFF);
        btnRemoveGps.setTextSize(16);
        btnRemoveGps.setTypeface(null, android.graphics.Typeface.BOLD);
        btnRemoveGps.setBackgroundColor(0xFFFF3B30);

        btnRemoveGps.setOnClickListener(v -> {
            gpsSelected = false;
            selectedRouteMapUri = null;
            renderSelectedPhotos();
        });

        gpsBox.addView(gpsImage);
        gpsBox.addView(btnRemoveGps);

        layoutSelectedPhotos.addView(gpsBox);
    }

    private void addPhotoThumbnail(Uri uri) {
        FrameLayout photoBox = new FrameLayout(this);

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(dp(78), dp(78));
        boxParams.setMargins(0, 0, dp(12), 0);
        photoBox.setLayoutParams(boxParams);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);

        TextView btnRemove = new TextView(this);
        FrameLayout.LayoutParams removeParams =
                new FrameLayout.LayoutParams(dp(24), dp(24));
        removeParams.gravity = Gravity.TOP | Gravity.END;

        btnRemove.setLayoutParams(removeParams);
        btnRemove.setGravity(Gravity.CENTER);
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

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }
}