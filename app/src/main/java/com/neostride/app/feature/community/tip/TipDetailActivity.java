package com.neostride.app.feature.community.tip;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.friend.api.FriendApi;
import com.neostride.app.feature.community.friend.model.FriendRequest;
import com.neostride.app.feature.community.friend.repository.FriendRepository;
import com.neostride.app.feature.community.tip.model.TipBookmarkResponse;
import com.neostride.app.feature.community.tip.model.TipLikeResponse;
import com.neostride.app.feature.community.mypage.MyPageActivity;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;
import com.neostride.app.feature.community.tip.model.TipCommentResponse;
import com.neostride.app.feature.community.tip.model.TipDetailResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

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

    // 댓글 편집 중인지 추적 — null이면 새 댓글 작성 모드
    private Long editingCommentId = null;

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

    // 코스 펼치기/접기 화살표 아이콘임
    private ImageView tvCourseToggle;

    // 댓글 입력창임
    private EditText etComment;

    // 댓글 목록을 동적으로 추가할 레이아웃임
    private LinearLayout layoutComments;
    private android.widget.ScrollView scrollTipDetail;
    private boolean scrollToBottomAfterBind = false;

    // GPS 코스 카드 전체 (gpsVisible에 따라 GONE/VISIBLE)
    private View cardTipGps;

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

    // 코스 주소 레이아웃·텍스트뷰임
    private LinearLayout layoutCourseAddress;
    private TextView tvCourseAddress;

    // 팁 관련 API 호출을 담당하는 Repository임
    private TipRepository tipRepository;

    // 현재 상세 화면의 팁 ID임
    private Long tipId;

    // 팁 작성자 ID임
    private Long writerId;

    // 팁 작성자 닉네임임
    private String nickname = "";

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

        // 키보드(IME) 높이만큼 루트에 bottom padding 추가 → 댓글 입력창이 키보드 위로 따라 올라옴
        // 입력창에 이미 marginBottom=60dp(네비바 보정)이 있어서, 그만큼 빼야 키보드 바로 위에 정확히 붙음
        View rootForIme = findViewById(R.id.layout_tip_detail_root);
        if (rootForIme != null) {
            int existingMarginPx = (int) (60 * getResources().getDisplayMetrics().density);
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootForIme, (v, insets) -> {
                androidx.core.graphics.Insets imeInsets =
                        insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
                int padBottom = imeInsets.bottom > existingMarginPx
                        ? imeInsets.bottom - existingMarginPx
                        : 0;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), padBottom);
                return insets;
            });
        }

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

        cardTipGps = findViewById(R.id.card_tip_gps);
        layoutComments = findViewById(R.id.layout_tip_detail_comments);
        scrollTipDetail = findViewById(R.id.scroll_tip_detail);
        layoutGpsBanner = findViewById(R.id.layout_tip_detail_gps_banner);
        layoutCourseMap = findViewById(R.id.layout_tip_detail_course_map);

        layoutLikeArea = findViewById(R.id.layout_tip_detail_like_area);
        layoutCommentArea = findViewById(R.id.layout_tip_detail_comment_area);
        layoutBookmarkArea = findViewById(R.id.layout_tip_detail_bookmark_area);

        ivCourseMap = findViewById(R.id.iv_tip_detail_course_map);
        layoutCourseAddress = findViewById(R.id.layout_tip_detail_course_address);
        tvCourseAddress = findViewById(R.id.tv_tip_detail_course_address);

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

        // 작성자 정보를 저장함
        writerId = response.getWriterId();
        nickname = getSafeText(response.getNickname(), "알 수 없음");

        likeCount = response.getLikeCount();
        commentCount = response.getCommentCount();

        isLiked = response.isLiked();
        isBookmarked = response.isBookmarked();
        isMine = response.isMine();

        tvNickname.setText(nickname);
        tvTime.setText(formatDate(response.getCreatedAt()));

        /*
         * 카테고리 알림판은 배경을 채우지 않고
         * 글자색과 윤곽선만 카테고리별 네온색으로 변경함
         */
        applyCategoryOutlineStyle(tvCategory, response.getCategory());

        tvTitle.setText(getSafeText(response.getTitle(), ""));
        tvContent.setText(getSafeText(response.getContent(), ""));

        // 피드 상세와 통일하기 위해 글자 + 숫자 형식으로 표시함
        tvLikeCount.setText("좋아요 " + likeCount);
        tvCommentCount.setText("댓글 " + commentCount);

        // 작성자 프로필 이미지: 원형 + Glide circleCrop
        ivProfile.setImageTintList(null);
        String profileImageUrl = response.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }

        // 작성자 배지: 보유 시 등급별 색상으로 표시
        com.neostride.app.feature.badge.model.BadgeTier tier =
                com.neostride.app.feature.badge.model.BadgeTier.fromString(response.getBadgeType());
        if (!response.isBadgeOwned() || tier.isNone()) {
            ivBadge.setVisibility(View.GONE);
        } else {
            ivBadge.setVisibility(View.VISIBLE);
            ivBadge.setColorFilter(tier.getColor());
        }

        /*
         * GPS 코스 팁이면 GPS 카드 전체를 표시함
         */
        if (cardTipGps != null) {
            cardTipGps.setVisibility(response.isGpsVisible() ? View.VISIBLE : View.GONE);
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
            tvCourseToggle.setRotation(0f); // 접힌 상태 = 아래 화살표
        }

        bindCourseMapImage();
        bindCourseAddress(response.getCourseAddress());

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
                        .centerCrop()
                        .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                        .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
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
            ivCourseMap.setVisibility(View.GONE);
            return;
        }

        Glide.with(this)
                .load(courseMapImageUrl)
                .centerCrop()
                .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                .error(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
                .into(ivCourseMap);
    }

    /*
     * 코스 주소를 지도 이미지 아래에 표시함
     * 주소가 없으면 주소 레이아웃을 숨김
     */
    private void bindCourseAddress(String address) {
        if (layoutCourseAddress == null || tvCourseAddress == null) return;
        if (address == null || address.trim().isEmpty()) {
            layoutCourseAddress.setVisibility(View.GONE);
            return;
        }
        tvCourseAddress.setText(address);
        layoutCourseAddress.setVisibility(View.VISIBLE);
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
            // 펼침 = 위 화살표(180°), 접힘 = 아래 화살표(0°)
            tvCourseToggle.animate()
                    .rotation(isCourseExpanded ? 180f : 0f)
                    .setDuration(200)
                    .start();
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

        if (scrollToBottomAfterBind && scrollTipDetail != null) {
            scrollToBottomAfterBind = false;
            scrollTipDetail.post(() -> scrollTipDetail.fullScroll(android.widget.ScrollView.FOCUS_DOWN));
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
        profile.setImageTintList(null);
        profile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profile.setBackground(null);

        // 댓글 작성자 프로필 — 원형 처리
        String commentProfileUrl = comment.getProfileImageUrl();
        if (commentProfileUrl != null && !commentProfileUrl.trim().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(commentProfileUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profile);
        } else {
            profile.setImageResource(R.drawable.ic_profile);
        }

        // 닉네임 — 왼쪽
        TextView nameView = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(dp(8), 0, 0, 0);
        nameView.setLayoutParams(nameParams);
        nameView.setText(getSafeText(comment.getNickname(), "알 수 없음"));
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(13);
        nameView.setTypeface(null, Typeface.BOLD);

        // 배지 — 닉네임 바로 오른쪽
        ImageView badgeView = new ImageView(this);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        badgeParams.setMargins(dp(5), 0, 0, 0);
        badgeView.setLayoutParams(badgeParams);
        badgeView.setImageResource(R.drawable.ic_badge);
        com.neostride.app.feature.badge.model.BadgeTier commentTier =
                com.neostride.app.feature.badge.model.BadgeTier.fromString(comment.getBadgeType());
        if (comment.isBadgeOwned() && !commentTier.isNone()) {
            badgeView.setColorFilter(commentTier.getColor());
            badgeView.setVisibility(View.VISIBLE);
        } else {
            badgeView.setVisibility(View.GONE);
        }

        // Spacer — 시간을 오른쪽으로 밀어냄
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));

        // 시간 — 오른쪽 (••• 바로 왼쪽)
        TextView timeView = new TextView(this);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(0, 0, dp(6), 0);
        timeView.setLayoutParams(timeParams);
        timeView.setText(formatDate(comment.getCreatedAt()));
        timeView.setTextColor(Color.parseColor("#A0A0A0"));
        timeView.setTextSize(12);
        timeView.setTypeface(null, Typeface.BOLD);

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

        // 프로필·닉네임·배지 클릭 — 내 댓글이면 무반응, 남의 댓글이면 러너페이지
        int myId = TokenManager.getUserId(this);
        Long commentWriterId = comment.getWriterId();
        View.OnClickListener commentProfileClick = (commentWriterId != null && commentWriterId != myId)
                ? v -> {
                    Intent intent = new Intent(this, RunnerPageActivity.class);
                    intent.putExtra("user_id", commentWriterId.intValue());
                    intent.putExtra("nickname", comment.getNickname());
                    startActivity(intent);
                }
                : null;

        profile.setOnClickListener(commentProfileClick);
        nameView.setOnClickListener(commentProfileClick);
        badgeView.setOnClickListener(commentProfileClick);

        topRow.addView(profile);
        topRow.addView(nameView);
        topRow.addView(badgeView);
        topRow.addView(spacer);
        topRow.addView(timeView);
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

        /*
         * 작성자 프로필 이미지 또는 닉네임 클릭 시
         * 내 글이면 마이페이지, 남의 글이면 러너페이지로 이동함
         */
        View.OnClickListener profileClickListener = v -> openWriterProfile();

        if (ivProfile != null) {
            ivProfile.setOnClickListener(profileClickListener);
        }

        if (tvNickname != null) {
            tvNickname.setOnClickListener(profileClickListener);
        }

        if (ivBadge != null) {
            ivBadge.setOnClickListener(profileClickListener);
        }

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
                    Toast.makeText(this, "댓글을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (editingCommentId != null) {
                    updateTipComment(editingCommentId, comment);
                } else {
                    createComment(comment);
                }
            });
        }
    }

    /*
     * 댓글 수정 모드 진입 — 입력창에 기존 댓글 채우고 키보드 강제로 띄움
     */
    private void enterCommentEditMode(TipCommentResponse comment) {
        editingCommentId = comment.getCommentId();
        etComment.setText(comment.getContent() != null ? comment.getContent() : "");
        etComment.setSelection(etComment.getText().length());
        forceShowKeyboardOnComment();
    }

    /*
     * 키보드를 강제로 띄움 — popup 닫힌 직후에도 안정적으로 동작
     */
    private void forceShowKeyboardOnComment() {
        if (etComment == null) return;
        etComment.requestFocus();
        etComment.postDelayed(() -> {
            if (etComment == null) return;
            etComment.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etComment, InputMethodManager.SHOW_FORCED);
            }
        }, 200);
    }

    /*
     * 댓글 수정 API 호출
     */
    private void updateTipComment(Long commentId, String content) {
        if (tipId == null) return;
        tipRepository.updateTipComment(tipId, commentId, content,
                new TipRepository.TipCommentCreateCallback() {
                    @Override
                    public void onSuccess(TipCommentResponse response) {
                        Toast.makeText(TipDetailActivity.this, "댓글을 수정했습니다", Toast.LENGTH_SHORT).show();
                        editingCommentId = null;
                        etComment.setText("");
                        loadTipDetail();  // 댓글 목록 갱신
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(TipDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * 댓글 삭제 확인 다이얼로그 → API
     */
    private void confirmAndDeleteComment(Long commentId) {
        if (tipId == null || commentId == null) return;
        DangerConfirmDialog.show(
                this,
                "댓글 삭제",
                "이 댓글을 정말 삭제하시겠습니까?",
                "삭제",
                () -> tipRepository.deleteTipComment(tipId, commentId,
                        new TipRepository.TipDeleteCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(TipDetailActivity.this, "댓글을 삭제했습니다", Toast.LENGTH_SHORT).show();
                                loadTipDetail();
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(TipDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        })
        );
    }

    /*
     * 팁 작성자의 프로필로 이동하는 함수임
     * 내 글이면 마이페이지로 이동하고, 남의 글이면 러너페이지로 이동함
     */
    private void openWriterProfile() {
        int myId = TokenManager.getUserId(this);

        if (writerId == null || isMine || writerId == myId) {
            Intent intent = new Intent(this, MyPageActivity.class);
            intent.putExtra("username", nickname);
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, RunnerPageActivity.class);
        intent.putExtra("user_id", writerId.intValue());
        intent.putExtra("nickname", nickname);
        startActivity(intent);
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
                        scrollToBottomAfterBind = true;
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
     * 즉시 UI를 업데이트하고 백그라운드에서 서버 동기화함
     */
    private void toggleLike() {
        if (tipId == null) {
            return;
        }

        // 즉시 로컬 상태 + UI 업데이트
        boolean prevLiked = isLiked;
        int prevCount = likeCount;
        isLiked = !isLiked;
        likeCount = Math.max(0, likeCount + (isLiked ? 1 : -1));
        tvLikeCount.setText("좋아요 " + likeCount);
        setLikeColor(isLiked);

        // 서버 동기화 — 성공 시 UI는 낙관적 업데이트 유지, 실패 시만 롤백
        tipRepository.toggleTipLike(
                tipId,
                new TipRepository.TipLikeCallback() {
                    @Override
                    public void onSuccess(TipLikeResponse response) {
                        // UI는 이미 올바르게 업데이트됨 — 서버 응답 likeCount가 부정확할 수 있어 덮어쓰지 않음
                        isLiked = response.isLiked();
                    }

                    @Override
                    public void onFailure(String message) {
                        // 롤백
                        isLiked = prevLiked;
                        likeCount = prevCount;
                        tvLikeCount.setText("좋아요 " + likeCount);
                        setLikeColor(isLiked);
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

        // 버튼 테두리도 활성화 상태로 — drawable selector의 state_activated 색상 적용
        if (layoutLikeArea != null) {
            layoutLikeArea.setActivated(liked);
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
                    public void onSuccess(TipBookmarkResponse response) {
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
     * 북마크 상태에 따라 아이콘과 글자 색상을 변경하는 함수임
     * bookmarked가 true이면 꽉 찬 북마크 아이콘을 사용하고,
     * false이면 빈 북마크 아이콘을 사용함
     */
    private void setBookmarkColor(boolean bookmarked) {
        int color = bookmarked ? POINT_COLOR : WHITE_COLOR;

        if (ivBookmark != null) {
            ivBookmark.setImageResource(
                    bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark
            );
            ivBookmark.setImageTintList(ColorStateList.valueOf(color));
        }

        if (tvBookmark != null) {
            tvBookmark.setTextColor(color);
            tvBookmark.setTypeface(null, Typeface.BOLD);
        }

        // 버튼 테두리도 활성화 상태로
        if (layoutBookmarkArea != null) {
            layoutBookmarkArea.setActivated(bookmarked);
        }
    }

    /*
     * 글 점 세 개 메뉴를 보여주는 함수임
     * 본인 글이면 수정/삭제, 남의 글이면 신고/차단 메뉴를 보여줌
     */
    private void showMoreMenu() {
        if (isMine) {
            showEditDeletePopup(tvMore,
                    () -> launchTipEdit(),
                    () -> confirmAndDeleteTip());
        } else {
            showReportBlockPopup(tvMore,
                    "게시글 신고 기능 연결 예정",
                    () -> confirmAndBlockWriter());
        }
    }

    /*
     * 팁 수정 — TipUploadActivity edit 모드로 실행
     */
    private void launchTipEdit() {
        if (tipId == null) return;
        Intent intent = new Intent(this, TipUploadActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("tipId", tipId.longValue());
        startActivity(intent);
    }

    /*
     * 팁 삭제 확인 다이얼로그 → API 호출 → 액티비티 종료
     */
    private void confirmAndDeleteTip() {
        if (tipId == null) return;
        DangerConfirmDialog.show(
                this,
                "팁 삭제",
                "정말 이 팁을 삭제하시겠습니까?",
                "삭제",
                () -> tipRepository.deleteTip(tipId, new TipRepository.TipDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(TipDetailActivity.this, "팁을 삭제했습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(TipDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    /*
     * 작성자 차단 — 러너페이지와 동일 스타일 다이얼로그
     */
    private void confirmAndBlockWriter() {
        if (writerId == null) return;
        String name = nickname != null ? nickname : "이 작성자";
        DangerConfirmDialog.show(
                this,
                "차단하기",
                "차단하면 상대방의 글과 댓글이 나에게 보이지 않으며,\n상대방 글에 남긴 좋아요·북마크·댓글은 삭제됩니다.\n정말 " + name + "님을 차단하시겠습니까?",
                "차단",
                () -> {
                    FriendRepository friendRepo =
                            new FriendRepository(
                                    com.neostride.app.common.network.ApiClient.getInstance()
                                            .create(FriendApi.class));
                    FriendRequest req =
                            new FriendRequest(
                                    writerId.intValue(), "block");
                    friendRepo.updateStatus(req, success -> runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(TipDetailActivity.this, name + "님을 차단했습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(TipDetailActivity.this, "차단에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }));
                }
        );
    }

    /*
     * 본인 글일 때 ··· 드롭다운 — Runnable 콜백
     */
    private void showEditDeletePopup(View anchor, Runnable onEdit, Runnable onDelete) {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_owner_more_options, null);
        int width = (int) (160 * getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(25);
        popup.showAsDropDown(anchor, -width + anchor.getWidth(), 8);

        menuView.findViewById(R.id.menu_edit).setOnClickListener(v -> {
            popup.dismiss();
            if (onEdit != null) onEdit.run();
        });
        menuView.findViewById(R.id.menu_delete).setOnClickListener(v -> {
            popup.dismiss();
            if (onDelete != null) onDelete.run();
        });
    }

    /*
     * 러너페이지 스타일의 신고/차단 드롭다운 — 신고 토스트 유지, 차단은 Runnable 콜백
     */
    private void showReportBlockPopup(View anchor, String reportToast, Runnable onBlock) {
        View menuView = LayoutInflater.from(this).inflate(R.layout.layout_runner_more_options, null);
        int width = (int) (160 * getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(menuView, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(25);
        popup.showAsDropDown(anchor, -width + anchor.getWidth(), 8);

        menuView.findViewById(R.id.menu_block).setOnClickListener(v -> {
            popup.dismiss();
            if (onBlock != null) onBlock.run();
        });
        menuView.findViewById(R.id.menu_report).setOnClickListener(v -> {
            popup.dismiss();
            Toast.makeText(this, reportToast, Toast.LENGTH_SHORT).show();
        });
    }

    /*
     * 댓글 점 세 개 메뉴를 보여주는 함수임
     * 본인 댓글이면 수정/삭제, 남의 댓글이면 신고/차단 메뉴를 보여줌
     */
    private void showCommentMoreMenu(View anchorView, TipCommentResponse comment) {
        if (comment.isMine()) {
            showEditDeletePopup(anchorView,
                    () -> enterCommentEditMode(comment),
                    () -> confirmAndDeleteComment(comment.getCommentId()));
        } else {
            Long commentWriterId = comment.getWriterId();
            showReportBlockPopup(anchorView, "댓글 신고 기능 연결 예정", () -> {
                if (commentWriterId != null) {
                    confirmAndBlockUser(commentWriterId.intValue(), "댓글 작성자");
                }
            });
        }
    }

    /*
     * 임의 userId 차단 (댓글 작성자 차단용)
     */
    private void confirmAndBlockUser(int targetUserId, String label) {
        DangerConfirmDialog.show(
                this,
                "차단하기",
                "차단하면 상대방의 글과 댓글이 나에게 보이지 않으며,\n상대방 글에 남긴 좋아요·북마크·댓글은 삭제됩니다.\n정말 이 " + label + "을 차단하시겠습니까?",
                "차단",
                () -> {
                    FriendRepository friendRepo =
                            new FriendRepository(
                                    com.neostride.app.common.network.ApiClient.getInstance()
                                            .create(FriendApi.class));
                    FriendRequest req =
                            new FriendRequest(targetUserId, "block");
                    friendRepo.updateStatus(req, success -> runOnUiThread(() ->
                            Toast.makeText(TipDetailActivity.this,
                                    success ? label + "을 차단했습니다." : "차단에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show()));
                }
        );
    }

    /*
     * 팁 상세 카테고리 알림판 스타일을 적용하는 함수임
     * 배경은 투명하게 유지하고 글자색과 윤곽선만 카테고리별 네온색으로 변경함
     */
    private void applyCategoryOutlineStyle(TextView categoryView, String category) {
        if (categoryView == null) {
            return;
        }

        int categoryColor = Color.parseColor(getCategoryColorCode(category));

        categoryView.setText(convertCategoryToKorean(category));
        categoryView.setTextColor(categoryColor);
        categoryView.setTypeface(null, Typeface.BOLD);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);

        // 배경을 채우지 않고 투명하게 유지함
        drawable.setColor(Color.TRANSPARENT);

        // 글자와 같은 색으로 윤곽선만 표시함
        drawable.setStroke(dp(1), categoryColor);

        // 사진처럼 둥근 알림판 형태로 표시함
        drawable.setCornerRadius(dp(14));

        categoryView.setBackground(drawable);
    }

    /*
     * 카테고리별 네온 색상 코드를 반환하는 함수임
     */
    private String getCategoryColorCode(String category) {
        String koreanCategory = convertCategoryToKorean(category);

        switch (koreanCategory) {
            case "전체":
                return "#CCFF00";

            case "자유":
                return "#00E5FF";

            case "훈련":
                return "#FF3DFF";

            case "코스":
                return "#FFB300";

            case "장비":
                return "#00FF85";

            default:
                return "#CCFF00";
        }
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
     * ISO 시간 문자열을 화면 표시용으로 변환 (오늘 내: 상대 시간, 이전: 절대 날짜)
     */
    private String formatDate(String isoTime) {
        return TimeFormatter.format(isoTime);
    }

    /*
     * dp 값을 px 값으로 변환하는 함수임
     */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}