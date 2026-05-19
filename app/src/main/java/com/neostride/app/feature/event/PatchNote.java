package com.neostride.app.feature.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * 패치노트 데이터 모델임
 * 앱 업데이트 시 이 파일에 새 항목을 추가하면 됨
 */
public class PatchNote {

    public final String version;
    public final String date;
    public final String summary;
    public final List<String> newFeatures;
    public final List<String> improvements;
    public final List<String> bugFixes;

    public PatchNote(
            String version,
            String date,
            String summary,
            List<String> newFeatures,
            List<String> improvements,
            List<String> bugFixes
    ) {
        this.version      = version;
        this.date         = date;
        this.summary      = summary;
        this.newFeatures  = newFeatures  != null ? newFeatures  : Collections.emptyList();
        this.improvements = improvements != null ? improvements : Collections.emptyList();
        this.bugFixes     = bugFixes     != null ? bugFixes     : Collections.emptyList();
    }

    /*
     * 하드코딩 패치노트 목록임 — 최신 버전이 맨 위에 오도록 역순으로 추가할 것
     * 새 버전 출시 시 리스트 맨 앞에 항목 추가
     */
    public static List<PatchNote> getAll() {
        return Arrays.asList(

            new PatchNote(
                "v1.0.0",
                "2026.05.16",
                "Neo-Stride 첫 번째 릴리즈입니다. 러닝 기록, 커뮤니티, 코칭 기능을 만나보세요.",
                Arrays.asList(
                    "러닝 기록 및 GPS 트래킹",
                    "피드 · 팁 커뮤니티",
                    "검색 (피드 / 팁 / 프로필 / 친구)",
                    "친구 요청 · 수락 · 삭제",
                    "AI 코칭 및 목표 설정",
                    "배지 등급 시스템",
                    "마이페이지 및 러너 프로필"
                ),
                null,
                null
            )

        );
    }
}
