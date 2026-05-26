package com.neostride.app.feature.account.api;

import com.neostride.app.feature.account.model.AccountInfoResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.Part;

public interface AccountApi {

    /** 계정 정보 조회 (이메일, 닉네임, 프로필 사진) */
    @GET("users/me/account")
    Call<AccountInfoResponse> getAccountInfo();

    /** 닉네임 변경 */
    @PATCH("users/me/nickname")
    Call<ResponseBody> updateNickname(@Body Map<String, String> body);

    /** 프로필 이미지 변경 — multipart/form-data, image 파트 */
    @Multipart
    @PATCH("users/me/profile-image")
    Call<ResponseBody> updateProfileImage(@Part MultipartBody.Part image);

    /** 계정 탈퇴 */
    @DELETE("users/me")
    Call<ResponseBody> deleteAccount();
}
