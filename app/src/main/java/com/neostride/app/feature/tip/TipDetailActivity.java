package com.neostride.app.feature.tip;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neostride.app.R;

import java.util.ArrayList;

/*
 * 팁 상세 화면을 담당하는 Activity 클래스임
 * 팁 목록에서 선택한 게시글 정보를 받아 상세 내용을 표시함
 */
public class TipDetailActivity extends AppCompatActivity {

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

    private EditText etComment;

    private String nickname;
    private String category;
    private String title;
    private String content;

    private int likeCount;
    private int commentCount;

    private boolean badgeOwner;
    private boolean gpsVisible;
    private boolean isLiked = false;
    private boolean isBookmarked = false;

    private ArrayList<Uri> imageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_detail);

        getIntentData();
        initViews();
        bindData();
        setupClickEvents();
    }

    /*
     * TipAdapter에서 전달한 Intent 데이터를 가져오는 함수임
     */
    private void getIntentData() {
        nickname = getIntent().getStringExtra("nickname");
        category = getIntent().getStringExtra("category");
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");

        likeCount = getIntent().getIntExtra("likeCount", 0);
        commentCount = getIntent().getIntExtra("commentCount", 0);

        badgeOwner = getIntent().getBooleanExtra("badgeOwner", false);
        gpsVisible = getIntent().getBooleanExtra("gpsVisible", false);

        imageUris = getIntent().getParcelableArrayListExtra("imageUris");

        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "알 수 없음";
        }

        if (category == null || category.trim().isEmpty()) {
            category = "전체";
        }

        if (title == null) {
            title = "";
        }

        if (content == null) {
            content = "";
        }

        if (imageUris == null) {
            imageUris = new ArrayList<>();
        }
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

        etComment = findViewById(R.id.et_tip_detail_comment);
    }

    /*
     * 전달받은 팁 데이터를 화면에 표시하는 함수임
     */
    private void bindData() {
        tvNickname.setText(nickname);
        tvTime.setText("· 방금 전");

        tvCategory.setText(category);
        tvTitle.setText(title);
        tvContent.setText(content);

        tvLikeCount.setText("좋아요 " + likeCount);
        tvCommentCount.setText("댓글 " + commentCount);

        ivProfile.setImageResource(R.drawable.ic_profile);
        ivProfile.setImageTintList(null);

        ivBadge.setVisibility(badgeOwner ? View.VISIBLE : View.GONE);
        ivGps.setVisibility(gpsVisible ? View.VISIBLE : View.GONE);

        if (!imageUris.isEmpty()) {
            ivTipImage.setVisibility(View.VISIBLE);
            ivTipImage.setImageURI(imageUris.get(0));
        } else {
            ivTipImage.setVisibility(View.GONE);
        }

        ivBookmark.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        setLikeColor(false);
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
                    "댓글 작성 기능 연결 예정",
                    Toast.LENGTH_SHORT
            ).show();

            etComment.setText("");
        });
    }

    /*
     * 좋아요 상태를 변경하는 함수임
     */
    private void toggleLike() {
        isLiked = !isLiked;

        if (isLiked) {
            likeCount++;
        } else {
            likeCount--;

            if (likeCount < 0) {
                likeCount = 0;
            }
        }

        tvLikeCount.setText("좋아요 " + likeCount);
        setLikeColor(isLiked);
    }

    /*
     * 좋아요 색상을 변경하는 함수임
     */
    private void setLikeColor(boolean liked) {
        int color;

        if (liked) {
            color = Color.parseColor("#B8FF06");
        } else {
            color = Color.WHITE;
        }

        tvLikeCount.setTextColor(color);
        tvLikeCount.setTypeface(null, Typeface.BOLD);
    }

    /*
     * 북마크 상태를 변경하는 함수임
     */
    private void toggleBookmark() {
        isBookmarked = !isBookmarked;

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
     * 점 세 개 메뉴를 보여주는 함수임
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
}