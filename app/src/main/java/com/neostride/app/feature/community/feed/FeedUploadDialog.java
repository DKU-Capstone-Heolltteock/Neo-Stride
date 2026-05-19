package com.neostride.app.feature.community.feed;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.neostride.app.R;
import com.neostride.app.feature.community.feed.model.FeedDetailResponse;
import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.feed.model.FeedUploadRequest;
import com.neostride.app.feature.community.feed.model.TagUser;
import com.neostride.app.feature.community.feed.repository.FeedRepository;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * 피드 업로드 다이얼로그 클래스임
 * 사용자가 기록을 기반으로 피드 제목, 내용, 공개 범위, 사진, 태그 등을 입력한 뒤 업로드하도록 처리함
 */
public class FeedUploadDialog {

    private final Context context;
    private final RunningRecordResponse recordData;
    private String routeMapImageUri;
    private final Runnable openPhotoPicker;
    private final OnFeedUploadedListener onFeedUploadedListener;

    private Dialog dialog;
    private LinearLayout layoutSelectedPhotos;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();

    // 태그 선택된 사용자 ID 목록을 저장함
    private final ArrayList<Long> selectedTaggedUserIds = new ArrayList<>();

    private TextView tvPrivacyValue;
    private EditText etFeedTitle;
    private EditText etFeedContent;
    private SwitchCompat switchMapVisible;

    private int selectedTagCount = 0;

    // 편집 모드 — 신규 작성과 동일 다이얼로그를 재사용 (recordData 없이)
    private final boolean isEditMode;
    private final Long editFeedId;
    // 편집 시 보존할 기존 러닝 통계 (recordData 자리 대체용)
    private double editDistance = 0.0;
    private String editDurationText = "00:00";
    private String editPaceText = "0:00/km";

    /*
     * 신규 작성용 생성자 (기록 상세에서 업로드 버튼 눌렀을 때)
     */
    public FeedUploadDialog(
            Context context,
            RunningRecordResponse recordData,
            String routeMapImageUri,
            Runnable openPhotoPicker,
            OnFeedUploadedListener onFeedUploadedListener
    ) {
        this.context = context;
        this.recordData = recordData;
        this.routeMapImageUri = routeMapImageUri;
        this.openPhotoPicker = openPhotoPicker;
        this.onFeedUploadedListener = onFeedUploadedListener;
        this.isEditMode = false;
        this.editFeedId = null;
    }

    /*
     * 편집 모드 생성자 — 기존 피드 상세를 불러와 폼에 채움
     * 사진 추가 동작이 필요 없는 경우 openPhotoPicker는 빈 Runnable 전달 가능
     */
    public FeedUploadDialog(
            Context context,
            Long feedId,
            Runnable openPhotoPicker,
            OnFeedUploadedListener onFeedUploadedListener
    ) {
        this.context = context;
        this.recordData = null;
        this.routeMapImageUri = null;
        this.openPhotoPicker = openPhotoPicker != null ? openPhotoPicker : () -> {};
        this.onFeedUploadedListener = onFeedUploadedListener;
        this.isEditMode = true;
        this.editFeedId = feedId;
    }

