package com.neostride.app.feature.account;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.account.api.AccountApi;
import com.neostride.app.feature.account.model.AccountInfoResponse;
import com.neostride.app.feature.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//  계정 관리 화면 Activity
//  <p>
//  - 이메일·닉네임·프로필 사진 조회 및 수정
//  - 닉네임 변경 다이얼로그 제공
//  - 로그아웃 및 계정 탈퇴 처리

public class AccountActivity extends AppCompatActivity {

    // ── UI 뷰 ──
    private ShapeableImageView ivProfile;
    private TextView tvEmailValue, tvNicknameValue;

    // ── 네트워크 ──
    private AccountApi accountApi;
    private String currentNickname = "";

    // 카메라 촬영 런처
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null) {
                        Glide.with(AccountActivity.this).load(bitmap).circleCrop().into(ivProfile);
                        // TODO: 백엔드 연결 시 multipart 업로드
                    }
                }
            });

    // 카메라 권한 요청 런처
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            });

    // 갤러리 런처
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        Glide.with(this).load(imageUri).circleCrop().into(ivProfile);
                        // TODO: 백엔드 연결 시 multipart 업로드
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        accountApi = ApiClient.getInstance().create(AccountApi.class);
        //목서버 연결용
        //accountApi = MockApiClient.getInstance().create(AccountApi.class);

        initViews();
        fetchAccountInfo();
    }

    // ─── 뷰 참조 초기화 및 클릭 리스너 등록 ───
    private void initViews() {
        ivProfile       = findViewById(R.id.iv_profile);
        tvEmailValue    = findViewById(R.id.tv_email_value);
        tvNicknameValue = findViewById(R.id.tv_nickname_value);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 프로필 사진 변경
        findViewById(R.id.btn_edit_photo).setOnClickListener(v -> showImagePickDialog());

        // 닉네임 수정
        findViewById(R.id.row_nickname).setOnClickListener(v -> showNicknameEditDialog());

        // 로그아웃
        findViewById(R.id.row_logout).setOnClickListener(v -> showLogoutDialog());

        // 계정 탈퇴
        findViewById(R.id.btn_delete_account).setOnClickListener(v -> showDeleteAccountDialog());

    }

    // ─── 서버에서 계정 정보(이메일, 닉네임, 프로필 사진)를 불러와 UI에 반영 ───
    private void fetchAccountInfo() {
        accountApi.getAccountInfo().enqueue(new Callback<AccountInfoResponse>() {
            @Override
            public void onResponse(Call<AccountInfoResponse> call, Response<AccountInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfoResponse info = response.body();
                    currentNickname = info.nickname != null ? info.nickname : "";
                    String email    = info.email    != null ? info.email    : "—";

                    tvNicknameValue.setText(currentNickname);
                    tvEmailValue.setText(email);

                    if (info.profilePhoto != null && !info.profilePhoto.isEmpty()) {
                        Glide.with(AccountActivity.this)
                                .load(info.profilePhoto)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(ivProfile);
                    }
                }
            }

            @Override
            public void onFailure(Call<AccountInfoResponse> call, Throwable t) {
                // 로컬 저장된 닉네임으로 폴백
                runOnUiThread(() -> {
                    currentNickname = TokenManager.getNickname(AccountActivity.this);
                    tvNicknameValue.setText(currentNickname);
                });
            }
        });
    }

    // ─── 프로필 이미지 변경 다이얼로그 (카메라/갤러리/기본 이미지 선택) ───
    private void showImagePickDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_profile_image, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btn_take_camera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                } catch (SecurityException e) {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                }
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_pick_gallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_default_image).setOnClickListener(v -> {
            ivProfile.setImageResource(R.drawable.ic_profile);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_image_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ─── 닉네임 수정 다이얼로그 (글자 수 실시간 표시, 서버 PATCH 요청) ───
    private void showNicknameEditDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_account_edit_nickname, null);

        EditText etInput    = dialogView.findViewById(R.id.et_nickname_input);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        TextView btnCancel  = dialogView.findViewById(R.id.btn_cancel);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);

        // 기존 닉네임 세팅 및 커서 맨 뒤로
        etInput.setText(currentNickname);
        etInput.setSelection(etInput.length());
        tvCharCount.setText(currentNickname.length() + "/12");

        // 실시간 글자 수 카운트
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/12");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String newNick = etInput.getText().toString().trim();
            if (newNick.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newNick.equals(currentNickname)) {
                dialog.dismiss();
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("nickname", newNick);
            accountApi.updateNickname(body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        if (response.isSuccessful()) {
                            currentNickname = newNick;
                            tvNicknameValue.setText(newNick);
                            TokenManager.saveUserInfo(AccountActivity.this,
                                    TokenManager.getUserId(AccountActivity.this), newNick);
                            Toast.makeText(AccountActivity.this, "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AccountActivity.this, "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(AccountActivity.this, "닉네임 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        dialog.show();
    }

    // ─── 로그아웃 확인 다이얼로그 (확인 시 토큰 삭제 후 LoginActivity 이동) ───
    private void showLogoutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = buildSimpleConfirmRoot("로그아웃", 0xFFFF3B30,
                R.drawable.bg_popup_red_border, "정말 로그아웃 하시겠습니까?");

        LinearLayout btnRow = makeBtnRow();

        TextView btnCancel = makeCancelBtn();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = makeConfirmBtn("로그아웃", 0xFFFF3B30, Color.BLACK);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            TokenManager.clearTokens(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        showDialog(dialog, root);
    }

    // ─── 계정 탈퇴 확인 다이얼로그 (확인 시 서버 DELETE 요청 후 LoginActivity 이동) ───
    private void showDeleteAccountDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = buildSimpleConfirmRoot("계정 탈퇴", 0xFFFF4444, R.drawable.bg_popup_red_border,
                "탈퇴 시 모든 데이터가 삭제되며\n복구할 수 없습니다.\n정말 탈퇴하시겠습니까?");

        LinearLayout btnRow = makeBtnRow();
        TextView btnCancel = makeCancelBtn();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnConfirm = makeConfirmBtn("탈퇴", 0xFFFF4444, Color.BLACK);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            accountApi.deleteAccount().enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    runOnUiThread(() -> {
                        TokenManager.clearTokens(AccountActivity.this);
                        Toast.makeText(AccountActivity.this, "계정이 탈퇴 처리되었습니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() ->
                            Toast.makeText(AccountActivity.this, "탈퇴 처리에 실패했습니다.", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        showDialog(dialog, root);
    }

    // ── 다이얼로그 빌더 헬퍼 ──

    // ─── 기본 배경(bg_feed_card_bordered)의 확인 다이얼로그 루트 레이아웃 생성 ───
    private LinearLayout buildSimpleConfirmRoot(String title, int titleColor, String message) {
        return buildSimpleConfirmRoot(title, titleColor, R.drawable.bg_feed_card_bordered, message);
    }

    private LinearLayout buildSimpleConfirmRoot(String title, int titleColor, int bgRes, String message) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(bgRes);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(titleColor);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(14);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(12);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        root.addView(makeDivider(dp(20)));
        return root;
    }

    // ─── 다이얼로그 내부 수평 구분선 뷰 생성 ───
    private View makeDivider(int topMargin) {
        View divider = new View(this);
        divider.setBackgroundColor(0xFF2A2A2A);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = topMargin;
        divider.setLayoutParams(divP);
        return divider;
    }

    // ─── 버튼을 오른쪽 정렬로 담는 수평 행 레이아웃 생성 ───
    private LinearLayout makeBtnRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(16);
        row.setLayoutParams(p);
        return row;
    }

    // ─── 회색 텍스트 "취소" 버튼 생성 ───
    private TextView makeCancelBtn() {
        TextView btn = new TextView(this);
        btn.setText("취소");
        btn.setTextColor(0xFF888888);
        btn.setPadding(dp(16), dp(10), dp(16), dp(10));
        return btn;
    }

    // ─── 지정 색상·텍스트로 확인 버튼(둥근 배경) 생성 ───
    private TextView makeConfirmBtn(String text, int bgColor, int textColor) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(bgColor);
        btn.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMarginStart(dp(8));
        btn.setLayoutParams(p);
        return btn;
    }

    // ─── 다이얼로그를 화면 너비 85%로 중앙에 표시 ───
    private void showDialog(Dialog dialog, LinearLayout root) {
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─── dp 값을 픽셀로 변환 ───
    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
