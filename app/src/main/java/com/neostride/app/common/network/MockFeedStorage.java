package com.neostride.app.common.network;

import com.neostride.app.feature.feed.model.FeedItem;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import java.util.ArrayList;
import java.util.List;

/*
 * 피드 목록을 임시로 저장하는 Mock Storage 클래스임
 * 실제 서버 API가 완성되기 전까지 앱 내부에서 피드 데이터를 보관함
 */
public class MockFeedStorage {

    // 앱 실행 중 임시로 유지되는 피드 목록임
    private static final List<FeedItem> feedItemList =
            new ArrayList<>();

    /*
     * 업로드 응답 데이터를 FeedItem으로 변환해서 저장함
     */
    public static void addFeedFromResponse(
            FeedUploadResponse response
    ) {

        FeedItem feedItem = new FeedItem(

                response.getProfileImageUrl(),

                response.getNickname(),

                "방금 전",

                response.getTitle(),

                response.getContent(),

                response.getTaggedCount(),

                response.getLikeCount(),

                response.getCommentCount(),

                response.getDistance(),

                response.getDuration(),

                response.getPace(),

                // 지도 표시 여부
                response.isMapVisible(),

                // 지도 캡처 이미지 URI
                response.getRouteMapImageUri(),

                // 일반 사진 목록
                response.getImageUrls()
        );

        feedItemList.add(0, feedItem);
    }

    /*
     * 저장된 피드 목록을 반환함
     */
    public static List<FeedItem> getFeedItemList() {
        return new ArrayList<>(feedItemList);
    }

    /*
     * 테스트용 피드 목록 초기화 함수임
     */
    public static void clear() {
        feedItemList.clear();
    }
}