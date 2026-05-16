package com.neostride.app.feature.account.model;

import com.google.gson.annotations.SerializedName;

public class AccountInfoResponse {
    @SerializedName("email")         public String email;
    @SerializedName("nickname")      public String nickname;
    @SerializedName("profile_photo") public String profilePhoto;
}
