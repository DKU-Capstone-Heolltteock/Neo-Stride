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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.feed.api.FeedApi;
import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.FeedUploadResponse;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedUploadDialog {

    private final Context context;
    private final RunningRecordResponse recordData;
    private final String routeMapImageUri;
    private final Runnable openPhotoPicker;
    private final OnFeedUploadedListener onFeedUploadedListener;

    private Dialog dialog;
    private LinearLayout layoutSelectedPhotos;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();

    private TextView tvPrivacyValue;
    private EditText etFeedTitle;
    private EditText etFeedContent;
    private SwitchCompat switchMapVisible;

    private int selectedTagCount = 0;

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
    }

    public void show() {
        dialog = new Dialog(context);
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

        ImageView btnAddPhoto = dialog.findViewById(R.id.btn_add_photo);
        TextView btnComplete = dialog.findViewById(R.id.btn_complete_upload);

        layoutSelectedPhotos = dialog.findViewById(R.id.layout_selected_photos);

        LinearLayout layoutTagPeople = dialog.findViewById(R.id.layout_tag_people);
        TextView tvTagCount = dialog.findViewById(R.id.tv_tag_count);

        LinearLayout layoutPrivacy = dialog.findViewById(R.id.layout_privacy);
        tvPrivacyValue = dialog.findViewById(R.id.tv_privacy_value);

        etFeedTitle = dialog.findViewById(R.id.et_feed_title);
        etFeedContent = dialog.findViewById(R.id.et_feed_content);
        switchMapVisible = dialog.findViewById(R.id.switch_map_visible);

        btnAddPhoto.setOnClickListener(v -> {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(context, "사진은 최대 3장까지 선택할 수 있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            openPhotoPicker.run();
        });

        layoutTagPeople.setOnClickListener(v -> {
            TagPeopleDialog tagDialog =
                    new TagPeopleDialog(context, selectedCount -> {
                        selectedTagCount = selectedCount;

                        tvTagCount.setVisibility(android.view.View.VISIBLE);
                        tvTagCount.setText(String.valueOf(selectedCount));
                    });

            tagDialog.show();
        });

        layoutPrivacy.setOnClickListener(v -> {
            PrivacyDialog privacyDialog =
                    new PrivacyDialog(
                            context,
                            tvPrivacyValue.getText().toString(),
                            selectedPrivacy -> tvPrivacyValue.setText(selectedPrivacy)
                    );

            privacyDialog.show();
        });

        btnComplete.setOnClickListener(v -> {
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
                    new ArrayList<>(),
                    convertImageUrisToStrings(),
                    recordData.getDistance(),
                    formatRunningTime((int) recordData.getTime()),
                    formatPace(recordData.getPace()),
                    selectedTagCount
            );

            FeedApi feedApi = ApiClient.getInstance().create(FeedApi.class);

            feedApi.uploadFeed(request).enqueue(new Callback<FeedUploadResponse>() {
                @Override
                public void onResponse(
                        Call<FeedUploadResponse> call,
                        Response<FeedUploadResponse> response
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        FeedUploadResponse result = response.body();

                        if (onFeedUploadedListener != null) {
                            onFeedUploadedListener.onFeedUploaded(result);
                        }

                        Toast.makeText(
                                context,
                                "피드 업로드 성공",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    } else {
                        Toast.makeText(
                                context,
                                "피드 업로드 실패: " + response.code(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                @Override
                public void onFailure(
                        Call<FeedUploadResponse> call,
                        Throwable t
                ) {
                    Toast.makeText(
                            context,
                            "서버 연결 실패: " + t.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });


        // =========================
        // 기존 Mock 코드
        // =========================

        /*FeedUploadResponse response =
                MockFeedData.createUploadFeedResponse(request);

        if (onFeedUploadedListener != null) {
            onFeedUploadedListener.onFeedUploaded(response);
        }

        Toast.makeText(
                context,
                "피드 업로드 Mock 성공: " + response.getTitle(),
                Toast.LENGTH_SHORT
        ).show();

        dialog.dismiss();
    });*/

        dialog.show();
    }

    public void addSelectedImages(List<Uri> uris) {
        for (Uri uri : uris) {
            if (selectedImageUris.size() >= 3) {
                Toast.makeText(context, "사진은 최대 3장까지만 추가됩니다", Toast.LENGTH_SHORT).show();
                break;
            }

            if (!selectedImageUris.contains(uri)) {
                selectedImageUris.add(uri);
            }
        }

        renderSelectedPhotos();
    }

    private void renderSelectedPhotos() {
        if (layoutSelectedPhotos == null) return;

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

    private String convertPrivacyToServerValue(String privacyText) {
        switch (privacyText) {
            case "나만 보기":
                return "PRIVATE";

            case "친구":
            case "친구에게만":
                return "FRIEND";

            case "뱃지홀더":
            case "뱃지홀더에게만":
                return "BADGE_HOLDER";

            case "모두":
            case "모두에게만":
                return "PUBLIC";

            default:
                return "FRIEND";
        }
    }

    private List<String> convertImageUrisToStrings() {
        List<String> imageUrls = new ArrayList<>();

        for (Uri uri : selectedImageUris) {
            imageUrls.add(uri.toString());
        }

        return imageUrls;
    }

    private String formatRunningTime(int seconds) {
        return String.format(
                Locale.KOREA,
                "%02d:%02d",
                seconds / 60,
                seconds % 60
        );
    }

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

    private int dp(int value) {
        return (int) (
                value *
                        context.getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    public interface OnFeedUploadedListener {
        void onFeedUploaded(FeedUploadResponse response);
    }
}