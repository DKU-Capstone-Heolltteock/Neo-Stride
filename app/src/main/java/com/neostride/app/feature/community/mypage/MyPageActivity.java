package com.neostride.app.feature.community.mypage;

import android.content.Intent;
import com.bumptech.glide.Glide;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow; // PopupWindow 임포트 확인
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.friend.FriendActivity;
import com.neostride.app.feature.community.runnerpage.RunnerPageActivity;

import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.badge.repository.BadgeRepository;
import com.neostride.app.feature.community.mypage.model.CommunityContentResponse;
import com.neostride.app.feature.community.mypage.model.UserProfileResponse;
import com.neostride.app.feature.community.mypage.repository.MyPageRepository;
import com.neostride.app.feature.community.tip.model.TipResponse;
import com.neostride.app.feature.community.tip.repository.TipRepository;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//  마이페이지 Activity
//  <p>
//  - 프로필(닉네임·배지·상태메시지·친구 수) 표시 및 수정
//  - 내 피드 / 태그된 피드 / 활동(댓글·좋아요·북마크) 탭 전환
//  - 프로필 사진 변경 (카메라/갤러리/기본 이미지), 서버 Multipart 업로드

public class MyPageActivity extends AppCompatActivity {

    // ── UI 뷰 ──
    private ImageButton btnBack;
    private LinearLayout layoutStatus;
    private TabLayout tabLayout;
    private RecyclerView rvMyFeeds;
    private TextView tvUsername, tvFriends, tvStatusMessage, tvEmptyState;
    private ImageView ivProfile, ivBadge;

    // ── 필터 상태 ──
    private String currentPostFilter = "all"; // "all" | "feed" | "tip"
    private String currentActivityType = null; // "comments" | "likes" | "bookmarks"

    // ── 레포지터리 ──
    private MyPageRepository repository;
    private BadgeRepository badgeRepository;

    // ── 상태 ──
    private UserProfileResponse cachedUserData;
    private boolean isMenuItemSelected = false;
    private int lastRealTabPosition = 0;  // 0(내 피드) 또는 1(태그 피드)
    private boolean isRevertingTab = false; // 탭 복원 중 무한 루프 방지 플래그