    /*
     * 피드 업로드 다이얼로그를 화면에 표시하는 함수임
     */
    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_feed_upload);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();
            int sw = context.getResources().getDisplayMetrics().widthPixels;
            int sh = context.getResources().getDisplayMetrics().heightPixels;
            params.width  = (int) (sw * 0.92f);
            params.height = (int) (sh * 0.85f);
            params.gravity = Gravity.CENTER;

            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.7f);
        }

        ImageView btnAddPhoto = dialog.findViewById(R.id.btn_add_photo);
        TextView btnComplete = dialog.findViewById(R.id.btn_complete_upload);
        ImageView btnBack = dialog.findViewById(R.id.btn_back_upload);
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());

        layoutSelectedPhotos = dialog.findViewById(R.id.layout_selected_photos);

        LinearLayout layoutTagPeople = dialog.findViewById(R.id.layout_tag_people);
        TextView tvTagCount = dialog.findViewById(R.id.tv_tag_count);

        LinearLayout layoutPrivacy = dialog.findViewById(R.id.layout_privacy);
        tvPrivacyValue = dialog.findViewById(R.id.tv_privacy_value);

        etFeedTitle = dialog.findViewById(R.id.et_feed_title);
        etFeedContent = dialog.findViewById(R.id.et_feed_content);
        switchMapVisible = dialog.findViewById(R.id.switch_map_visible);

        // 사진 추가 버튼 클릭 이벤트를 처리함
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

        // 태그할 사람 선택 다이얼로그를 표시함
        layoutTagPeople.setOnClickListener(v -> {
            TagPeopleDialog tagDialog =
                    new TagPeopleDialog(context, selectedUsers -> {
                        selectedTaggedUserIds.clear();

                        for (TagUser user : selectedUsers) {
                            if (user.getUserId() != null) {
                                selectedTaggedUserIds.add(user.getUserId());
                            }
                        }

                        selectedTagCount = selectedTaggedUserIds.size();

                        tvTagCount.setVisibility(android.view.View.VISIBLE);
                        tvTagCount.setText(String.valueOf(selectedTagCount));
                    });

            tagDialog.show();
        });

        // 공개 범위 선택 다이얼로그를 표시함
        layoutPrivacy.setOnClickListener(v -> {
            PrivacyDialog privacyDialog =
                    new PrivacyDialog(
                            context,
                            tvPrivacyValue.getText().toString(),
                            selectedPrivacy -> tvPrivacyValue.setText(selectedPrivacy)
                    );

            privacyDialog.show();
        });

        // 완료 버튼 클릭 시 — 편집 모드면 업데이트, 신규면 업로드
        btnComplete.setOnClickListener(v -> {
            if (isEditMode) {
                updateFeed();
            } else {
                uploadFeed();
            }
        });

        dialog.show();

        // 편집 모드면 타이틀 "Feed Edit", 완료 버튼은 그대로 "완료" 유지
        if (isEditMode && editFeedId != null) {
            TextView tvUploadTitle = dialog.findViewById(R.id.tv_upload_title);
            if (tvUploadTitle != null) tvUploadTitle.setText("Feed Edit");
            prefillExistingFeed();
        }
    }

    /*
     * 편집 모드 — 기존 피드 상세 조회 후 폼 채움
     */
    private void prefillExistingFeed() {
        FeedRepository repo = new FeedRepository(context);
        repo.getFeedDetail(editFeedId, new FeedRepository.RepositoryCallback<FeedDetailResponse>() {
            @Override
            public void onSuccess(FeedDetailResponse detail) {
                if (detail == null) return;
                etFeedTitle.setText(detail.getTitle() != null ? detail.getTitle() : "");
                etFeedContent.setText(detail.getContent() != null ? detail.getContent() : "");
                switchMapVisible.setChecked(detail.isMapVisible());

                // 기록/지도 정보 보존
                if (detail.getDistance() != null) editDistance = parseFirstNumber(detail.getDistance());
                if (detail.getDuration() != null) editDurationText = detail.getDuration();
                if (detail.getPace() != null) editPaceText = detail.getPace();
                if (detail.getRouteMapImageUri() != null) routeMapImageUri = detail.getRouteMapImageUri();
                selectedTagCount = detail.getTaggedCount();

                // 기존 이미지 표시
                if (detail.getImageUrls() != null) {
                    for (String url : detail.getImageUrls()) {
                        if (url == null || url.isEmpty()) continue;
                        try { selectedImageUris.add(Uri.parse(url)); } catch (Exception ignored) {}
                    }
                    renderSelectedPhotos();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(context, "피드 정보 불러오기 실패: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseFirstNumber(String text) {
        if (text == null) return 0.0;
        try {
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isDigit(c) || c == '.') sb.append(c);
                else if (sb.length() > 0) break;
            }
            return sb.length() == 0 ? 0.0 : Double.parseDouble(sb.toString());
        } catch (Exception e) { return 0.0; }
    }

    /*
     * 편집 모드 완료 — PUT 호출
     */
    private void updateFeed() {
        String title = etFeedTitle.getText().toString().trim();
        String content = etFeedContent.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(context, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean mapVisible = switchMapVisible.isChecked();

        FeedUploadRequest request = new FeedUploadRequest(
                title, content,
                convertPrivacyToServerValue(tvPrivacyValue.getText().toString()),
                mapVisible, mapVisible ? routeMapImageUri : null,
                new ArrayList<>(selectedTaggedUserIds),
                convertImageUrisToStrings(),
                editDistance, editDurationText, editPaceText, selectedTagCount
        );

        FeedRepository repo = new FeedRepository(context);
        repo.updateFeed(editFeedId, request, new FeedRepository.RepositoryCallback<FeedResponse>() {
            @Override
            public void onSuccess(FeedResponse result) {
                if (onFeedUploadedListener != null) onFeedUploadedListener.onFeedUploaded(result);
                Toast.makeText(context, "피드가 수정되었습니다", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * 피드 업로드 요청을 생성하고 Repository를 통해 업로드하는 함수임
     */
    private void uploadFeed() {
        String title = etFeedTitle.getText().toString().trim();
        String content = etFeedContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(context, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean mapVisible = switchMapVisible.isChecked();

        FeedUploadRequest request = new FeedUploadRequest(
                title,
                content,
                convertPrivacyToServerValue(tvPrivacyValue.getText().toString()),
                mapVisible,
                mapVisible ? routeMapImageUri : null,
                new ArrayList<>(selectedTaggedUserIds),
                convertImageUrisToStrings(),
                recordData.getDistance(),
                formatRunningTime((int) recordData.getTime()),
                formatPace(recordData.getPace()),
                selectedTagCount
        );
        //임시로 로그찍음
        android.util.Log.e(
                "FeedUploadCheck",
                "request = {"
                        + "\"title\":\"" + title + "\", "
                        + "\"content\":\"" + content + "\", "
                        + "\"privacy\":\"" + convertPrivacyToServerValue(tvPrivacyValue.getText().toString()) + "\", "
                        + "\"mapVisible\":" + mapVisible + ", "
                        + "\"routeMapImageUri\":\"" + (mapVisible ? routeMapImageUri : null) + "\", "
                        + "\"taggedUserIds\":" + selectedTaggedUserIds + ", "
                        + "\"imageUrls\":" + convertImageUrisToStrings() + ", "
                        + "\"distance\":" + recordData.getDistance() + ", "
                        + "\"runningTime\":\"" + formatRunningTime((int) recordData.getTime()) + "\", "
                        + "\"pace\":\"" + formatPace(recordData.getPace()) + "\", "
                        + "\"tagCount\":" + selectedTagCount
                        + "}"
        );
        FeedRepository feedRepository = new FeedRepository(context);

        feedRepository.uploadFeed(
                request,
                new FeedRepository.RepositoryCallback<FeedResponse>() {
                    @Override
                    public void onSuccess(FeedResponse result) {
                        if (onFeedUploadedListener != null) {
                            onFeedUploadedListener.onFeedUploaded(result);
                        }

                        Toast.makeText(
                                context,
                                "피드 업로드 성공",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 사용자가 선택한 이미지들을 추가하는 함수임
     */
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

    /*
     * 선택된 사진 미리보기를 다시 그리는 함수임
     */
    private void renderSelectedPhotos() {
        if (layoutSelectedPhotos == null) {
            return;
        }

        layoutSelectedPhotos.removeAllViews();

        if (selectedImageUris.isEmpty()) {
            layoutSelectedPhotos.setVisibility(android.view.View.GONE);
            return;
        }

        layoutSelectedPhotos.setVisibility(android.view.View.VISIBLE);

        for (Uri uri : selectedImageUris) {
            FrameLayout photoBox = new FrameLayout(context);

            LinearLayout.LayoutParams boxParams =
                    new LinearLayout.LayoutParams(dp(78), dp(78));
            boxParams.setMargins(0, 0, dp(12), 0);
            photoBox.setLayoutParams(boxParams);

            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    )
            );
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(uri);

            TextView btnRemove = new TextView(context);

            FrameLayout.LayoutParams removeParams =
                    new FrameLayout.LayoutParams(dp(24), dp(24));
            removeParams.gravity = Gravity.TOP | Gravity.END;

            btnRemove.setLayoutParams(removeParams);
            btnRemove.setGravity(Gravity.CENTER);
            btnRemove.setText("×");
            btnRemove.setTextColor(0xFF000000);
            btnRemove.setTextSize(16);
            btnRemove.setTypeface(null, Typeface.BOLD);
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

    /*
     * 화면에 표시되는 공개 범위 문구를 서버용 값으로 변환하는 함수임
     */
    private String convertPrivacyToServerValue(String privacyText) {
        String text = privacyText.trim();

        switch (text) {
            case "나만 보기":
                return "PRIVATE";

            case "친구":
            case "친구에게만":
                return "FRIEND";

            case "뱃지홀더":
            case "뱃지홀더에게만":
            case "배지홀더":
            case "배지홀더에게만":
                return "BADGE_HOLDER";

            case "전체":
            case "모두":
            case "전체 공개":
            case "공개":
                return "PUBLIC";

            default:
                android.util.Log.e("FeedUploadCheck", "알 수 없는 공개범위 값 = " + privacyText);
                return "FRIEND";
        }
    }

    /*
     * 선택된 이미지 Uri 목록을 문자열 목록으로 변환하는 함수임
     */
    private List<String> convertImageUrisToStrings() {
        List<String> imageUrls = new ArrayList<>();

        for (Uri uri : selectedImageUris) {
            imageUrls.add(uri.toString());
        }

        return imageUrls;
    }

    /*
     * 초 단위 러닝 시간을 mm:ss 형식으로 변환하는 함수임
     */
    private String formatRunningTime(int seconds) {
        return String.format(
                Locale.KOREA,
                "%02d:%02d",
                seconds / 60,
                seconds % 60
        );
    }

    /*
     * 페이스 값을 m:ss/km 형식으로 변환하는 함수임
     */
    private String formatPace(double paceValue) {
        int minute = (int) paceValue;
        int second = (int) ((paceValue - minute) * 60);

        return String.format(
                Locale.KOREA,
                "%d:%02d/km",
                minute,
                second
        );
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

    /*
     * 피드 업로드 성공 결과를 외부로 전달하기 위한 인터페이스임
     */
    public interface OnFeedUploadedListener {
        void onFeedUploaded(FeedResponse response);
    }
}