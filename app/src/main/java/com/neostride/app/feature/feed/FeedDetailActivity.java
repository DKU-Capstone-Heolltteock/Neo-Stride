package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.feed.repository.FeedRepository;
import com.neostride.app.feature.mypage.MyPageActivity;

import java.util.ArrayList;
import java.util.List;

/*
 * 피드 상세 화면을 담당하는 Activity 클래스임
 * 피드 목록에서 선택한 피드 정보를 받아 상세 내용을 표시함
 */
public class FeedDetailActivity extends AppCompatActivity {

    private ImageView btnBack;

    private ImageView ivRouteArrow;
    private ImageView ivRecordArrow;

    private LinearLayout layoutRouteHeader;
    private LinearLayout layoutRecordHeader;
    private LinearLayout layoutRouteContent;
    private LinearLayout layoutRecordContent;

    private ImageView ivRouteMap;
    private ImageView ivFeedPhoto;

    private ImageView ivProfile;
    private ImageView ivBookmark;

    private TextView tvUsername;
    private TextView tvTime;
    private TextView tvMore;
    private TextView tvTitle;
    private TextView tvContent;

    private TextView tvTagCount;
    private TextView tvLikeCount;
    private TextView tvCommentCount;

    private TextView tvDistance;
    private TextView tvDuration;
    private TextView tvPace;

    private EditText etComment;
    private ImageView btnSendComment;

    private FeedRepository feedRepository;

    private boolean isRouteOpen = false;
    private boolean isRecordOpen = false;

    private boolean isLiked = false;
    private boolean isBookmarked = false;

    private Long feedId;

    private String username;
    private String time;
    private String title;
    private String content;

    private int tagCount;
    private int likeCount;
    private int commentCount;
    private int displayLikeCount;

    private String distance;
    private String duration;
    private String pace;

