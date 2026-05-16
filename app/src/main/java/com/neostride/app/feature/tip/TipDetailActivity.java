package com.neostride.app.feature.tip;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.feature.tip.model.TipCommentResponse;
import com.neostride.app.feature.tip.model.TipDetailResponse;
import com.neostride.app.feature.tip.repository.TipRepository;

import java.util.List;

/*
 * 팁 상세 화면을 담당하는 Activity 클래스임
 * 팁 목록에서 전달받은 tipId를 기준으로 상세 API를 호출하고,
 * 서버 응답 데이터를 화면에 표시함
 */
public class TipDetailActivity extends AppCompatActivity {

    private static final int POINT_COLOR = Color.parseColor("#B8FF06");
    private static final int WHITE_COLOR = Color.parseColor("#FFFFFF");

    private ImageView btnBack;
    private ImageView ivProfile;
    private ImageView ivBadge;
    private ImageView ivGps;
    private ImageView ivTipImage;
    private ImageView ivBookmark;
    private ImageView btnSendComment;

    private TextView tvNickname;
    private TextView tvTime;
    private TextView tvMore;
    private TextView tvCategory;
    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private TextView tvEmptyComment;

    private EditText etComment;

    // 댓글 목록을 동적으로 추가할 레이아웃임
    private LinearLayout layoutComments;

    private TipRepository tipRepository;

    private Long tipId;

    private int likeCount = 0;
    private int commentCount = 0;

