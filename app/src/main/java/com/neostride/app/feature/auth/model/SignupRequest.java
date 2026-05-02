package com.neostride.app.feature.auth.model;

public class SignupRequest {

    private String email;      // 이메일
    private String name;       // 이름
    private String password;   // 비밀번호

    public SignupRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}