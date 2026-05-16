package com.neostride.app.feature.feed;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.feed.model.FeedBookmarkResponse;
import com.neostride.app.feature.feed.model.FeedCommentRequest;
import com.neostride.app.feature.feed.model.FeedCommentResponse;
import com.neostride.app.feature.feed.model.FeedDetailResponse;
import com.neostride.app.feature.feed.model.FeedLikeResponse;
import com.neostride.app.feature.feed.repository.FeedRepository;
import com.neostride.app.feature.mypage.MyPageActivity;
import com.neostride.app.feature.runnerpage.RunnerPageActivity;

import java.util.ArrayList;
import java.util.List;

/*
 * 피드 상세 화면을 담당하는 Activity 클래스임
 * 피드 목록에서 선택한 피드의 feedId를 받아 상세 API를 호출하고 상세 내용을 표시함
 */
public class FeedDetailActivity extends AppCompatActivity {

    private static final String NEON_COLOR = "#B8FF06";

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

    // 좋아요, 댓글, 북마크 아이콘임
    private ImageView ivLike;
    private ImageView ivComment;
    private ImageView ivBookmark;

    // 좋아요, 댓글, 북마크 버튼 전체 영역임
    private LinearLayout layoutLikeBox;
    private LinearLayout layoutCommentBox;
    private LinearLayout layoutBookmarkBox;

    private TextView tvUsername;
    private TextView tvTime;
    private TextView tvMore;
    private TextView tvTitle;
    private TextView tvContent;

    // 태그 뱃지 TextView임
    private TextView tvTagBadge;

    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private TextView tvBookmark;

    private TextView tvDistance;
    private TextView tvDuration;
    private TextView tvPace;

    private LinearLayout layoutCommentList;
    private TextView tvEmptyComment;

    private EditText etComment;
    private ImageView btnSendComment;

    private FeedRepository feedRepository;

    private boolean isRouteOpen = false;
    private boolean isRecordOpen = false;

    private boolean isLiked = false;
    private boolean isBookmarked = false;

    private Long feedId;
    private Long writerId;
    private boolean isMine = false;

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

