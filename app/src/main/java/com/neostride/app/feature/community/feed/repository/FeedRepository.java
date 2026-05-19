package com.neostride.app.feature.community.feed.repository;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.neostride.app.common.network.ApiClient;
//import com.neostride.app.common.network.MockApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.feed.api.FeedApi;
import com.neostride.app.feature.community.feed.model.FeedDetailResponse;
import com.neostride.app.feature.community.feed.model.FeedResponse;
import com.neostride.app.feature.community.feed.model.FeedUploadRequest;
import com.neostride.app.feature.community.feed.model.TagUser;
import com.neostride.app.feature.community.feed.model.FeedLikeResponse;
import com.neostride.app.feature.community.feed.model.FeedBookmarkResponse;
import com.neostride.app.feature.community.feed.model.FeedCommentRequest;
import com.neostride.app.feature.community.feed.model.FeedCommentResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * 피드 관련 데이터 처리를 담당하는 Repository 클래스임
 * 실제 서버 API 또는 Mock API를 통해 피드 목록 조회, 피드 상세 조회, 피드 업로드를 처리함
 */
public class FeedRepository {

    private final FeedApi feedApi;

    // 로그인한 사용자 ID를 가져오기 위해 Context를 저장함
    private final Context context;

    /*
     * FeedRepository 생성자임
     * ApiClient 또는 MockApiClient를 통해 FeedApi 객체를 생성함
     * Context는 TokenManager에서 로그인한 사용자 ID를 가져오기 위해 사용함
     */
    public FeedRepository(Context context) {
        this.context = context.getApplicationContext();

        // 실제 서버 연결 시 사용함
        feedApi = ApiClient.getInstance().create(FeedApi.class);

        // 개발 중 Mock 서버 테스트용으로 사용함
        /* push/merge 전에는 실제 서버용 ApiClient로 되돌리는 것을 권장함
        feedApi = MockApiClient.getInstance().create(FeedApi.class);*/
    }