    private boolean mapVisible;
    private String routeMapImageUri;
    private ArrayList<String> imageUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_detail);

        feedRepository = new FeedRepository(this);

        getIntentData();

        initViews();

        bindFeedData();

        setupClickEvents();
    }

    /*
     * FeedAdapter에서 전달한 Intent 데이터를 가져오는 함수임
     */
    private void getIntentData() {
        long receivedFeedId = getIntent().getLongExtra("feedId", -1L);

        if (receivedFeedId == -1L) {
            feedId = null;
        } else {
            feedId = receivedFeedId;
        }

        username = getIntent().getStringExtra("username");
        time = getIntent().getStringExtra("time");
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");

        tagCount = getIntent().getIntExtra("tagCount", 0);
        likeCount = getIntent().getIntExtra("likeCount", 0);
        commentCount = getIntent().getIntExtra("commentCount", 0);

        distance = getIntent().getStringExtra("distance");
        duration = getIntent().getStringExtra("duration");
        pace = getIntent().getStringExtra("pace");

        mapVisible = getIntent().getBooleanExtra("mapVisible", false);
        routeMapImageUri = getIntent().getStringExtra("routeMapImageUri");

        imageUrls = getIntent().getStringArrayListExtra("imageUrls");

        if (username == null || username.trim().isEmpty()) {
            username = "알 수 없음";
        }

        if (time == null || time.trim().isEmpty()) {
            time = "방금 전";
        }

        if (title == null) {
            title = "";
        }

        if (content == null) {
            content = "";
        }

        if (distance == null || distance.trim().isEmpty()) {
            distance = "0.00 km";
        }

        if (duration == null || duration.trim().isEmpty()) {
            duration = "00:00";
        }

        if (pace == null || pace.trim().isEmpty()) {
            pace = "0:00/km";
        }

        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        displayLikeCount = likeCount;
    }

    /*
     * XML에 있는 View들을 Java 변수와 연결하는 함수임
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_feed_detail_back);

        layoutRouteHeader = findViewById(R.id.layout_route_header);
        layoutRecordHeader = findViewById(R.id.layout_record_header);
        layoutRouteContent = findViewById(R.id.layout_route_content);
        layoutRecordContent = findViewById(R.id.layout_record_content);

        ivRouteArrow = findViewById(R.id.iv_route_arrow);
        ivRecordArrow = findViewById(R.id.iv_record_arrow);

        ivRouteMap = findViewById(R.id.iv_detail_route_map);
        ivFeedPhoto = findViewById(R.id.iv_detail_feed_photo);

        ivProfile = findViewById(R.id.iv_detail_profile);
        ivBookmark = findViewById(R.id.iv_detail_bookmark);

        tvUsername = findViewById(R.id.tv_detail_username);
        tvTime = findViewById(R.id.tv_detail_time);
        tvMore = findViewById(R.id.tv_detail_more);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);

        tvTagCount = findViewById(R.id.tv_detail_tag_count);
        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_detail_comment_count);

        tvDistance = findViewById(R.id.tv_detail_distance);
        tvDuration = findViewById(R.id.tv_detail_duration);
        tvPace = findViewById(R.id.tv_detail_pace);

        etComment = findViewById(R.id.et_detail_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
    }

    /*
     * 전달받은 피드 데이터를 화면에 표시하는 함수임
     */
    private void bindFeedData() {
        tvUsername.setText(username);
        tvTime.setText("· " + time);
        tvTitle.setText(title);
        tvContent.setText(content);

        tvTagCount.setText(String.valueOf(tagCount));
        tvLikeCount.setText(String.valueOf(displayLikeCount));
        tvCommentCount.setText(String.valueOf(commentCount));

        tvDistance.setText(distance);
        tvDuration.setText(duration);
        tvPace.setText(pace);

        if (ivProfile != null) {
            ivProfile.setImageTintList(null);
            ivProfile.setImageResource(R.drawable.ic_profile);
        }

        if (ivBookmark != null) {
            ivBookmark.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }

        setLikeColor(false);

        if (!imageUrls.isEmpty()) {
            ivFeedPhoto.setVisibility(View.VISIBLE);
            ivFeedPhoto.setImageURI(Uri.parse(imageUrls.get(0)));
        } else {
            ivFeedPhoto.setVisibility(View.GONE);
        }

        if (mapVisible && routeMapImageUri != null && !routeMapImageUri.trim().isEmpty()) {
            ivRouteMap.setImageURI(Uri.parse(routeMapImageUri));
        } else {
            ivRouteMap.setImageResource(R.drawable.bg_feed_detail_empty_route);
        }

        layoutRouteContent.setVisibility(View.GONE);
        layoutRecordContent.setVisibility(View.GONE);

        ivRouteArrow.setRotation(0f);
        ivRecordArrow.setRotation(0f);
    }

    /*
     * 버튼과 영역 클릭 이벤트를 연결하는 함수임
     */
    private void setupClickEvents() {
        btnBack.setOnClickListener(v -> finish());

        layoutRouteHeader.setOnClickListener(v -> toggleRouteContent());

        layoutRecordHeader.setOnClickListener(v -> toggleRecordContent());

        View.OnClickListener profileClickListener = v -> openUserProfile(username);

        if (ivProfile != null) {
            ivProfile.setOnClickListener(profileClickListener);
        }

        tvUsername.setOnClickListener(profileClickListener);

        tvLikeCount.setOnClickListener(v -> toggleLike());

        if (ivBookmark != null) {
            ivBookmark.setOnClickListener(v -> toggleBookmark());
        }

        /*
         * 태그 숫자 클릭 시 Activity 내부 하드코딩이 아니라
         * Repository를 통해 MockFeedServer에서 태그된 사용자 목록을 가져옴
         */
        tvTagCount.setOnClickListener(v -> loadTaggedUsers());

        tvMore.setOnClickListener(v -> showMoreMenu());

        btnSendComment.setOnClickListener(v -> {
            String comment = etComment.getText().toString().trim();

            if (comment.isEmpty()) {
                Toast.makeText(
                        this,
                        "댓글을 입력해주세요",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    this,
                    "댓글 기능 연결 예정",
                    Toast.LENGTH_SHORT
            ).show();

            etComment.setText("");
        });
    }

    /*
     * 태그된 사용자 목록을 Repository에서 가져오는 함수임
     */
    private void loadTaggedUsers() {
        if (tagCount <= 0) {
            Toast.makeText(
                    this,
                    "태그된 사람이 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        feedRepository.getTaggedUsers(
                feedId,
                new FeedRepository.RepositoryCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> data) {
                        if (data == null || data.isEmpty()) {
                            Toast.makeText(
                                    FeedDetailActivity.this,
                                    "태그된 사람이 없습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        showTaggedUserDialog(data);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(
                                FeedDetailActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 해당 유저 프로필 또는 마이페이지로 이동하는 함수임
     */
    private void openUserProfile(String targetUsername) {
        Intent intent = new Intent(this, MyPageActivity.class);

        intent.putExtra("username", targetUsername);

        startActivity(intent);
    }

    /*
     * 좋아요 상태를 변경하는 함수임
     */
    private void toggleLike() {
        isLiked = !isLiked;

        if (isLiked) {
            displayLikeCount++;
        } else {
            displayLikeCount--;

            if (displayLikeCount < likeCount) {
                displayLikeCount = likeCount;
            }
        }

        tvLikeCount.setText(String.valueOf(displayLikeCount));
        setLikeColor(isLiked);
    }

    /*
     * 좋아요 TextView의 숫자 색상과 아이콘 색상을 변경하는 함수임
     */
    private void setLikeColor(boolean liked) {
        int color;

        if (liked) {
            color = Color.parseColor("#B8FF06");
        } else {
            color = Color.WHITE;
        }

        tvLikeCount.setTextColor(color);
        tintTextViewDrawables(tvLikeCount, color);
    }

    /*
     * TextView에 drawableStart로 붙은 아이콘 색상을 변경하는 함수임
     */
    private void tintTextViewDrawables(TextView textView, int color) {
        Drawable[] drawables = textView.getCompoundDrawablesRelative();

        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setTint(color);
            }
        }
    }

    /*
     * 북마크 상태를 변경하는 함수임
     */
    private void toggleBookmark() {
        isBookmarked = !isBookmarked;

        if (ivBookmark == null) {
            return;
        }

        if (isBookmarked) {
            ivBookmark.setImageTintList(
                    ColorStateList.valueOf(Color.parseColor("#B8FF06"))
            );
        } else {
            ivBookmark.setImageTintList(
                    ColorStateList.valueOf(Color.WHITE)
            );
        }
    }

    /*
     * 태그된 사람 목록을 커스텀 Dialog로 보여주는 함수임
     * 태그 목록 데이터는 MockFeedServer에서 전달받은 값을 사용함
     */
    private void showTaggedUserDialog(List<String> taggedUsers) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(22), dp(20), dp(22), dp(18));
        rootLayout.setBackgroundResource(R.drawable.bg_tagged_user_dialog);

        TextView titleView = new TextView(this);
        titleView.setText("태그된 사람");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(22);
        titleView.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        titleParams.setMargins(0, 0, 0, dp(14));
        titleView.setLayoutParams(titleParams);

        rootLayout.addView(titleView);

        for (String taggedUser : taggedUsers) {
            LinearLayout userRow = new LinearLayout(this);
            userRow.setOrientation(LinearLayout.HORIZONTAL);
            userRow.setGravity(Gravity.CENTER_VERTICAL);
            userRow.setPadding(dp(10), dp(10), dp(10), dp(10));
            userRow.setBackgroundResource(R.drawable.bg_tagged_user_item);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(58)
                    );
            rowParams.setMargins(0, 0, 0, dp(10));
            userRow.setLayoutParams(rowParams);

            ImageView profileImage = new ImageView(this);
            profileImage.setImageResource(R.drawable.ic_profile);
            profileImage.setImageTintList(null);

            LinearLayout.LayoutParams profileParams =
                    new LinearLayout.LayoutParams(dp(34), dp(34));
            profileParams.setMargins(0, 0, dp(12), 0);
            profileImage.setLayoutParams(profileParams);

            TextView nameView = new TextView(this);
            nameView.setText(taggedUser);
            nameView.setTextColor(Color.WHITE);
            nameView.setTextSize(17);
            nameView.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams nameParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );
            nameView.setLayoutParams(nameParams);

            TextView moveText = new TextView(this);
            moveText.setText("보기");
            moveText.setTextColor(Color.parseColor("#B8FF06"));
            moveText.setTextSize(13);
            moveText.setTypeface(null, Typeface.BOLD);

            FrameLayout moveBadge = new FrameLayout(this);
            moveBadge.setPadding(dp(10), dp(5), dp(10), dp(5));
            moveBadge.setBackgroundResource(R.drawable.bg_tagged_user_view_badge);
            moveBadge.addView(moveText);

            userRow.addView(profileImage);
            userRow.addView(nameView);
            userRow.addView(moveBadge);

            userRow.setOnClickListener(v -> {
                dialog.dismiss();
                openUserProfile(taggedUser);
            });

            rootLayout.addView(userRow);
        }

        TextView closeButton = new TextView(this);
        closeButton.setText("닫기");
        closeButton.setTextColor(Color.parseColor("#B8FF06"));
        closeButton.setTextSize(16);
        closeButton.setTypeface(null, Typeface.BOLD);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(0, dp(12), 0, 0);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        rootLayout.addView(closeButton);

        dialog.setContentView(rootLayout);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.86);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;

            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.65f);
        }

        dialog.show();
    }

    /*
     * 우측 점 세 개 메뉴를 보여주는 함수임
     */
    private void showMoreMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvMore);

        popupMenu.getMenu().add("수정");
        popupMenu.getMenu().add("삭제");

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String menuTitle = menuItem.getTitle().toString();

            if (menuTitle.equals("수정")) {
                Toast.makeText(
                        this,
                        "수정 기능 연결 예정",
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }

            if (menuTitle.equals("삭제")) {
                Toast.makeText(
                        this,
                        "삭제 기능 연결 예정",
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    /*
     * 달린 루트 보기 영역을 열고 닫는 함수임
     */
    private void toggleRouteContent() {
        isRouteOpen = !isRouteOpen;

        if (isRouteOpen) {
            layoutRouteContent.setVisibility(View.VISIBLE);
            ivRouteArrow.setRotation(180f);
        } else {
            layoutRouteContent.setVisibility(View.GONE);
            ivRouteArrow.setRotation(0f);
        }
    }

    /*
     * 기록 정보 보기 영역을 열고 닫는 함수임
     */
    private void toggleRecordContent() {
        isRecordOpen = !isRecordOpen;

        if (isRecordOpen) {
            layoutRecordContent.setVisibility(View.VISIBLE);
            ivRecordArrow.setRotation(180f);
        } else {
            layoutRecordContent.setVisibility(View.GONE);
            ivRecordArrow.setRotation(0f);
        }
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }
}