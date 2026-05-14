package com.neostride.app.common.network;

import com.neostride.app.feature.feed.model.FeedUploadRequest;
import com.neostride.app.feature.feed.model.FeedUploadResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 서버 API가 완성되기 전까지 피드 기능 테스트를 위한 Mock 서버 클래스임
 * 실제 서버처럼 피드 목록 조회, 피드 업로드, 태그 조회 등의 결과를 임시로 반환함
 */
public class MockFeedServer {

    // Mock 피드 목록을 메모리에 저장함
    private static final List<FeedUploadResponse> mockFeedList = new ArrayList<>();

    // 피드 ID별 태그된 사용자 목록을 저장함
    private static final Map<Long, List<String>> mockTaggedUserMap = new HashMap<>();

    // Mock 데이터가 한 번만 초기화되도록 확인하는 변수임
    private static boolean initialized = false;

    /*
     * Mock 피드 데이터를 초기화하는 함수임
     */
    private static void initMockDataIfNeeded() {
        if (initialized) {
            return;
        }

        FeedUploadResponse first = new FeedUploadResponse();
        first.setFeedId(1L);
        first.setProfileImageUrl("");
        first.setNickname("zinza");
        first.setCreatedAt("방금 전");
        first.setTitle("오늘 러닝 완료");
        first.setContent("날씨가 좋아서 가볍게 뛰었습니다.");
        first.setTaggedCount(3);
        first.setLikeCount(12);
        first.setCommentCount(3);
        first.setDistance(3.25);
        first.setDuration("18:42");
        first.setPace("5:45/km");
        first.setMapVisible(true);
        first.setRouteMapImageUri("");
        first.setImageUrls(new ArrayList<>());

        FeedUploadResponse second = new FeedUploadResponse();
        second.setFeedId(2L);
        second.setProfileImageUrl("");
        second.setNickname("runner_kim");
        second.setCreatedAt("10분 전");
        second.setTitle("야간 러닝");
        second.setContent("페이스는 조금 느렸지만 끝까지 완주했습니다.");
        second.setTaggedCount(0);
        second.setLikeCount(8);
        second.setCommentCount(1);
        second.setDistance(5.10);
        second.setDuration("31:20");
        second.setPace("6:08/km");
        second.setMapVisible(false);
        second.setRouteMapImageUri(null);
        second.setImageUrls(new ArrayList<>());

        mockFeedList.add(first);
        mockFeedList.add(second);

        // 1번 피드에 태그된 사용자 목록을 저장함
        List<String> firstTaggedUsers = new ArrayList<>();
        firstTaggedUsers.add("zinza");
        firstTaggedUsers.add("runner_kim");
        firstTaggedUsers.add("Ongcheon1004");
        mockTaggedUserMap.put(1L, firstTaggedUsers);

        // 2번 피드는 태그된 사용자가 없음
        mockTaggedUserMap.put(2L, new ArrayList<>());

        initialized = true;
    }

    /*
     * Mock 피드 목록을 반환하는 함수임
     */
    public static List<FeedUploadResponse> getFeedList() {
        initMockDataIfNeeded();

        return new ArrayList<>(mockFeedList);
    }

    /*
     * Mock 피드 업로드 결과를 반환하는 함수임
     */
    public static FeedUploadResponse uploadFeed(FeedUploadRequest request) {
        initMockDataIfNeeded();

        FeedUploadResponse response = new FeedUploadResponse();

        Long newFeedId = System.currentTimeMillis();

        response.setFeedId(newFeedId);
        response.setProfileImageUrl("");
        response.setNickname("zinza");
        response.setCreatedAt("방금 전");
        response.setTitle(request.getTitle());
        response.setContent(request.getContent());
        response.setTaggedCount(request.getTagCount());
        response.setLikeCount(0);
        response.setCommentCount(0);
        response.setDistance(request.getDistance());
        response.setDuration(request.getRunningTime());
        response.setPace(request.getPace());
        response.setMapVisible(request.isMapVisible());
        response.setRouteMapImageUri(request.getRouteMapImageUri());
        response.setImageUrls(request.getImageUrls());

        // 새로 업로드한 피드를 목록 맨 위에 추가함
        mockFeedList.add(0, response);

        // 업로드한 피드의 태그 사용자 목록도 Mock 서버에 저장함
        mockTaggedUserMap.put(newFeedId, createMockTaggedUsers(request.getTagCount()));

        return response;
    }

    /*
     * Mock 태그된 사용자 목록을 반환하는 함수임
     * 화면이나 Adapter에서 태그 이름을 직접 만들지 않고 여기서만 제공함
     */
    public static List<String> getTaggedUsers(Long feedId) {
        initMockDataIfNeeded();

        if (feedId == null) {
            return new ArrayList<>();
        }

        List<String> taggedUsers = mockTaggedUserMap.get(feedId);

        if (taggedUsers == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(taggedUsers);
    }

    /*
     * 업로드 시 선택한 태그 수에 맞춰 Mock 태그 사용자 목록을 만드는 함수임
     */
    private static List<String> createMockTaggedUsers(int tagCount) {
        List<String> taggedUsers = new ArrayList<>();

        if (tagCount <= 0) {
            return taggedUsers;
        }

        String[] mockUsers = {
                "zinza",
                "runner_kim",
                "Ongcheon1004",
                "neo_runner",
                "pace_master"
        };

        int count = Math.min(tagCount, mockUsers.length);

        for (int i = 0; i < count; i++) {
            taggedUsers.add(mockUsers[i]);
        }

        return taggedUsers;
    }
}