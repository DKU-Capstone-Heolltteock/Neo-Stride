package com.neostride.app.feature.community.tip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.community.feed.FeedRecordPickerDialog;
import com.neostride.app.feature.community.tip.model.TipDetailResponse;
import com.neostride.app.feature.community.tip.model.TipUploadRequest;
import com.neostride.app.feature.community.tip.model.TipUploadResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class TipUploadActivity extends AppCompatActivity {

    private TextView btnFree, btnTraining, btnCourse, btnGear;
    private ImageView btnBack;
    private TextView btnDone;

    private FrameLayout layoutGpsContainer;
    private TextView btnRemoveGpsOverlay;
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
    private String selectedCourseAddress = null;

    private TipRepository tipRepository;

    // 편집 모드 지원 — Intent extras: mode="edit", tipId=<Long>
    private boolean isEditMode = false;
    private Long editTipId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tip_upload);

        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        getWindow().setLayout(
                (int) (sw * 0.92f),
                (int) (sh * 0.85f)
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

        layoutGpsContainer = findViewById(R.id.layout_gps_container);
        btnRemoveGpsOverlay = findViewById(R.id.btn_remove_gps_overlay);
        btnGps = findViewById(R.id.btn_gps);
        btnAddPhoto = findViewById(R.id.btn_add_photo);

        btnRemoveGpsOverlay.setOnClickListener(v -> {
            gpsSelected = false;
            selectedRouteMapUri = null;
            updateGpsButton();
            renderSelectedPhotos();
        });

        scrollSelectedPhotos = findViewById(R.id.scroll_selected_photos);
        layoutSelectedPhotos = findViewById(R.id.layout_selected_photos);

        etTitle = findViewById(R.id.et_tip_title);
        etContent = findViewById(R.id.et_tip_content);

        tipRepository = new TipRepository(this);

        initPhotoPicker();
        initGpsRecordLauncher();

        selectCategory(btnFree, "자유", false);

        btnFree.setOnClickListener(v -> selectCategory(btnFree, "자유", false));
        btnTraining.setOnClickListener(v -> selectCategory(btnTraining, "훈련", false));
        btnGear.setOnClickListener(v -> selectCategory(btnGear, "장비", false));
        btnCourse.setOnClickListener(v -> selectCategory(btnCourse, "코스", true));

        btnGps.setOnClickListener(v -> {
            FeedRecordPickerDialog[] pickerHolder = new FeedRecordPickerDialog[1];
            pickerHolder[0] = new FeedRecordPickerDialog(this,
                    (record, routeMapUri, address) -> {
                        if (pickerHolder[0] != null) pickerHolder[0].dismiss();
                        gpsSelected = true;
                        selectedRouteMapUri = routeMapUri;
                        selectedCourseAddress = address;
                        Toast.makeText(this, "GPS 기록이 등록되었습니다", Toast.LENGTH_SHORT).show();
                        updateGpsButton();
                        renderSelectedPhotos();
                    });
            pickerHolder[0].setCoursePicker(true);
            pickerHolder[0].show();
        });

        btnBack.setOnClickListener(v -> finish());

        btnAddPhoto.setOnClickListener(v -> {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(this, "사진은 최대 3장까지 선택할 수 있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            photoPickerLauncher.launch(new String[]{"image/*"});
        });

        btnDone.setOnClickListener(v -> submitTip());

        // 편집 모드 진입 처리
        handleEditModeIntent();
    }

    /*
     * Intent extras로 "mode=edit" + "tipId"가 들어왔으면 편집 모드로 전환
     * - 타이틀 변경
     * - 서버에서 기존 팁 데이터 조회 → 폼에 채움
     */
    private void handleEditModeIntent() {
        Intent intent = getIntent();
        if (intent == null) return;
        String mode = intent.getStringExtra("mode");
        long tipIdExtra = intent.getLongExtra("tipId", -1L);
        if (!"edit".equals(mode) || tipIdExtra <= 0) return;

        isEditMode = true;
        editTipId = tipIdExtra;

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText("Tip Edit");

        // 기존 팁 데이터 조회 후 폼 채움
        tipRepository.getTipDetail(editTipId, new TipRepository.TipDetailCallback() {
            @Override
            public void onSuccess(TipDetailResponse response) {
                if (response == null) return;
                prefillFromDetail(response);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(TipUploadActivity.this,
                        "팁 정보 불러오기 실패: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * 서버에서 받은 기존 팁 데이터로 폼을 미리 채움
     */
    private void prefillFromDetail(TipDetailResponse detail) {
        etTitle.setText(detail.getTitle() != null ? detail.getTitle() : "");
        etContent.setText(detail.getContent() != null ? detail.getContent() : "");

        // 카테고리 복원
        String cat = detail.getCategory();
        if (cat != null) {
            switch (cat) {
                case "훈련":
                case "TRAINING":
                    selectCategory(btnTraining, "훈련", false); break;
                case "코스":
                case "COURSE":
                    selectCategory(btnCourse, "코스", true); break;
                case "장비":
                case "GEAR":
                    selectCategory(btnGear, "장비", false); break;
                case "자유":
                case "FREE":
                default:
                    selectCategory(btnFree, "자유", false); break;
            }
        }

        // GPS 경로 이미지 복원
        if (detail.isGpsVisible() && detail.getRouteMapImageUrl() != null
                && !detail.getRouteMapImageUrl().isEmpty()) {
            gpsSelected = true;
            selectedRouteMapUri = detail.getRouteMapImageUrl();
            updateGpsButton();
        }

        // 이미지 URL 복원
        if (detail.getImageUrls() != null) {
            for (String url : detail.getImageUrls()) {
                if (url == null || url.isEmpty()) continue;
                try {
                    selectedImageUris.add(Uri.parse(url));
                } catch (Exception ignored) {}
            }
        }

        renderSelectedPhotos();
    }

    /*
     * 완료 버튼 — 편집 모드면 update, 신규면 upload
     */
    private void submitTip() {
        if (isEditMode) {
            updateTip();
        } else {
            uploadTip();
        }
    }

    private void updateTip() {
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

        ArrayList<String> imageUrlStrings = new ArrayList<>();
        for (Uri uri : selectedImageUris) imageUrlStrings.add(uri.toString());

        String serverCategory = convertCategory(selectedCategory);
        String routeMapImageUrl = selectedRouteMapUri == null ? "" : selectedRouteMapUri;

        String courseAddr = selectedCourseAddress != null ? selectedCourseAddress : "";
        TipUploadRequest request = new TipUploadRequest(
                serverCategory, title, content, gpsSelected, routeMapImageUrl, courseAddr, imageUrlStrings);

        tipRepository.updateTip(editTipId, request, new TipRepository.TipUploadCallback() {
            @Override
            public void onSuccess(TipUploadResponse response) {
                Toast.makeText(TipUploadActivity.this, "팁이 수정되었습니다", Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(TipUploadActivity.this, message, Toast.LENGTH_SHORT).show();
            }
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
                        updateGpsButton();
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
        // 모든 버튼 비선택 상태(회색)로 초기화
        applyCategoryButtonStyle(btnFree, false, "#00E5FF");
        applyCategoryButtonStyle(btnTraining, false, "#FF3DFF");
        applyCategoryButtonStyle(btnCourse, false, "#FFB300");
        applyCategoryButtonStyle(btnGear, false, "#00FF85");

        // 선택된 버튼에 카테고리 색상 적용
        String categoryColor = getCategoryColor(category);
        applyCategoryButtonStyle(selectedButton, true, categoryColor);

        selectedCategory = category;

        layoutGpsContainer.setVisibility(isCourse ? View.VISIBLE : View.GONE);
    }

    private void applyCategoryButtonStyle(TextView button, boolean selected, String colorHex) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20 * getResources().getDisplayMetrics().density);

        if (selected) {
            drawable.setColor(android.graphics.Color.parseColor(colorHex));
            button.setTextColor(android.graphics.Color.BLACK);
        } else {
            drawable.setColor(android.graphics.Color.parseColor("#E0E0E0"));
            button.setTextColor(android.graphics.Color.BLACK);
        }

        button.setBackground(drawable);
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "자유":   return "#00E5FF";
            case "훈련":   return "#FF3DFF";
            case "코스":   return "#FFB300";
            case "장비":   return "#00FF85";
            default:       return "#00E5FF";
        }
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
        String courseAddr = selectedCourseAddress != null ? selectedCourseAddress : "";
        TipUploadRequest request = new TipUploadRequest(
                serverCategory,
                title,
                content,
                gpsSelected,
                routeMapImageUrl,
                courseAddr,
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

        // GPS 기록은 btnGps 버튼 내부에 표시되므로 스크롤 영역에는 포함하지 않음
        if (selectedImageUris.isEmpty()) {
            scrollSelectedPhotos.setVisibility(View.GONE);
            return;
        }

        scrollSelectedPhotos.setVisibility(View.VISIBLE);

        for (Uri uri : selectedImageUris) {
            addPhotoThumbnail(uri);
        }
    }

    /**
     * GPS 기록 선택 상태에 따라 btnGps 아이콘을 업데이트함
     * - 선택됨: 경로 지도 이미지를 버튼 내부에 표시하고 × 오버레이 표시
     * - 미선택: ic_location 아이콘으로 복원하고 × 오버레이 숨김
     */
    private void updateGpsButton() {
        if (gpsSelected && selectedRouteMapUri != null) {
            Glide.with(this)
                    .load(selectedRouteMapUri)
                    .transform(new RoundedCorners(dp(6)))
                    .into(btnGps);
            btnRemoveGpsOverlay.setVisibility(View.VISIBLE);
        } else {
            Glide.with(this).clear(btnGps);
            btnGps.setScaleType(ImageView.ScaleType.FIT_CENTER);
            btnGps.setImageResource(R.drawable.ic_location);
            btnRemoveGpsOverlay.setVisibility(View.GONE);
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