package com.neostride.app.feature.tip.model;

import java.util.List;

/*
 * 팁 목록 조회 응답 DTO 클래스임
 */
public class TipListResponse {

    private List<TipUploadResponse> tips;

    public List<TipUploadResponse> getTips() {
        return tips;
    }
}