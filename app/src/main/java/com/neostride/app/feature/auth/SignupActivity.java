package com.neostride.app.feature.auth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.neostride.app.R;
import com.neostride.app.feature.auth.model.SignupRequest;
import com.neostride.app.feature.auth.model.SignupResponse;
import com.neostride.app.feature.auth.repository.AuthRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private android.widget.ImageButton imgAdd;
    private EditText etEmail;
    private EditText etName;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;

    private boolean isPwVisible = false;

    private AuthRepository authRepository;

    // 선택된 이미지 (갤러리 URI 또는 카메라 Bitmap 중 하나만 사용)
    private Uri selectedImageUri = null;
    private Bitmap selectedCameraBitmap = null;

    // 카메라 촬영 런처
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null) {
                        selectedCameraBitmap = bitmap;
                        selectedImageUri = null;
                        Glide.with(SignupActivity.this).load(bitmap).circleCrop().into(ivProfile);
                        if (imgAdd != null) imgAdd.setVisibility(android.view.View.GONE);
                    }
                }
            });

    // 카메라 권한 요청 런처
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
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
                        selectedImageUri = imageUri;
                        selectedCameraBitmap = null;
                        Glide.with(this).load(imageUri).circleCrop().into(ivProfile);
                        if (imgAdd != null) imgAdd.setVisibility(android.view.View.GONE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ivProfile         = findViewById(R.id.img_profile);
        imgAdd            = findViewById(R.id.img_add);
        etEmail           = findViewById(R.id.et_email);
        etName            = findViewById(R.id.et_name);
        etPassword        = findViewById(R.id.et_pw);
        etConfirmPassword = findViewById(R.id.et_confirm_pw);
        btnRegister       = findViewById(R.id.btn_register);

        authRepository = new AuthRepository();

        // 프로필 이미지 / 추가 버튼 클릭 → 이미지 선택 다이얼로그
        findViewById(R.id.img_profile).setOnClickListener(v -> showImagePickDialog());
        findViewById(R.id.img_add).setOnClickListener(v -> showImagePickDialog());

        // 눈알 버튼 — 비밀번호 표시/숨김 토글
        ImageButton imgEye1 = findViewById(R.id.img_eye1);
        ImageButton imgEye2 = findViewById(R.id.img_eye2);

        android.view.View.OnClickListener eyeToggle = v -> {
            isPwVisible = !isPwVisible;

            etPassword.setTransformationMethod(
                    isPwVisible ? HideReturnsTransformationMethod.getInstance()
                                : PasswordTransformationMethod.getInstance());
            etPassword.setSelection(etPassword.length());

            etConfirmPassword.setTransformationMethod(
                    isPwVisible ? HideReturnsTransformationMethod.getInstance()
                                : PasswordTransformationMethod.getInstance());
            etConfirmPassword.setSelection(etConfirmPassword.length());

            if (isPwVisible) {
                imgEye1.setColorFilter(android.graphics.Color.WHITE);
                imgEye2.setColorFilter(android.graphics.Color.WHITE);
            } else {
                imgEye1.clearColorFilter();
                imgEye2.clearColorFilter();
            }
        };

        imgEye1.setOnClickListener(eyeToggle);
        imgEye2.setOnClickListener(eyeToggle);

        // 회원가입 버튼 클릭
        btnRegister.setOnClickListener(v -> {

            String email    = etEmail.getText().toString().trim();
            String name     = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("SIGNUP", "회원가입 요청 email=" + email + " name=" + name);

            // 이미지가 선택된 경우 multipart, 없으면 JSON 방식으로 가입
            MultipartBody.Part photoPart = buildPhotoPart();
            if (photoPart != null) {
                RequestBody rbEmail    = RequestBody.create(MediaType.parse("text/plain"), email);
                RequestBody rbName     = RequestBody.create(MediaType.parse("text/plain"), name);
                RequestBody rbPassword = RequestBody.create(MediaType.parse("text/plain"), password);
                authRepository.signupWithPhoto(rbEmail, rbName, rbPassword, photoPart, signupCallback());
            } else {
                authRepository.signup(new SignupRequest(email, name, password), signupCallback());
            }
        });
    }

    // ─── 선택된 이미지를 MultipartBody.Part로 변환 ───
    // 갤러리 URI 또는 카메라 Bitmap 중 선택된 것을 JPEG로 인코딩하여 반환.
    // 이미지 미선택 시 null 반환.
    private MultipartBody.Part buildPhotoPart() {
        try {
            byte[] imageBytes = null;

            if (selectedImageUri != null) {
                // 갤러리에서 선택한 경우: URI → InputStream → byte[]
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) baos.write(buffer, 0, len);
                    is.close();
                    imageBytes = baos.toByteArray();
                }
            } else if (selectedCameraBitmap != null) {
                // 카메라로 촬영한 경우: Bitmap → JPEG byte[]
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                selectedCameraBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                imageBytes = baos.toByteArray();
            }

            if (imageBytes == null) return null;

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
            // 서버 수신 필드명: "profile_photo"
            return MultipartBody.Part.createFormData("profile_photo", "profile.jpg", requestBody);

        } catch (Exception e) {
            Log.e("SIGNUP", "이미지 변환 실패", e);
            return null;
        }
    }

    // ─── 회원가입 응답 공통 콜백 ───
    private Callback<SignupResponse> signupCallback() {
        return new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                Log.d("SIGNUP", "응답 코드: " + response.code());

                if (response.code() == 201) {
                    Log.d("SIGNUP", "회원가입 성공");
                    Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, SuccessActivity.class));
                    finish();

                } else if (response.code() == 409) {
                    // 백엔드 응답 body의 message 필드를 그대로 표시
                    String msg = "이미 가입된 이메일입니다.";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            org.json.JSONObject json = new org.json.JSONObject(errorBody);
                            if (json.has("message")) msg = json.getString("message");
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_SHORT).show();

                } else if (response.code() == 400) {
                    Toast.makeText(SignupActivity.this, "입력값을 확인해주세요.", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                Log.e("SIGNUP", "요청 실패: " + t.getMessage());
                Toast.makeText(SignupActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        };
    }

    // ─── 프로필 이미지 변경 다이얼로그 (카메라 / 갤러리 / 기본 이미지) ───
    private void showImagePickDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_profile_image, null);

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
            // 기본 이미지 선택 시 선택된 이미지 초기화
            selectedImageUri = null;
            selectedCameraBitmap = null;
            ivProfile.setImageResource(R.drawable.ic_profile);
            if (imgAdd != null) imgAdd.setVisibility(android.view.View.VISIBLE);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_image_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
