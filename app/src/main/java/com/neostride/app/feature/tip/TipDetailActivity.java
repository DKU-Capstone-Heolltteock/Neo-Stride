package com.neostride.app.feature.tip;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
    private static final int COMMENT_TEXT_COLOR = Color.parseColor("#DADADA");

    private ImageView btnBack;
    private ImageView ivProfile;
    private ImageView ivBadge;
    private ImageView ivGps;
    private ImageView ivTipImage;

    // 좋아요, 댓글, 북마크 아이콘임
    private ImageView ivLike;
    private ImageView ivComment;
    private ImageView ivBookmark;

    // 댓글 전송 버튼임
    private ImageView btnSendComment;

    private TextView tvNickname;
    private TextView tvTime;
    private TextView tvMore;
    private TextView tvCategory;
    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private TextView tvBookmark;
    private TextView tvEmptyComment;

    // 코스 펼치기 버튼 텍스트임
    private TextView tvCourseToggle;

    // 댓글 입력창임
    private EditText etComment;

    // 댓글 목록을 동적으로 추가할 레이아웃임
    private LinearLayout layoutComments;

    // GPS 배너 전체 영역임
    private LinearLayout layoutGpsBanner;

    // 코스 지도 펼침 영역임
    private LinearLayout layoutCourseMap;

    // 좋아요, 댓글, 북마크 버튼 전체 영역임
    private LinearLayout layoutLikeArea;
    private LinearLayout layoutCommentArea;
    private LinearLayout layoutBookmarkArea;

    // 코스 지도 이미지임
    private ImageView ivCourseMap;

    // 팁 관련 API 호출을 담당하는 Repository임
    private TipRepository tipRepository;

    // 현재 상세 화면의 팁 ID임
    private Long tipId;

    private int likeCount = 0;
    private int commentCount = 0;

    private boolean isLiked = false;
    private boolean isBookmarked = false;
    private boolean isMine = false;

    private boolean isCourseExpanded = false;
    private String courseMapImageUrl = "";

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

        ivLike = findViewById(R.id.iv_tip_detail_like);
        ivComment = findViewById(R.id.iv_tip_detail_comment);
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
        tvBookmark = findViewById(R.id.tv_tip_detail_bookmark);
        tvEmptyComment = findViewById(R.id.tv_tip_detail_empty_comment);

        tvCourseToggle = findViewById(R.id.tv_tip_detail_course_toggle);

        layoutComments = findViewById(R.id.layout_tip_detail_comments);
        layoutGpsBanner = findViewById(R.id.layout_tip_detail_gps_banner);
        layoutCourseMap = findViewById(R.id.layout_tip_detail_course_map);

        layoutLikeArea = findViewById(R.id.layout_tip_detail_like_area);
        layoutCommentArea = findViewById(R.id.layout_tip_detail_comment_area);
        layoutBookmarkArea = findViewById(R.id.layout_tip_detail_bookmark_area);

        ivCourseMap = findViewById(R.id.iv_tip_detail_course_map);

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

        /*
         * GPS 코스 팁이면 상단 GPS 배너를 표시함
         */
        if (layoutGpsBanner != null) {
            layoutGpsBanner.setVisibility(response.isGpsVisible() ? View.VISIBLE : View.GONE);
        }

        if (ivGps != null) {
            ivGps.setVisibility(View.VISIBLE);
        }

        /*
         * 코스 지도는 처음에는 접힌 상태로 초기화함
         */
        courseMapImageUrl = getSafeText(response.getRouteMapImageUrl(), "");
        isCourseExpanded = false;

        if (layoutCourseMap != null) {
            layoutCourseMap.setVisibility(View.GONE);
        }

        if (tvCourseToggle != null) {
            tvCourseToggle.setText("코스 보기");
        }

        bindCourseMapImage();

        /*
         * 일반 첨부 이미지만 본문 이미지 영역에 표시함
         * GPS 지도 이미지는 코스 보기 영역에서만 표시함
         */
        bindImage(response);

        setLikeColor(isLiked);
        setCommentColor();
        setBookmarkColor(isBookmarked);

        /*
         * 글 점3개는 항상 표시함
         * 본인 글이면 수정/삭제, 남의 글이면 신고/차단 메뉴가 뜸
         */
        tvMore.setVisibility(View.VISIBLE);

        bindComments(response.getComments());
    }

    /*
     * 일반 첨부 이미지를 화면에 표시하는 함수임
     * GPS 지도 이미지는 여기서 표시하지 않고 코스 펼침 영역에서만 표시함
     */
    private void bindImage(TipDetailResponse response) {
        List<String> imageUrls = response.getImageUrls();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            String firstImageUrl = getFirstValidUrl(imageUrls);

            if (firstImageUrl != null) {
                ivTipImage.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(firstImageUrl)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .centerCrop()
                        .into(ivTipImage);

                return;
            }
        }

        Glide.with(this).clear(ivTipImage);
        ivTipImage.setVisibility(View.GONE);
    }

    /*
     * 코스 지도 이미지를 펼침 영역에 미리 연결하는 함수임
     */
    private void bindCourseMapImage() {
        if (ivCourseMap == null) {
            return;
        }

        if (courseMapImageUrl == null || courseMapImageUrl.trim().isEmpty()) {
            ivCourseMap.setImageResource(R.drawable.ic_image);
            return;
        }

        Glide.with(this)
                .load(courseMapImageUrl)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .centerCrop()
                .into(ivCourseMap);
    }

    /*
     * 코스 보기 버튼을 눌렀을 때 코스 지도 영역을 펼치거나 접는 함수임
     */
    private void toggleCourseMap() {
        if (layoutCourseMap == null) {
            return;
        }

        if (courseMapImageUrl == null || courseMapImageUrl.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "표시할 코스 정보가 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        isCourseExpanded = !isCourseExpanded;

        layoutCourseMap.setVisibility(isCourseExpanded ? View.VISIBLE : View.GONE);

        if (tvCourseToggle != null) {
            tvCourseToggle.setText(isCourseExpanded ? "접기" : "코스 보기");
        }
    }

    /*
     * 이미지 URL 목록에서 비어있지 않은 첫 번째 URL을 찾는 함수임
     */
    private String getFirstValidUrl(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return imageUrl;
            }
        }

        return null;
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
     * 댓글을 각각 독립된 네온 카드처럼 보이게 구성함
     */
    private View createCommentView(TipCommentResponse comment) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_tip_comment_neon_item);
        root.setPadding(dp(12), dp(10), dp(10), dp(10));

        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rootParams.setMargins(0, 0, 0, dp(10));
        root.setLayoutParams(rootParams);

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
                new LinearLayout.LayoutParams(dp(26), dp(26));
        profile.setLayoutParams(profileParams);
        profile.setImageResource(R.drawable.ic_profile);
        profile.setImageTintList(null);

        TextView nameAndTime = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        nameParams.setMargins(dp(8), 0, 0, 0);
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
                new LinearLayout.LayoutParams(dp(32), dp(32));
        more.setLayoutParams(moreParams);
        more.setGravity(Gravity.CENTER);
        more.setText("•••");
        more.setTextColor(Color.WHITE);
        more.setTextSize(15);
        more.setTypeface(null, Typeface.BOLD);
        more.setVisibility(View.VISIBLE);
        more.setOnClickListener(v -> showCommentMoreMenu(more, comment));

        topRow.addView(profile);
        topRow.addView(nameAndTime);
        topRow.addView(more);

        TextView contentView = new TextView(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentParams.setMargins(dp(34), dp(4), dp(4), 0);
        contentView.setLayoutParams(contentParams);
        contentView.setText(getSafeText(comment.getContent(), ""));
        contentView.setTextColor(COMMENT_TEXT_COLOR);
        contentView.setTextSize(13);
        contentView.setLineSpacing(dp(2), 1.0f);

        root.addView(topRow);
        root.addView(contentView);

        return root;
    }

    /*
     * 클릭 이벤트를 설정하는 함수임
     */
    private void setupClickEvents() {
        btnBack.setOnClickListener(v -> finish());

        tvMore.setOnClickListener(v -> showMoreMenu());

        if (tvLikeCount != null) {
            tvLikeCount.setOnClickListener(v -> toggleLike());
        }

        if (ivLike != null) {
            ivLike.setOnClickListener(v -> toggleLike());
        }

        if (layoutLikeArea != null) {
            layoutLikeArea.setOnClickListener(v -> toggleLike());
        }

        if (ivComment != null) {
            ivComment.setOnClickListener(v -> focusCommentInput());
        }

        if (tvCommentCount != null) {
            tvCommentCount.setOnClickListener(v -> focusCommentInput());
        }

        if (layoutCommentArea != null) {
            layoutCommentArea.setOnClickListener(v -> focusCommentInput());
        }

        if (ivBookmark != null) {
            ivBookmark.setOnClickListener(v -> toggleBookmark());
        }

        if (tvBookmark != null) {
            tvBookmark.setOnClickListener(v -> toggleBookmark());
        }

        if (layoutBookmarkArea != null) {
            layoutBookmarkArea.setOnClickListener(v -> toggleBookmark());
        }

        /*
         * GPS 배너 또는 코스 보기 텍스트를 누르면 코스 지도 영역을 펼치거나 접음
         */
        if (layoutGpsBanner != null) {
            layoutGpsBanner.setOnClickListener(v -> toggleCourseMap());
        }

        if (tvCourseToggle != null) {
            tvCourseToggle.setOnClickListener(v -> toggleCourseMap());
        }

        /*
         * 댓글 전송 버튼 클릭 시 댓글 작성 API를 호출함
         */
        if (btnSendComment != null) {
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

                createComment(comment);
            });
        }
    }

    /*
     * 댓글 입력창에 포커스를 주는 함수임
     */
    private void focusCommentInput() {
        etComment.requestFocus();

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /*
     * 댓글 작성 API를 호출하는 함수임
     * 댓글 작성 성공 시 상세 정보를 다시 불러와 댓글 목록을 갱신함
     */
    private void createComment(String comment) {
        if (tipId == null) {
            return;
        }

        tipRepository.createTipComment(
                tipId,
                comment,
                new TipRepository.TipCommentCreateCallback() {
                    @Override
                    public void onSuccess(TipCommentResponse response) {
                        etComment.setText("");

                        Toast.makeText(
                                TipDetailActivity.this,
                                "댓글이 작성되었습니다",
                                Toast.LENGTH_SHORT
                        ).show();

                        /*
                         * 댓글 작성 후 상세 정보를 다시 조회해서
                         * 댓글 목록과 댓글 개수를 최신 상태로 갱신함
                         */
                        loadTipDetail();
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                TipDetailActivity.this,
                                "댓글 작성 실패: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 좋아요 상태를 변경하는 함수임
     * 서버 API 호출 후 응답값으로 UI를 갱신함
     */
    private void toggleLike() {
        if (tipId == null) {
            return;
        }

        tipRepository.toggleTipLike(
                tipId,
                new TipRepository.TipLikeCallback() {
                    @Override
                    public void onSuccess(com.neostride.app.feature.tip.model.TipLikeResponse response) {
                        isLiked = response.isLiked();
                        likeCount = response.getLikeCount();

                        tvLikeCount.setText("좋아요 " + likeCount);
                        setLikeColor(isLiked);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                TipDetailActivity.this,
                                "좋아요 처리 실패: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 좋아요 색상을 변경하는 함수임
     */
    private void setLikeColor(boolean liked) {
        int color = liked ? POINT_COLOR : WHITE_COLOR;

        if (tvLikeCount != null) {
            tvLikeCount.setTextColor(color);
            tvLikeCount.setTypeface(null, Typeface.BOLD);
        }

        if (ivLike != null) {
            ivLike.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    /*
     * 댓글 버튼 색상을 기본값으로 맞추는 함수임
     */
    private void setCommentColor() {
        if (tvCommentCount != null) {
            tvCommentCount.setTextColor(WHITE_COLOR);
            tvCommentCount.setTypeface(null, Typeface.BOLD);
        }

        if (ivComment != null) {
            ivComment.setImageTintList(ColorStateList.valueOf(WHITE_COLOR));
        }
    }

    /*
     * 북마크 상태를 변경하는 함수임
     * 서버 API 호출 후 응답값으로 UI를 갱신함
     */
    private void toggleBookmark() {
        if (tipId == null) {
            return;
        }

        tipRepository.toggleTipBookmark(
                tipId,
                new TipRepository.TipBookmarkCallback() {
                    @Override
                    public void onSuccess(com.neostride.app.feature.tip.model.TipBookmarkResponse response) {
                        isBookmarked = response.isBookmarked();
                        setBookmarkColor(isBookmarked);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(
                                TipDetailActivity.this,
                                "북마크 처리 실패: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    /*
     * 북마크 색상을 변경하는 함수임
     */
    private void setBookmarkColor(boolean bookmarked) {
        int color = bookmarked ? POINT_COLOR : WHITE_COLOR;

        if (ivBookmark != null) {
            ivBookmark.setImageTintList(ColorStateList.valueOf(color));
        }

        if (tvBookmark != null) {
            tvBookmark.setTextColor(color);
            tvBookmark.setTypeface(null, Typeface.BOLD);
        }
    }

    /*
     * 글 점 세 개 메뉴를 보여주는 함수임
     * 본인 글이면 수정/삭제, 남의 글이면 신고/차단 메뉴를 보여줌
     */
    private void showMoreMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvMore);

        if (isMine) {
            popupMenu.getMenu().add("수정");
            popupMenu.getMenu().add("삭제");
        } else {
            popupMenu.getMenu().add("신고");
            popupMenu.getMenu().add("차단");
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String menuTitle = menuItem.getTitle().toString();

            if (menuTitle.equals("수정")) {
                Toast.makeText(this, "수정 기능 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("삭제")) {
                Toast.makeText(this, "삭제 API 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("신고")) {
                Toast.makeText(this, "게시글 신고 기능 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("차단")) {
                Toast.makeText(this, "작성자 차단 기능 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    /*
     * 댓글 점 세 개 메뉴를 보여주는 함수임
     * 본인 댓글이면 수정/삭제, 남의 댓글이면 신고/차단 메뉴를 보여줌
     */
    private void showCommentMoreMenu(View anchorView, TipCommentResponse comment) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        if (comment.isMine()) {
            popupMenu.getMenu().add("수정");
            popupMenu.getMenu().add("삭제");
        } else {
            popupMenu.getMenu().add("신고");
            popupMenu.getMenu().add("차단");
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String menuTitle = menuItem.getTitle().toString();

            if (menuTitle.equals("수정")) {
                Toast.makeText(this, "댓글 수정 API 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("삭제")) {
                Toast.makeText(this, "댓글 삭제 API 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("신고")) {
                Toast.makeText(this, "댓글 신고 기능 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("차단")) {
                Toast.makeText(this, "댓글 작성자 차단 기능 연결 예정", Toast.LENGTH_SHORT).show();
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