    /*
     * 피드 목록을 조회하는 함수임
     * GET /api/feeds 요청에는 X-User-Id 헤더가 필요함
     */
    public void getFeedList(RepositoryCallback<List<FeedResponse>> callback) {

        // 현재 로그인한 사용자 ID를 가져옴
        int userId = TokenManager.getUserId(context);

        // FeedApi의 getFeedList 함수에 X-User-Id 헤더 값으로 userId를 전달함
        feedApi.getFeedList((long) userId).enqueue(new Callback<List<FeedResponse>>() {
            @Override
            public void onResponse(
                    Call<List<FeedResponse>> call,
                    Response<List<FeedResponse>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<FeedResponse>> call,
                    Throwable t
            ) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 상세 정보를 조회하는 함수임
     * GET /api/feeds/{feedId} 요청에는 X-User-Id 헤더가 필요함
     * 상세 화면 전용 FeedDetailResponse를 반환함
     */
    public void getFeedDetail(
            Long feedId,
            RepositoryCallback<FeedDetailResponse> callback
    ) {
        int userId = TokenManager.getUserId(context);

        feedApi.getFeedDetail((long) userId, feedId).enqueue(new Callback<FeedDetailResponse>() {
            @Override
            public void onResponse(
                    Call<FeedDetailResponse> call,
                    Response<FeedDetailResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<FeedDetailResponse> call,
                    Throwable t
            ) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 좋아요 토글 API를 호출하는 함수임
     * 성공 시 좋아요 여부와 최신 좋아요 수를 반환함
     */
    public void toggleFeedLike(
            Long feedId,
            RepositoryCallback<FeedLikeResponse> callback
    ) {
        feedApi.toggleFeedLike(feedId).enqueue(new Callback<FeedLikeResponse>() {
            @Override
            public void onResponse(
                    Call<FeedLikeResponse> call,
                    Response<FeedLikeResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 좋아요 처리에 실패했습니다");
                }
            }

            @Override
            public void onFailure(Call<FeedLikeResponse> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 수정 API를 호출하는 함수임 (PUT /api/community/feeds/{feedId})
     */
    public void updateFeed(
            Long feedId,
            FeedUploadRequest request,
            RepositoryCallback<FeedResponse> callback
    ) {
        feedApi.updateFeed(feedId, request).enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call,
                                   Response<FeedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 삭제 API를 호출하는 함수임
     */
    public void deleteFeed(Long feedId, RepositoryCallback<Boolean> callback) {
        feedApi.deleteFeed(feedId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 북마크 토글 API를 호출하는 함수임
     * 성공 시 북마크 여부를 반환함
     */
    public void toggleFeedBookmark(
            Long feedId,
            RepositoryCallback<FeedBookmarkResponse> callback
    ) {
        feedApi.toggleFeedBookmark(feedId).enqueue(new Callback<FeedBookmarkResponse>() {
            @Override
            public void onResponse(
                    Call<FeedBookmarkResponse> call,
                    Response<FeedBookmarkResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 북마크 처리에 실패했습니다");
                }
            }

            @Override
            public void onFailure(Call<FeedBookmarkResponse> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 댓글 작성 API를 호출하는 함수임
     * 성공 시 서버가 생성한 댓글 정보를 반환함
     */
    public void createFeedComment(
            Long feedId,
            FeedCommentRequest request,
            RepositoryCallback<FeedCommentResponse> callback
    ) {
        feedApi.createFeedComment(feedId, request).enqueue(new Callback<FeedCommentResponse>() {
            @Override
            public void onResponse(
                    Call<FeedCommentResponse> call,
                    Response<FeedCommentResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("피드 댓글 작성에 실패했습니다");
                }
            }

            @Override
            public void onFailure(Call<FeedCommentResponse> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 댓글 수정 API 호출
     */
    public void updateFeedComment(
            Long feedId,
            Long commentId,
            FeedCommentRequest request,
            RepositoryCallback<FeedCommentResponse> callback
    ) {
        feedApi.updateFeedComment(feedId, commentId, request).enqueue(new Callback<FeedCommentResponse>() {
            @Override
            public void onResponse(Call<FeedCommentResponse> call, Response<FeedCommentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<FeedCommentResponse> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 피드 댓글 삭제 API 호출
     */
    public void deleteFeedComment(Long feedId, Long commentId, RepositoryCallback<Boolean> callback) {
        feedApi.deleteFeedComment(feedId, commentId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) callback.onSuccess(true);
                else callback.onError("오류 코드 " + response.code());
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }


    /*
     * 피드 업로드를 처리하는 함수임
     * 텍스트 필드는 @PartMap 으로, 이미지(경로 지도 + 피드 사진)는 MultipartBody.Part 리스트로 전송함
     * 마이페이지 프로필 이미지 업로드와 동일한 multipart/form-data 방식임
     */
    public void uploadFeed(
            FeedUploadRequest request,
            RepositoryCallback<FeedResponse> callback
    ) {
        // ── 텍스트 필드 ────────────────────────────────────────────────────────
        Map<String, RequestBody> fields = new HashMap<>();
        fields.put("title",       toPlainBody(request.getTitle()));
        fields.put("content",     toPlainBody(request.getContent()));
        fields.put("privacy",     toPlainBody(request.getPrivacy()));
        fields.put("mapVisible",  toPlainBody(String.valueOf(request.isMapVisible())));
        fields.put("distance",    toPlainBody(String.valueOf(request.getDistance())));
        fields.put("runningTime", toPlainBody(request.getRunningTime()));
        fields.put("pace",        toPlainBody(request.getPace()));
        fields.put("tagCount",    toPlainBody(String.valueOf(request.getTagCount())));

        // taggedUserIds → JSON 배열 문자열로 직렬화
        if (request.getTaggedUserIds() != null && !request.getTaggedUserIds().isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < request.getTaggedUserIds().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(request.getTaggedUserIds().get(i));
            }
            sb.append("]");
            fields.put("taggedUserIds", toPlainBody(sb.toString()));
        }

        // ── 이미지 파트 ────────────────────────────────────────────────────────
        List<MultipartBody.Part> imageParts = new ArrayList<>();

        // 경로 지도 이미지 (file:// URI — 캐시 디렉터리에 저장된 PNG)
        if (request.isMapVisible()
                && request.getRouteMapImageUri() != null
                && !request.getRouteMapImageUri().trim().isEmpty()) {
            MultipartBody.Part routeMapPart =
                    uriToMultipartPart(context, request.getRouteMapImageUri(), "routeMapImage");
            if (routeMapPart != null) imageParts.add(routeMapPart);
        }

        // 피드 사진 (content:// URI — 갤러리에서 선택)
        if (request.getImageUrls() != null) {
            for (String uriString : request.getImageUrls()) {
                if (uriString == null || uriString.trim().isEmpty()) continue;
                MultipartBody.Part part = uriToMultipartPart(context, uriString, "images");
                if (part != null) imageParts.add(part);
            }
        }

        // ── API 호출 ────────────────────────────────────────────────────────────
        feedApi.uploadFeed(fields, imageParts).enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(
                    Call<FeedResponse> call,
                    Response<FeedResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<FeedResponse> call,
                    Throwable t
            ) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }

    // ── 헬퍼 메서드 ─────────────────────────────────────────────────────────────

    /*
     * 문자열을 text/plain RequestBody 로 변환함
     */
    private static RequestBody toPlainBody(String value) {
        return RequestBody.create(
                value != null ? value : "",
                MediaType.parse("text/plain")
        );
    }

    /*
     * 로컬 URI (file:// 또는 content://) 를 MultipartBody.Part 로 변환함
     * http:// 등 원격 URI 는 null 을 반환함
     */
    private static MultipartBody.Part uriToMultipartPart(
            Context context, String uriString, String partName) {
        try {
            Uri uri = Uri.parse(uriString);
            String scheme = uri.getScheme();
            byte[] bytes;
            String filename;

            if ("file".equals(scheme)) {
                // file:// — 캐시 디렉터리에 저장된 경로 지도 PNG
                File file = new File(uri.getPath());
                filename = file.getName();
                FileInputStream fis = new FileInputStream(file);
                bytes = readAllBytes(fis);
                fis.close();
            } else if ("content".equals(scheme)) {
                // content:// — 갤러리에서 선택한 사진
                filename = queryDisplayName(context, uri);
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) return null;
                bytes = readAllBytes(is);
                is.close();
            } else {
                // http:// 등 원격 URI 는 지원하지 않음
                android.util.Log.w("FeedRepository",
                        "uriToMultipartPart: unsupported scheme — " + uriString);
                return null;
            }

            RequestBody body = RequestBody.create(
                    bytes,
                    MediaType.parse(guessMimeType(filename))
            );
            return MultipartBody.Part.createFormData(partName, filename, body);

        } catch (Exception e) {
            android.util.Log.e("FeedRepository",
                    "uriToMultipartPart failed: " + uriString, e);
            return null;
        }
    }

    /*
     * InputStream 을 byte 배열로 읽어 반환함
     */
    private static byte[] readAllBytes(InputStream is) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    /*
     * content:// URI 에서 파일명을 조회함
     */
    private static String queryDisplayName(Context context, Uri uri) {
        String result = "image.jpg";
        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /*
     * 파일명 확장자를 기반으로 MIME 타입을 추측함
     */
    private static String guessMimeType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    /*
     * 태그할 친구 목록을 조회하는 함수임
     * FeedApi의 친구 목록 API를 호출하고 결과를 화면으로 전달함
     */
    public void getFriendList(
            RepositoryCallback<List<TagUser>> callback
    ) {
        int userId = TokenManager.getUserId(context);

        feedApi.getFriendList(
                (long) userId,
                "ACCEPTED"
        ).enqueue(new Callback<List<TagUser>>() {
            @Override
            public void onResponse(
                    Call<List<TagUser>> call,
                    Response<List<TagUser>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<List<TagUser>> call,
                    Throwable t
            ) {
                callback.onError("서버에 연결할 수 없습니다");
            }
        });
    }


    /*
     * 태그된 사용자 목록을 조회하는 함수임
     * 현재 서버에 태그 조회 API가 없으면 빈 리스트를 반환함
     */
    public void getTaggedUsers(
            Long feedId,
            RepositoryCallback<List<String>> callback
    ) {
        callback.onSuccess(new ArrayList<>());
    }

    /*
     * Repository 작업 결과를 화면으로 전달하기 위한 Callback 인터페이스임
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }


}