    private List<FeedCommentResponse> commentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_detail);

        // 피드 관련 API 호출을 담당하는 Repository를 생성함
        feedRepository = new FeedRepository(this);

        // 목록 화면에서 전달받은 Intent 데이터를 먼저 가져옴
        getIntentData();

        // XML View들을 Java 변수와 연결함
        initViews();

        // Intent로 받은 기존 데이터를 먼저 화면에 표시함
        // 상세 API가 느리거나 실패해도 기본 화면이 보이도록 하기 위함
        bindFeedData();

        // 버튼 클릭 이벤트들을 연결함
        setupClickEvents();

        // feedId를 이용해 상세 API를 호출하고, 성공 시 화면을 다시 갱신함
        loadFeedDetail();
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

        username = getSafeText(username, "알 수 없음");
        time = getSafeText(time, "시간 정보 없음");
        title = getSafeText(title);
        content = getSafeText(content);

        distance = getSafeText(distance, "0.00 km");
        duration = getSafeText(duration, "00:00");
        pace = getSafeText(pace, "0:00/km");

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

        ivLike = findViewById(R.id.iv_detail_like);
        ivComment = findViewById(R.id.iv_detail_comment);
        ivBookmark = findViewById(R.id.iv_detail_bookmark);

        layoutLikeBox = findViewById(R.id.layout_detail_like_box);
        layoutCommentBox = findViewById(R.id.layout_detail_comment_box);
        layoutBookmarkBox = findViewById(R.id.layout_detail_bookmark_box);

        tvUsername = findViewById(R.id.tv_detail_username);
        tvTime = findViewById(R.id.tv_detail_time);
        tvMore = findViewById(R.id.tv_detail_more);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);

        tvTagBadge = findViewById(R.id.tv_detail_tag_badge);

        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_detail_comment_count);
        tvBookmark = findViewById(R.id.tv_detail_bookmark);

        tvDistance = findViewById(R.id.tv_detail_distance);
        tvDuration = findViewById(R.id.tv_detail_duration);
        tvPace = findViewById(R.id.tv_detail_pace);

        layoutCommentList = findViewById(R.id.layout_comment_list);
        tvEmptyComment = findViewById(R.id.tv_empty_comment);

        etComment = findViewById(R.id.et_detail_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
    }

    /*
     * feedId를 이용해 서버 또는 Mock 서버에서 피드 상세 정보를 조회하는 함수임
     */
    private void loadFeedDetail() {
        if (feedId == null) {
            Toast.makeText(
                    this,
                    "피드 ID가 없어 상세 정보를 불러올 수 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        feedRepository.getFeedDetail(
                feedId,
                new FeedRepository.RepositoryCallback<FeedDetailResponse>() {
                    @Override
                    public void onSuccess(FeedDetailResponse response) {
                        applyFeedDetailResponse(response);
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
     * 피드 상세 조회 응답을 Activity 내부 데이터에 반영하고 화면을 다시 그리는 함수임
     */
    private void applyFeedDetailResponse(FeedDetailResponse response) {
        if (response == null) {
            return;
        }

        feedId = response.getFeedId();
        writerId = response.getWriterId();

        username = getSafeText(response.getNickname(), "알 수 없음");
        time = getSafeText(response.getCreatedAt(), "시간 정보 없음");
        title = getSafeText(response.getTitle());
        content = getSafeText(response.getContent());

        tagCount = response.getTaggedCount();
        likeCount = response.getLikeCount();
        commentCount = response.getCommentCount();

        isLiked = response.isLiked();
        isBookmarked = response.isBookmarked();
        isMine = response.isMine();

        displayLikeCount = likeCount;

        distance = getSafeText(response.getDistance(), "0.00 km");
        duration = getSafeText(response.getDuration(), "00:00");
        pace = getSafeText(response.getPace(), "0:00/km");

        mapVisible = response.isMapVisible();
        routeMapImageUri = response.getRouteMapImageUri();

        if (response.getImageUrls() != null) {
            imageUrls = new ArrayList<>(response.getImageUrls());
        } else {
            imageUrls = new ArrayList<>();
        }

        if (response.getComments() != null) {
            commentList = new ArrayList<>(response.getComments());
        } else {
            commentList = new ArrayList<>();
        }

        // 서버 commentCount가 0인데 comments 배열이 내려오는 경우를 대비해 화면 표시용 댓글 수를 보정함
        if (commentCount <= 0 && !commentList.isEmpty()) {
            commentCount = commentList.size();
        }

        bindFeedData();
    }

    /*
     * 피드 데이터를 화면에 표시하는 함수임
     */
    private void bindFeedData() {
        tvUsername.setText(username);
        tvTime.setText("· " + time);
        tvTitle.setText(title);
        tvContent.setText(content);

        // 태그는 액션 버튼에서 빼고 팁 상세 카테고리 위치처럼 상단 뱃지로 표시함
        if (tvTagBadge != null) {
            tvTagBadge.setText(String.valueOf(tagCount));
            tvTagBadge.setVisibility(tagCount > 0 ? View.VISIBLE : View.GONE);
        }

        // 액션 버튼은 팁 상세와 맞춰 좋아요 / 댓글 / 북마크 3개만 표시함
        tvLikeCount.setText("좋아요 " + displayLikeCount);
        tvCommentCount.setText("댓글 " + commentCount);

        tvDistance.setText(distance);
        tvDuration.setText(duration);
        tvPace.setText(pace);

        if (ivProfile != null) {
            ivProfile.setImageTintList(null);
            ivProfile.setImageResource(R.drawable.ic_profile);
        }

        setLikeColor(isLiked);
        setCommentColor();
        setBookmarkColor(isBookmarked);

        if (imageUrls != null && !imageUrls.isEmpty()) {
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

        bindComments(commentList);

        layoutRouteContent.setVisibility(View.GONE);
        layoutRecordContent.setVisibility(View.GONE);

        ivRouteArrow.setRotation(0f);
        ivRecordArrow.setRotation(0f);

        isRouteOpen = false;
        isRecordOpen = false;
    }

    /*
     * 상세 API 응답으로 받은 댓글 목록을 화면에 표시하는 함수임
     */
    private void bindComments(List<FeedCommentResponse> comments) {
        if (layoutCommentList == null || tvEmptyComment == null) {
            return;
        }

        layoutCommentList.removeAllViews();

        if (comments == null || comments.isEmpty()) {
            tvEmptyComment.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyComment.setVisibility(View.GONE);

        for (FeedCommentResponse comment : comments) {
            View commentView = createCommentView(comment);
            layoutCommentList.addView(commentView);
        }
    }

    /*
     * 댓글 1개 View를 코드로 생성하는 함수임
     * 댓글을 각각 독립된 네온 카드처럼 보이게 구성함
     */
    private View createCommentView(FeedCommentResponse comment) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_feed_comment_neon_item);
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
        contentView.setTextColor(Color.parseColor("#DADADA"));
        contentView.setTextSize(13);
        contentView.setLineSpacing(dp(2), 1.0f);

        root.addView(topRow);
        root.addView(contentView);

        return root;
    }

    /*
     * 버튼과 영역 클릭 이벤트를 연결하는 함수임
     */
    private void setupClickEvents() {
        btnBack.setOnClickListener(v -> finish());

        layoutRouteHeader.setOnClickListener(v -> toggleRouteContent());
        layoutRecordHeader.setOnClickListener(v -> toggleRecordContent());

        View.OnClickListener profileClickListener = v -> openWriterProfile();

        if (ivProfile != null) {
            ivProfile.setOnClickListener(profileClickListener);
        }

        if (tvUsername != null) {
            tvUsername.setOnClickListener(profileClickListener);
        }

        if (tvTagBadge != null) {
            tvTagBadge.setOnClickListener(v -> loadTaggedUsers());
        }

        if (layoutLikeBox != null) {
            layoutLikeBox.setOnClickListener(v -> toggleLike());
        }

        if (ivLike != null) {
            ivLike.setOnClickListener(v -> toggleLike());
        }

        if (tvLikeCount != null) {
            tvLikeCount.setOnClickListener(v -> toggleLike());
        }

        if (layoutCommentBox != null) {
            layoutCommentBox.setOnClickListener(v -> focusCommentInput());
        }

        if (ivComment != null) {
            ivComment.setOnClickListener(v -> focusCommentInput());
        }

        if (tvCommentCount != null) {
            tvCommentCount.setOnClickListener(v -> focusCommentInput());
        }

        if (layoutBookmarkBox != null) {
            layoutBookmarkBox.setOnClickListener(v -> toggleBookmark());
        }

        if (ivBookmark != null) {
            ivBookmark.setOnClickListener(v -> toggleBookmark());
        }

        if (tvBookmark != null) {
            tvBookmark.setOnClickListener(v -> toggleBookmark());
        }

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

            createFeedComment(comment);
        });
    }

    /*
     * 댓글 입력창으로 포커스를 이동하고 키보드를 여는 함수임
     */
    private void focusCommentInput() {
        if (etComment == null) {
            return;
        }

        etComment.requestFocus();

        etComment.postDelayed(() -> {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(
                        etComment,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        }, 120);
    }

    /*
     * 피드 댓글 작성 API를 호출하는 함수임
     * 성공 시 새 댓글을 댓글 목록에 추가하고 화면을 다시 갱신함
     */
    private void createFeedComment(String content) {
        if (feedId == null) {
            Toast.makeText(
                    this,
                    "피드 ID가 없어 댓글을 작성할 수 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FeedCommentRequest request = new FeedCommentRequest(content);

        feedRepository.createFeedComment(
                feedId,
                request,
                new FeedRepository.RepositoryCallback<FeedCommentResponse>() {
                    @Override
                    public void onSuccess(FeedCommentResponse data) {
                        if (data == null) {
                            Toast.makeText(
                                    FeedDetailActivity.this,
                                    "댓글 작성 응답이 비어 있습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        // 새 댓글을 기존 댓글 목록 맨 위에 추가함
                        commentList.add(0, data);

                        // 댓글 수를 1 증가시킴
                        commentCount++;

                        // 댓글 수 TextView를 갱신함
                        tvCommentCount.setText("댓글 " + commentCount);

                        // 댓글 버튼 색상을 기본 흰색으로 유지함
                        setCommentColor();

                        // 댓글 목록을 다시 그림
                        bindComments(commentList);

                        // 입력창을 비움
                        etComment.setText("");

                        Toast.makeText(
                                FeedDetailActivity.this,
                                "댓글이 작성되었습니다",
                                Toast.LENGTH_SHORT
                        ).show();
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
     * 태그된 사용자 목록을 Repository에서 가져오는 함수임
     */
    private void loadTaggedUsers() {
        if (feedId == null) {
            Toast.makeText(
                    this,
                    "피드 ID가 없어 태그 정보를 불러올 수 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

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
     * 피드 작성자의 프로필로 이동하는 함수임
     * 내 글이면 마이페이지로 이동하고, 남의 글이면 러너페이지로 이동함
     */
    private void openWriterProfile() {
        int myId = TokenManager.getUserId(this);

        if (writerId == null || isMine || writerId == myId) {
            Intent intent = new Intent(this, MyPageActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, RunnerPageActivity.class);
        intent.putExtra("user_id", writerId.intValue());
        intent.putExtra("nickname", username);
        startActivity(intent);
    }

    /*
     * 피드 좋아요 상태를 변경하는 함수임
     * Repository를 통해 좋아요 토글 API를 호출하고 서버 응답값으로 화면을 갱신함
     */
    private void toggleLike() {
        if (feedId == null) {
            Toast.makeText(
                    this,
                    "피드 ID가 없어 좋아요를 처리할 수 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        feedRepository.toggleFeedLike(
                feedId,
                new FeedRepository.RepositoryCallback<FeedLikeResponse>() {
                    @Override
                    public void onSuccess(FeedLikeResponse data) {
                        if (data == null) {
                            Toast.makeText(
                                    FeedDetailActivity.this,
                                    "좋아요 응답이 비어 있습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        isLiked = data.isLiked();
                        likeCount = data.getLikeCount();
                        displayLikeCount = data.getLikeCount();

                        tvLikeCount.setText("좋아요 " + displayLikeCount);
                        setLikeColor(isLiked);
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
     * 좋아요 버튼 색상을 변경하는 함수임
     */
    private void setLikeColor(boolean liked) {
        int color = liked ? Color.parseColor(NEON_COLOR) : Color.WHITE;

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
            tvCommentCount.setTextColor(Color.WHITE);
            tvCommentCount.setTypeface(null, Typeface.BOLD);
        }

        if (ivComment != null) {
            ivComment.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    /*
     * 피드 북마크 상태를 변경하는 함수임
     * Repository를 통해 북마크 토글 API를 호출하고 서버 응답값으로 화면을 갱신함
     */
    private void toggleBookmark() {
        if (feedId == null) {
            Toast.makeText(
                    this,
                    "피드 ID가 없어 북마크를 처리할 수 없습니다",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        feedRepository.toggleFeedBookmark(
                feedId,
                new FeedRepository.RepositoryCallback<FeedBookmarkResponse>() {
                    @Override
                    public void onSuccess(FeedBookmarkResponse data) {
                        if (data == null) {
                            Toast.makeText(
                                    FeedDetailActivity.this,
                                    "북마크 응답이 비어 있습니다",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        isBookmarked = data.isBookmarked();
                        setBookmarkColor(isBookmarked);
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
     * 북마크 상태에 따라 아이콘과 글자 색상을 변경하는 함수임
     * bookmarked가 true이면 꽉 찬 북마크 아이콘을 사용하고,
     * false이면 빈 북마크 아이콘을 사용함
     */
    private void setBookmarkColor(boolean bookmarked) {
        int color = bookmarked ? Color.parseColor(NEON_COLOR) : Color.WHITE;

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
    }

    /*
     * 태그된 사람 목록을 커스텀 Dialog로 보여주는 함수임
     * 태그 목록 데이터는 Repository에서 전달받은 값을 사용함
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
            moveText.setTextColor(Color.parseColor(NEON_COLOR));
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

                Toast.makeText(
                        FeedDetailActivity.this,
                        taggedUser + " 프로필 이동은 사용자 ID 연결 후 구현 예정",
                        Toast.LENGTH_SHORT
                ).show();
            });

            rootLayout.addView(userRow);
        }

        TextView closeButton = new TextView(this);
        closeButton.setText("닫기");
        closeButton.setTextColor(Color.parseColor(NEON_COLOR));
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
                Toast.makeText(this, "삭제 기능 연결 예정", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (menuTitle.equals("신고")) {
                Toast.makeText(this, "신고 기능 연결 예정", Toast.LENGTH_SHORT).show();
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
    private void showCommentMoreMenu(View anchorView, FeedCommentResponse comment) {
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
     * null 값을 빈 문자열로 바꿔주는 함수임
     */
    private String getSafeText(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    /*
     * null 또는 빈 문자열일 때 기본값을 넣어주는 함수임
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
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }
}