    // 카메라 실행 런처
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    Glide.with(MyPageActivity.this).load(bitmap).circleCrop().into(ivProfile);
                    uploadProfileImageBitmap(bitmap);
                }
            }
    );

    // 카메라 권한 요청 런처
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // 이제 위에서 cameraLauncher가 이미 정의되었으므로 에러가 나지 않습니다!
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // [갤러리] 런처
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    Glide.with(MyPageActivity.this).load(imageUri).circleCrop().into(ivProfile);
                    uploadProfileImageUri(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_page);

        repository = new MyPageRepository();
        badgeRepository = new BadgeRepository(ApiClient.getInstance().create(BadgeService.class));

        initViews();
        fetchData();

        updateActivityTabTitle("내 활동");

        btnBack.setOnClickListener(v -> finish());
        layoutStatus.setOnClickListener(v -> {
            showEditStatusDialog(); //상메 부분 누르면 팝업 뜨도록 리스너 설정
        });

        setupRecyclerView();
        setupTabLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // AccountActivity에서 프로필 사진/닉네임을 변경하고 돌아올 때 최신 정보 반영
        if (repository != null) {
            repository.getUserProfile(new Callback<UserProfileResponse>() {
                @Override
                public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        updateUI(response.body());
                    }
                }
                @Override
                public void onFailure(Call<UserProfileResponse> call, Throwable t) {}
            });
        }

        // 상세 페이지에서 좋아요/댓글/북마크 후 뒤로가기 시 게시물 목록 갱신
        if (tabLayout != null) {
            int selectedTab = tabLayout.getSelectedTabPosition();
            if (selectedTab == 0) {
                // 내가 쓴 글 탭
                applyFilter(currentPostFilter);
            } else if (selectedTab == 1) {
                // 나를 태그한 피드 탭
                loadFeeds("tagged");
            } else if (selectedTab == 2 && isMenuItemSelected && currentActivityType != null) {
                // 내 활동 탭 (댓글/좋아요/북마크 중 선택된 항목)
                loadFeeds(currentActivityType);
            }
        }
    }

    // ─── 피드 타입(tagged/comments/likes/bookmarks)에 맞는 API를 호출하여 RecyclerView에 표시 ───
    private void loadFeeds(String type) {
        rvMyFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        // "tagged"는 팁 미지원 → 기존 피드 전용 어댑터 유지
        if ("tagged".equals(type)) {
            final String emptyMsg = "아직 나를 태그한 피드가 없어요\n함께 달린 친구에게 태그를 요청해보세요";
            repository.getTaggedFeeds(new Callback<List<CommunityContentResponse>>() {
                @Override
                public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        MyFeedAdapter adapter = new MyFeedAdapter(response.body());
                        adapter.setOnProfileClickListener((userId, nickname) -> {
                            int myId = TokenManager.getUserId(MyPageActivity.this);
                            if (userId == myId) return;
                            Intent intent = new Intent(MyPageActivity.this, RunnerPageActivity.class);
                            intent.putExtra("user_id", userId);
                            intent.putExtra("nickname", nickname);
                            startActivity(intent);
                        });
                        rvMyFeeds.setAdapter(adapter);
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    } else {
                        if (tvEmptyState != null) {
                            tvEmptyState.setText(emptyMsg);
                            tvEmptyState.setVisibility(View.VISIBLE);
                        }
                    }
                }
                @Override
                public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                    Log.e("API_ERROR", "tagged 로드 실패: " + t.getMessage());
                    if (tvEmptyState != null) {
                        tvEmptyState.setText(emptyMsg);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                }
            });
            return;
        }

        // "comments" / "likes" / "bookmarks": 피드 + 팁 병렬 조회 후 통합 표시
        loadFeedsAndTips(type);
    }

    // ─── 활동 탭(댓글/좋아요/북마크): 피드와 팁을 병렬 조회하여 MyPostsAdapter로 통합 표시 ───
    private void loadFeedsAndTips(String type) {
        final String emptyMsg;
        switch (type) {
            case "comments":  emptyMsg = "댓글을 단 게시글이 없어요\n다른 러너의 피드나 팁에 응원 한마디 남겨보세요"; break;
            case "likes":     emptyMsg = "아직 좋아요한 게시글이 없어요\n마음에 드는 피드나 팁에 좋아요를 눌러보세요"; break;
            case "bookmarks": emptyMsg = "아직 북마크한 게시글이 없어요\n나중에 다시 보고 싶은 피드나 팁을 저장해보세요"; break;
            default:          emptyMsg = "게시글이 없어요"; break;
        }

        final List<MyPostsAdapter.PostItem> combined = new ArrayList<>();
        final int[] pending = {2};

        Runnable onAllDone = () -> runOnUiThread(() -> {
            if (combined.isEmpty()) {
                if (tvEmptyState != null) {
                    tvEmptyState.setText(emptyMsg);
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                // 북마크 탭: 서버가 is_bookmarked를 반환 못하는 경우를 보완 → 피드 전체 true 강제
                if ("bookmarks".equals(type)) {
                    for (MyPostsAdapter.PostItem pi : combined) {
                        if (pi.type == MyPostsAdapter.TYPE_FEED && pi.feed != null)
                            pi.feed.isBookmarked = true;
                        // 팁은 bookmarked 필드를 직접 반환하므로 그대로 사용
                    }
                }
                MyPostsAdapter adapter = new MyPostsAdapter(MyPageActivity.this, combined);
                if ("bookmarks".equals(type)) {
                    adapter.setRemoveOnUnbookmark(true);
                    adapter.setOnBookmarkRemovedListener(MyPageActivity.this::onBookmarkRemovedFromList);
                }
                rvMyFeeds.setAdapter(adapter);
                if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            }
        });

        // API 1: 피드 목록
        Callback<List<CommunityContentResponse>> feedCallback = new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (CommunityContentResponse f : response.body())
                        combined.add(new MyPostsAdapter.PostItem(f));
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", type + " 피드 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        };

        // API 2: 팁 목록
        TipRepository tipRepo = new TipRepository();
        Callback<List<TipResponse>> tipCallback = new Callback<List<TipResponse>>() {
            @Override
            public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (TipResponse t : response.body())
                        combined.add(new MyPostsAdapter.PostItem(t));
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                Log.e("MyPage", type + " 팁 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        };

        switch (type) {
            case "comments":
                repository.getCommentedFeeds(feedCallback);
                tipRepo.getCommentedTips(tipCallback);
                break;
            case "likes":
                repository.getLikedFeeds(feedCallback);
                tipRepo.getLikedTips(tipCallback);
                break;
            case "bookmarks":
                repository.getBookmarkedFeeds(feedCallback);
                tipRepo.getBookmarkedTips(tipCallback);
                break;
        }
    }

    // ─── 뷰 참조 초기화 및 클릭 리스너 등록 ───
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        layoutStatus = findViewById(R.id.layout_status);
        tabLayout = findViewById(R.id.tab_layout);
        rvMyFeeds = findViewById(R.id.rv_my_feeds);
        tvUsername = findViewById(R.id.tv_username);
        tvUsername.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, com.neostride.app.feature.account.AccountActivity.class);
            startActivity(intent);
        });
        tvFriends = findViewById(R.id.tv_friends);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        ivProfile = findViewById(R.id.iv_profile);
        ivProfile.setOnClickListener(v -> showImagePickDialog());
        ivBadge = findViewById(R.id.iv_badge);
        tvFriends = findViewById(R.id.tv_friends);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // 배지 아이콘 참조 및 클릭 리스너 설정
        ivBadge = findViewById(R.id.iv_badge);
        if (ivBadge != null) {
            ivBadge.setOnClickListener(v -> {
                // 별도 패키지에 있으므로 아래와 같이 명시적으로 호출하거나
                Intent intent = new Intent(MyPageActivity.this, com.neostride.app.feature.badge.BadgeActivity.class);
                startActivity(intent);
            });
        }
        // 친구 수 텍스트 클릭 시 FriendActivity로 이동
        if (tvFriends != null) {
            tvFriends.setOnClickListener(v -> {
                Intent intent = new Intent(MyPageActivity.this, FriendActivity.class);
                startActivity(intent);
            });
        }
    }

    // ─── 서버에서 프로필·배지 정보를 조회하고 초기 피드(내 피드)를 로드 ───
    private void fetchData() {
        repository.getUserProfile(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                updateUI(null);
            }
        });

        // 배지 등급 조회 → 마이페이지에서는 언랭(NONE)도 회색으로 항상 표시
        badgeRepository.fetchBadgeDetail(badgeResponse -> {
            BadgeTier tier = BadgeTier.fromString(badgeResponse.tier);
            if (ivBadge != null && ivBadge.getDrawable() != null) {
                ivBadge.setVisibility(android.view.View.VISIBLE);
                Drawable d = DrawableCompat.wrap(ivBadge.getDrawable()).mutate();
                DrawableCompat.setTint(d, tier.getColor());
                ivBadge.setImageDrawable(d);
            }
        });

        // 첫 탭(내가 쓴 글) 기본값: 전체 필터
        currentPostFilter = "all";
        loadMyPostsAll();
    }

    // ─── 내 피드만 조회하여 RecyclerView에 표시 (레거시 메서드) ───
    private void fetchMyFeeds() {
        repository.getMyFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MyFeedAdapter adapter = new MyFeedAdapter(response.body());
                    adapter.setOnProfileClickListener((userId, nickname) -> {
                        int myId = TokenManager.getUserId(MyPageActivity.this);
                        if (userId == myId) return;
                        Intent intent = new Intent(MyPageActivity.this, RunnerPageActivity.class);
                        intent.putExtra("user_id", userId);
                        intent.putExtra("nickname", nickname);
                        startActivity(intent);
                    });
                    rvMyFeeds.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", "피드 로딩 실패!");
            }
        });
    }

    // ─── 서버 응답 데이터로 닉네임·친구 수·상태 메시지·탭 카운트 UI를 갱신 ───
    private void updateUI(UserProfileResponse data) {
        // 방어 코드: 데이터가 아예 없는 경우 처리(백엔드에서 정보를 불러오지 못하고 있을 때)
        if (data == null) {
            tvUsername.setText("Nul");
            tvFriends.setText("친구 Nul");
            tvStatusMessage.setText("정보를 불러올 수 없습니다.");
            return;
        }

        this.cachedUserData = data;

        // 프로필 이미지 — 서버 URL이 있으면 Glide로 로드, 없으면 기본 아이콘
        if (data.profilePhoto != null && !data.profilePhoto.trim().isEmpty()) {
            String photoUrl = data.profilePhoto;
            // 상대 경로면 BASE_URL 앞에 붙임
            if (!photoUrl.startsWith("http://") && !photoUrl.startsWith("https://")) {
                String base = com.neostride.app.BuildConfig.BASE_URL;
                if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                photoUrl = base + (photoUrl.startsWith("/") ? photoUrl : "/" + photoUrl);
            }
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }

        // 위 if문 이후인 여기서부터는 data가 null이 아님이 보장
        // 닉네임 처리
        tvUsername.setText(data.nickname != null ? data.nickname : "Nul");

        // 친구 수 처리
        String friendCount = (data.friendCount != null) ? String.valueOf(data.friendCount) : "Nul";
        tvFriends.setText("친구 " + friendCount);

        // 탭 레이아웃 (내가 쓴 글 — 피드 + 팁 합산 카운트)
        TabLayout.Tab postTab = tabLayout.getTabAt(0);
        if (postTab != null) {
            int feedCount = (data.postCount != null) ? data.postCount : 0;
            int tipCount  = (data.tipCount  != null) ? data.tipCount  : 0;
            postTab.setText("내가 쓴 글 " + (feedCount + tipCount));
        }

        // 탭 레이아웃 (태그된 수)
        TabLayout.Tab taggedTab = tabLayout.getTabAt(1);
        if (taggedTab != null) {
            String count = (data.taggedCount != null) ? String.valueOf(data.taggedCount) : "Nul";
            taggedTab.setText("나를 태그한 피드 " + count);
        }

        // 상태 메시지 처리
        if (data.statusMessage != null && !data.statusMessage.trim().isEmpty()) {
            tvStatusMessage.setText(data.statusMessage);
        } else {
            tvStatusMessage.setText("상태메세지가 없습니다.");
        }

        // 프로필 이미지 처리
        if (data.profilePhoto != null && !data.profilePhoto.isEmpty()) {
            Glide.with(this).load(data.profilePhoto).circleCrop().into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    // ─── 프로필 이미지 변경 다이얼로그 (카메라/갤러리/기본 이미지 선택) ───
    private void showImagePickDialog() {
        // 1. 커스텀 뷰 준비
        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_profile_image, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 배경 투명 처리 (둥근 테두리 부각)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // --- 각 버튼 클릭 이벤트 설정 ---

        // [카메라 버튼] 권한 체크 로직 포함
        dialogView.findViewById(R.id.btn_take_camera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(intent);
                } catch (SecurityException e) {
                    // Android auto-revoke 등으로 권한이 사라진 경우 재요청
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                }
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
            dialog.dismiss();
        });

        // [갤러리 버튼]
        dialogView.findViewById(R.id.btn_pick_gallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
            dialog.dismiss();
        });

        // [기본 이미지 버튼]
        dialogView.findViewById(R.id.btn_default_image).setOnClickListener(v -> {
            ivProfile.setImageResource(R.drawable.ic_profile);
            dialog.dismiss();
        });

        // [취소 버튼]
        dialogView.findViewById(R.id.btn_image_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ─── RecyclerView 레이아웃 매니저 설정 ───
    private void setupRecyclerView() {
        rvMyFeeds.setLayoutManager(new LinearLayoutManager(this));
    }

    // ─── TabLayout 탭 선택 리스너 설정 ───
    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isRevertingTab) return;

                int position = tab.getPosition();
                if (position == 0) {
                    updateActivityTabTitle("내 활동");
                    isMenuItemSelected = false;
                    lastRealTabPosition = position;
                    applyFilter(currentPostFilter);
                }
                else if (position == 1) {
                    updateActivityTabTitle("내 활동");
                    isMenuItemSelected = false;
                    lastRealTabPosition = position;
                    loadFeeds("tagged");
                }
                else if (position == 2) {
                    showActivityMenu();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // [삭제] 아이콘 색상 변경 로직이 필요 없습니다.
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) showActivityMenu();
            }
        });
    }

    // ─── "활동" 탭 클릭 시 댓글·좋아요·북마크 선택 PopupWindow 표시 ───
    private void showActivityMenu() {
        View popupView = getLayoutInflater().inflate(R.layout.layout_my_activity_menu, null);
        isMenuItemSelected = false; // 팝업이 열릴 때 초기화

        // 1. 숫자를 먼저 세팅합니다.
        if (cachedUserData != null) {
            TextView tvComments = popupView.findViewById(R.id.tv_count_comments);
            TextView tvLikes = popupView.findViewById(R.id.tv_count_likes);
            TextView tvBookmarks = popupView.findViewById(R.id.tv_count_bookmarks);

            tvComments.setText(String.valueOf(cachedUserData.commentedFeedCount != null ? cachedUserData.commentedFeedCount : 0));
            tvLikes.setText(String.valueOf(cachedUserData.likedFeedCount != null ? cachedUserData.likedFeedCount : 0));
            tvBookmarks.setText(String.valueOf(cachedUserData.bookmarkedFeedCount != null ? cachedUserData.bookmarkedFeedCount : 0));
        }

        // 2. PopupWindow 생성
        int menuWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 155, getResources().getDisplayMetrics());
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(15f);

        popupWindow.setOnDismissListener(() -> {
            // [수정] 항목을 선택하지 않고 그냥 팝업을 닫았을 때만 실행
            if (!isMenuItemSelected) {
                // 1. 탭 이름을 다시 "내 활동 ▼"으로 원상복구 시킵니다.
                updateActivityTabTitle("내 활동");

                // 2. 하이라이트를 이전 탭으로 돌려보냅니다.
                isRevertingTab = true;
                tabLayout.getTabAt(lastRealTabPosition).select();
                isRevertingTab = false;
            }
        });

        // [핵심 2] 클릭 리스너 통합 (하단에 있던 중복 리스너는 지워야 합니다!)
        popupView.findViewById(R.id.menu_my_comments).setOnClickListener(v -> {
            isMenuItemSelected = true;
            currentActivityType = "comments";
            int count = (cachedUserData != null && cachedUserData.commentedFeedCount != null) ? cachedUserData.commentedFeedCount : 0;
            updateActivityTabTitle("내가 쓴 댓글 " + count);
            loadFeeds("comments");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_my_likes).setOnClickListener(v -> {
            isMenuItemSelected = true;
            currentActivityType = "likes";
            int count = (cachedUserData != null && cachedUserData.likedFeedCount != null) ? cachedUserData.likedFeedCount : 0;
            updateActivityTabTitle("내가 한 좋아요 " + count);
            loadFeeds("likes");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menu_my_bookmarks).setOnClickListener(v -> {
            isMenuItemSelected = true;
            currentActivityType = "bookmarks";
            int count = (cachedUserData != null && cachedUserData.bookmarkedFeedCount != null) ? cachedUserData.bookmarkedFeedCount : 0;
            updateActivityTabTitle("내가 한 북마크 " + count);
            loadFeeds("bookmarks");
            popupWindow.dismiss();
        });

        // 3. 위치 정렬 및 띄우기
        View anchor = tabLayout.getTabAt(2).view;
        int xOff = -(menuWidthPx - anchor.getWidth());
        int yOff = 10;
        popupWindow.showAsDropDown(anchor, xOff, yOff);
    }

    // ─── 상태 메시지 수정 다이얼로그 (즉시 UI 반영 후 서버 PATCH) ───
    private void showEditStatusDialog() {
        // 1. [레이아웃 준비] 커스텀 뷰 인플레이트 및 뷰 참조
        View dialogView = getLayoutInflater().inflate(R.layout.layout_mypage_edit_status, null);

        EditText etInput = dialogView.findViewById(R.id.et_status_input);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);

        // 2. [입력창 설정] 실시간 글자 수 카운트 및 기존 메시지 세팅
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 실시간으로 (현재 글자수/30) 표시 업데이트
                tvCharCount.setText(s.length() + "/30");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 기존 메시지를 불러와서 입력창에 넣고 커서를 맨 뒤로 보냅니다.
        // "상태메세지가 없습니다."는 placeholder이므로 입력창은 빈 상태로 시작
        String currentStatus = tvStatusMessage.getText().toString();
        if (currentStatus.equals("상태메세지가 없습니다.")) {
            etInput.setText("");
        } else {
            etInput.setText(currentStatus);
            etInput.setSelection(etInput.length());
        }

        // 3. [다이얼로그 생성] AlertDialog 객체 빌드 및 스타일 설정
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 배경을 투명하게 설정해야 커스텀 XML에서 디자인한 둥근 모서리가 정상적으로 보입니다.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 4. [리스너 설정 - 확인 버튼] 백엔드 DB 연동 및 성공 시 UI 갱신
        btnConfirm.setOnClickListener(v -> {
            String newMessage = etInput.getText().toString().trim();

            // [Step 1] 프론트엔드 UI를 즉시 변경하고 팝업을 닫습니다. (빠른 피드백)
            tvStatusMessage.setText(newMessage);
            dialog.dismiss();

            // [Step 2] 백엔드 DB 연동은 뒷단에서 조용히 진행합니다.
            repository.updateStatusMessage(newMessage, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // 서버 저장까지 완벽하게 성공! (이미 UI가 바뀌어 있으므로 별도 조치 불필요)
                        Log.d("API_SUCCESS", "DB 동기화 완료");
                    } else {
                        // 만약 서버 저장에 실패했다면?
                        // 여기서 다시 원래 메시지로 돌려놓는 처리를 하기도 합니다. (선택 사항)
                        Log.e("API_ERROR", "서버 저장 실패");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // 네트워크 오류 시 로그만 남깁니다.
                    Log.e("API_FAILURE", "네트워크 통신 실패");
                }
            });
        });

        // 5. [리스너 설정 - 취소 버튼] 다이얼로그 닫기
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 모든 설정이 완료된 다이얼로그를 화면에 띄웁니다.
        dialog.show();
    }

    // ─── 카메라로 촬영한 Bitmap을 캐시 파일로 변환하여 서버에 업로드 ───
    private void uploadProfileImageBitmap(Bitmap bitmap) {
        try {
            java.io.File file = new java.io.File(getCacheDir(), "profile_camera.jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush(); fos.close();
            uploadProfileFile(file);
        } catch (Exception e) {
            Log.e("MyPage", "카메라 이미지 변환 실패: " + e.getMessage());
        }
    }

    // ─── 갤러리에서 선택한 Uri를 캐시 파일로 변환하여 서버에 업로드 ───
    private void uploadProfileImageUri(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.File file = new java.io.File(getCacheDir(), "profile_gallery.jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            byte[] buf = new byte[4096]; int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            fos.flush(); fos.close(); is.close();
            uploadProfileFile(file);
        } catch (Exception e) {
            Log.e("MyPage", "갤러리 이미지 변환 실패: " + e.getMessage());
        }
    }

    // ─── Multipart 파일을 서버에 업로드하는 공통 로직 ───
    private void uploadProfileFile(java.io.File file) {
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(file, okhttp3.MediaType.parse("image/jpeg"));
        okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("image", file.getName(), requestBody);
        repository.updateProfileImage(part, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) Log.d("MyPage", "프로필 이미지 업로드 성공");
                else Log.e("MyPage", "프로필 이미지 업로드 실패: " + response.code());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MyPage", "프로필 이미지 업로드 네트워크 오류: " + t.getMessage());
            }
        });
    }

    // ─── 현재 필터값에 따라 데이터 로드 ───
    private void applyFilter(String filter) {
        switch (filter) {
            case "feed": loadMyFeeds(); break;
            case "tip":  loadMyTips();  break;
            default:     loadMyPostsAll(); break;
        }
    }

    // ─── "전체" 필터: 피드 + 팁 + 북마크 목록을 병렬 조회 후 MyPostsAdapter에 바인딩 ───
    private void loadMyPostsAll() {
        rvMyFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        final List<MyPostsAdapter.PostItem> combined = new ArrayList<>();
        final java.util.Set<Long> bookmarkedFeedIds = new java.util.HashSet<>();
        final int[] pending = {3}; // 피드 + 팁 + 북마크 목록 3개 API 완료 대기

        Runnable onAllDone = () -> runOnUiThread(() -> {
            // contentQuery가 is_bookmarked를 반환하지 않으므로 북마크 목록과 교차 설정
            for (MyPostsAdapter.PostItem pi : combined) {
                if (pi.type == MyPostsAdapter.TYPE_FEED && pi.feed != null) {
                    pi.feed.isBookmarked = bookmarkedFeedIds.contains((long) pi.feed.contentId);
                }
            }
            if (combined.isEmpty()) {
                if (tvEmptyState != null) {
                    tvEmptyState.setText("아직 작성한 게시글이 없어요\n피드나 팁을 작성해보세요");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                MyPostsAdapter adapter = new MyPostsAdapter(this, combined, currentPostFilter, this::onFilterClick);
                adapter.setOnPostDeletedListener(this::decrementPostTabCount);
                rvMyFeeds.setAdapter(adapter);
            }
        });

        // API 1: 내 피드
        repository.getMyFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (CommunityContentResponse f : response.body())
                        combined.add(new MyPostsAdapter.PostItem(f));
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", "내 피드 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        // API 2: 내 팁
        repository.getMyTips(new Callback<List<TipResponse>>() {
            @Override
            public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (TipResponse t : response.body())
                        combined.add(new MyPostsAdapter.PostItem(t));
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                Log.e("MyPage", "내 팁 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        // API 3: 북마크된 피드 목록 (contentId 수집용)
        repository.getBookmarkedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (CommunityContentResponse f : response.body())
                        bookmarkedFeedIds.add((long) f.contentId);
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", "북마크 목록 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        });
    }

    // ─── 필터 버튼 클릭 콜백 (헤더 ViewHolder → Activity) ───
    private void onFilterClick(String filter) {
        currentPostFilter = filter;
        applyFilter(filter);
    }

    // ─── "피드" 필터: 내 피드 + 북마크 목록을 병렬 조회 후 isBookmarked 교차 설정하여 MyPostsAdapter에 바인딩 ───
    private void loadMyFeeds() {
        rvMyFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        final List<CommunityContentResponse> feedList = new ArrayList<>();
        final java.util.Set<Long> bookmarkedFeedIds = new java.util.HashSet<>();
        final int[] pending = {2}; // 피드 + 북마크 목록 2개 API 완료 대기

        Runnable onAllDone = () -> runOnUiThread(() -> {
            // contentQuery가 is_bookmarked를 반환하지 않으므로 북마크 목록과 교차 설정
            for (CommunityContentResponse f : feedList) {
                f.isBookmarked = bookmarkedFeedIds.contains((long) f.contentId);
            }
            if (feedList.isEmpty()) {
                // 빈 목록이어도 헤더(필터 버튼)는 보여야 하므로 빈 어댑터로 설정
                rvMyFeeds.setAdapter(new MyPostsAdapter(MyPageActivity.this, new ArrayList<>(), currentPostFilter, MyPageActivity.this::onFilterClick));
                if (tvEmptyState != null) {
                    tvEmptyState.setText("아직 작성한 피드가 없어요\n첫 번째 러닝 기록을 공유해보세요");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                List<MyPostsAdapter.PostItem> items = new ArrayList<>();
                for (CommunityContentResponse f : feedList)
                    items.add(new MyPostsAdapter.PostItem(f));
                MyPostsAdapter adapter = new MyPostsAdapter(MyPageActivity.this, items, currentPostFilter, MyPageActivity.this::onFilterClick);
                adapter.setOnPostDeletedListener(MyPageActivity.this::decrementPostTabCount);
                rvMyFeeds.setAdapter(adapter);
                if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
            }
        });

        // API 1: 내 피드
        repository.getMyFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    feedList.addAll(response.body());
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", "내 피드 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        });

        // API 2: 북마크된 피드 목록 (contentId 수집용, is_bookmarked 교차 설정)
        repository.getBookmarkedFeeds(new Callback<List<CommunityContentResponse>>() {
            @Override
            public void onResponse(Call<List<CommunityContentResponse>> call, Response<List<CommunityContentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (CommunityContentResponse f : response.body())
                        bookmarkedFeedIds.add((long) f.contentId);
                }
                if (--pending[0] == 0) onAllDone.run();
            }
            @Override
            public void onFailure(Call<List<CommunityContentResponse>> call, Throwable t) {
                Log.e("MyPage", "북마크 목록 로딩 실패: " + t.getMessage());
                if (--pending[0] == 0) onAllDone.run();
            }
        });
    }

    // ─── "팁" 필터: 내 팁만 조회하여 MyPostsAdapter에 바인딩 ───
    private void loadMyTips() {
        rvMyFeeds.setAdapter(null);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        repository.getMyTips(new Callback<List<TipResponse>>() {
            @Override
            public void onResponse(Call<List<TipResponse>> call, Response<List<TipResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<MyPostsAdapter.PostItem> items = new ArrayList<>();
                    for (TipResponse t : response.body())
                        items.add(new MyPostsAdapter.PostItem(t));
                    MyPostsAdapter adapter = new MyPostsAdapter(MyPageActivity.this, items, currentPostFilter, MyPageActivity.this::onFilterClick);
                    adapter.setOnPostDeletedListener(MyPageActivity.this::decrementPostTabCount);
                    rvMyFeeds.setAdapter(adapter);
                    if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                } else {
                    rvMyFeeds.setAdapter(new MyPostsAdapter(MyPageActivity.this, new ArrayList<>(), currentPostFilter, MyPageActivity.this::onFilterClick));
                    if (tvEmptyState != null) {
                        tvEmptyState.setText("아직 작성한 팁이 없어요\n러닝 노하우를 나눠보세요");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<TipResponse>> call, Throwable t) {
                Log.e("MyPage", "내 팁 로딩 실패: " + t.getMessage());
                rvMyFeeds.setAdapter(new MyPostsAdapter(MyPageActivity.this, new ArrayList<>(), currentPostFilter, MyPageActivity.this::onFilterClick));
                if (tvEmptyState != null) {
                    tvEmptyState.setText("아직 작성한 팁이 없어요\n러닝 노하우를 나눠보세요");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // ─── 글 삭제 완료 시 탭 카운트 즉시 감소 ───
    private void decrementPostTabCount(int type) {
        if (cachedUserData == null) return;
        if (type == MyPostsAdapter.TYPE_FEED) {
            if (cachedUserData.postCount != null && cachedUserData.postCount > 0)
                cachedUserData.postCount--;
        } else if (type == MyPostsAdapter.TYPE_TIP) {
            if (cachedUserData.tipCount != null && cachedUserData.tipCount > 0)
                cachedUserData.tipCount--;
        }
        TabLayout.Tab postTab = tabLayout.getTabAt(0);
        if (postTab != null) {
            int feedCount = (cachedUserData.postCount != null) ? cachedUserData.postCount : 0;
            int tipCount  = (cachedUserData.tipCount  != null) ? cachedUserData.tipCount  : 0;
            postTab.setText("내가 쓴 글 " + (feedCount + tipCount));
        }
    }

    // ─── 북마크 목록에서 아이템이 제거될 때 카운트 즉시 갱신 ───
    private void onBookmarkRemovedFromList() {
        if (cachedUserData != null && cachedUserData.bookmarkedFeedCount != null
                && cachedUserData.bookmarkedFeedCount > 0) {
            cachedUserData.bookmarkedFeedCount--;
        }
        if ("bookmarks".equals(currentActivityType)) {
            int count = (cachedUserData != null && cachedUserData.bookmarkedFeedCount != null)
                    ? cachedUserData.bookmarkedFeedCount : 0;
            updateActivityTabTitle("내가 한 북마크 " + count);
        }
    }

    // ─── "활동" 탭 텍스트를 갱신 (기본값: "내 활동 ▼", 하위 메뉴 선택 시 해당 이름 표시) ───
    private void updateActivityTabTitle(String title) {
        TabLayout.Tab activityTab = tabLayout.getTabAt(2);
        if (activityTab != null) {
            // 1. "내 활동"일 때만 화살표(▼)를 붙입니다.
            if (title.equals("내 활동")) {
                String fullText = title + " ▼";
                SpannableString ss = new SpannableString(fullText);

                int startIndex = fullText.indexOf("▼");
                if (startIndex != -1) {
                    ss.setSpan(new RelativeSizeSpan(0.8f), startIndex, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                activityTab.setText(ss);
            }
            // 2. 그 외(내가 쓴 댓글 등)의 경우에는 화살표 없이 깔끔하게 이름만 표시합니다.
            else {
                activityTab.setText(title);
            }
        }
    }
}