    private boolean isLiked = false;
    private boolean isBookmarked = false;
    private boolean isMine = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_detail);

        tipRepository = new TipRepository();

        initViews();
        getIntentData();
        setupClickEvents();

        loadTipDetail();
    }

    /*
     * XML View를 Java 변수와 연결하는 함수임
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_tip_detail_back);

        ivProfile = findViewById(R.id.iv_tip_detail_profile);
        ivBadge = findViewById(R.id.iv_tip_detail_badge);
        ivGps = findViewById(R.id.iv_tip_detail_gps);
        ivTipImage = findViewById(R.id.iv_tip_detail_image);
        ivBookmark = findViewById(R.id.iv_tip_detail_bookmark);
        btnSendComment = findViewById(R.id.btn_tip_detail_send_comment);

        tvNickname = findViewById(R.id.tv_tip_detail_nickname);
        tvTime = findViewById(R.id.tv_tip_detail_time);
        tvMore = findViewById(R.id.tv_tip_detail_more);
        tvCategory = findViewById(R.id.tv_tip_detail_category);
        tvTitle = findViewById(R.id.tv_tip_detail_title);
        tvContent = findViewById(R.id.tv_tip_detail_content);
        tvLikeCount = findViewById(R.id.tv_tip_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_tip_detail_comment_count);

        layoutComments = findViewById(R.id.layout_tip_detail_comments);
        tvEmptyComment = findViewById(R.id.tv_tip_detail_empty_comment);

        etComment = findViewById(R.id.et_tip_detail_comment);
    }

    /*
     * TipAdapter에서 전달한 tipId를 가져오는 함수임
     */
    private void getIntentData() {
        long receivedTipId = getIntent().getLongExtra("tipId", -1L);

        if (receivedTipId == -1L) {
            Toast.makeText(this, "팁 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tipId = receivedTipId;
    }

    /*
     * tipId를 기준으로 팁 상세 정보를 조회하는 함수임
     */
    private void loadTipDetail() {
        if (tipId == null) {
            Toast.makeText(this, "팁 ID가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tipRepository.getTipDetail(
                tipId,
                new TipRepository.TipDetailCallback() {
                    @Override
                    public void onSuccess(TipDetailResponse response) {
                        bindData(response);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                TipDetailActivity.this,
                                "팁 상세 정보를 불러오지 못했습니다: " + message,
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }
                }
        );
    }

    /*
     * 상세 API 응답 데이터를 화면에 표시하는 함수임
     */
    private void bindData(TipDetailResponse response) {
        if (response == null) {
            return;
        }

        likeCount = response.getLikeCount();
        commentCount = response.getCommentCount();

        isLiked = response.isLiked();
        isBookmarked = response.isBookmarked();
        isMine = response.isMine();

        tvNickname.setText(getSafeText(response.getNickname(), "알 수 없음"));
        tvTime.setText("· " + getSafeText(response.getCreatedAt(), "방금 전"));

        tvCategory.setText(convertCategoryToKorean(response.getCategory()));
        tvTitle.setText(getSafeText(response.getTitle(), ""));
        tvContent.setText(getSafeText(response.getContent(), ""));

        tvLikeCount.setText("좋아요 " + likeCount);
        tvCommentCount.setText("댓글 " + commentCount);

        ivProfile.setImageResource(R.drawable.ic_profile);
        ivProfile.setImageTintList(null);

        ivBadge.setVisibility(response.isBadgeOwned() ? View.VISIBLE : View.GONE);
        ivGps.setVisibility(response.isGpsVisible() ? View.VISIBLE : View.GONE);

        bindImage(response);

        setLikeColor(isLiked);
        setBookmarkColor(isBookmarked);

        /*
         * 본인이 작성한 글이 아니면 점3개 메뉴를 숨김
         */
        tvMore.setVisibility(isMine ? View.VISIBLE : View.GONE);

        /*
         * 상세 API 응답의 댓글 목록을 화면에 표시함
         */
        bindComments(response.getComments());
    }

    /*
     * 이미지 또는 지도 이미지를 화면에 표시하는 함수임
     * 현재는 Glide 없이 Uri 문자열을 직접 파싱해서 표시함
     */
    private void bindImage(TipDetailResponse response) {
        List<String> imageUrls = response.getImageUrls();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            String firstImageUrl = imageUrls.get(0);

            if (firstImageUrl != null && !firstImageUrl.trim().isEmpty()) {
                ivTipImage.setVisibility(View.VISIBLE);
                ivTipImage.setImageURI(Uri.parse(firstImageUrl));
                return;
            }
        }

        if (response.isGpsVisible()
                && response.getRouteMapImageUrl() != null
                && !response.getRouteMapImageUrl().trim().isEmpty()) {

            ivTipImage.setVisibility(View.VISIBLE);
            ivTipImage.setImageURI(Uri.parse(response.getRouteMapImageUrl()));
            return;
        }

        ivTipImage.setVisibility(View.GONE);
    }

    /*
     * 상세 API 응답으로 받은 댓글 목록을 화면에 표시하는 함수임
     */
    private void bindComments(List<TipCommentResponse> comments) {
        layoutComments.removeAllViews();

        if (comments == null || comments.isEmpty()) {
            tvEmptyComment.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyComment.setVisibility(View.GONE);

        for (TipCommentResponse comment : comments) {
            View commentView = createCommentView(comment);
            layoutComments.addView(commentView);
        }
    }

    /*
     * 댓글 1개 View를 코드로 생성하는 함수임
     */
    private View createCommentView(TipCommentResponse comment) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(10), 0, dp(10));

        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        root.setLayoutParams(rootParams);

        /*
         * 댓글 상단 영역임
         */
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(32)
        );
        topRow.setLayoutParams(topRowParams);

        ImageView profile = new ImageView(this);
        LinearLayout.LayoutParams profileParams =
                new LinearLayout.LayoutParams(dp(25), dp(25));
        profile.setLayoutParams(profileParams);
        profile.setImageResource(R.drawable.ic_profile);

        TextView nameAndTime = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        nameParams.setMargins(dp(7), 0, 0, 0);
        nameAndTime.setLayoutParams(nameParams);
        nameAndTime.setText(
                getSafeText(comment.getNickname(), "알 수 없음")
                        + " · "
                        + getSafeText(comment.getCreatedAt(), "방금 전")
        );
        nameAndTime.setTextColor(Color.WHITE);
        nameAndTime.setTextSize(13);
        nameAndTime.setTypeface(null, Typeface.BOLD);

        TextView more = new TextView(this);
        LinearLayout.LayoutParams moreParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        more.setLayoutParams(moreParams);
        more.setGravity(Gravity.CENTER);
        more.setText("•••");
        more.setTextColor(Color.WHITE);
        more.setTextSize(14);

        /*
         * 본인이 작성한 댓글일 때만 점3개를 보여줌
         */
        more.setVisibility(comment.isMine() ? View.VISIBLE : View.INVISIBLE);
        more.setOnClickListener(v -> showCommentMoreMenu(more, comment));

        topRow.addView(profile);
        topRow.addView(nameAndTime);
        topRow.addView(more);

        TextView contentView = new TextView(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentParams.setMargins(dp(32), dp(2), 0, 0);
        contentView.setLayoutParams(contentParams);
        contentView.setText(getSafeText(comment.getContent(), ""));
        contentView.setTextColor(Color.parseColor("#DADADA"));
        contentView.setTextSize(13);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.setMargins(0, dp(10), 0, 0);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(Color.parseColor("#6A6A6A"));

        root.addView(topRow);
        root.addView(contentView);
        root.addView(divider);

        return root;
    }

    /*
     * 클릭 이벤트를 설정하는 함수임
     */
    private void setupClickEvents() {
        btnBack.setOnClickListener(v -> finish());

        tvMore.setOnClickListener(v -> showMoreMenu());

        tvLikeCount.setOnClickListener(v -> toggleLike());

        ivBookmark.setOnClickListener(v -> toggleBookmark());

        ivGps.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "지도 위치 보기 기능 연결 예정",
                    Toast.LENGTH_SHORT
            ).show();
        });

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
                    "댓글 작성 API 연결 예정",
                    Toast.LENGTH_SHORT
            ).show();

            etComment.setText("");
        });
    }

    /*
     * 좋아요 상태를 변경하는 함수임
     * 현재는 로컬 UI만 변경함
     */
    private void toggleLike() {
        isLiked = !isLiked;

        if (isLiked) {
            likeCount++;
        } else {
            likeCount = Math.max(0, likeCount - 1);
        }

        tvLikeCount.setText("좋아요 " + likeCount);
        setLikeColor(isLiked);
    }

    /*
     * 좋아요 색상을 변경하는 함수임
     */
    private void setLikeColor(boolean liked) {
        int color = liked ? POINT_COLOR : WHITE_COLOR;

        tvLikeCount.setTextColor(color);
        tvLikeCount.setTypeface(null, Typeface.BOLD);
    }

    /*
     * 북마크 상태를 변경하는 함수임
     * 현재는 로컬 UI만 변경함
     */
    private void toggleBookmark() {
        isBookmarked = !isBookmarked;
        setBookmarkColor(isBookmarked);
    }

    /*
     * 북마크 색상을 변경하는 함수임
     */
    private void setBookmarkColor(boolean bookmarked) {
        int color = bookmarked ? POINT_COLOR : WHITE_COLOR;

        ivBookmark.setImageTintList(ColorStateList.valueOf(color));
    }

    /*
     * 글 점 세 개 메뉴를 보여주는 함수임
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
                        "삭제 API 연결 예정",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    /*
     * 댓글 점 세 개 메뉴를 보여주는 함수임
     */
    private void showCommentMoreMenu(View anchorView, TipCommentResponse comment) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        popupMenu.getMenu().add("수정");
        popupMenu.getMenu().add("삭제");

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String menuTitle = menuItem.getTitle().toString();

            if (menuTitle.equals("수정")) {
                Toast.makeText(
                        this,
                        "댓글 수정 API 연결 예정",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            if (menuTitle.equals("삭제")) {
                Toast.makeText(
                        this,
                        "댓글 삭제 API 연결 예정",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    /*
     * 서버 카테고리 값을 화면 표시용 한글 카테고리로 변환하는 함수임
     */
    private String convertCategoryToKorean(String category) {
        if (category == null) {
            return "자유";
        }

        switch (category) {
            case "FREE":
                return "자유";

            case "TRAINING":
                return "훈련";

            case "COURSE":
                return "코스";

            case "GEAR":
                return "장비";

            default:
                return category;
        }
    }

    /*
     * null 또는 빈 문자열을 기본값으로 바꾸는 함수임
     */
    private String getSafeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}