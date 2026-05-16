package com.neostride.app.feature.account.api;

import com.neostride.app.feature.account.model.AccountInfoResponse;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;

public interface AccountApi {

    /** 계정 정보 조회 (이메일, 닉네임, 프로필 사진) */
    @GET("users/me/account")
    Call<AccountInfoResponse> getAccountInfo();

    /** 닉네임 변경 */
    @PATCH("users/me/nickname")
    Call<ResponseBody> updateNickname(@Body Map<String, String> body);

    /** 계정 탈퇴 */
    @DELETE("users/me")
    Call<ResponseBody> deleteAccount();
}
