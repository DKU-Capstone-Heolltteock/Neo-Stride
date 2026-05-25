package com.neostride.app.feature.community.tip.repository;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.neostride.app.common.network.ApiClient;
import com.neostride.app.feature.community.tip.api.TipApi;
import com.neostride.app.feature.community.tip.model.TipDetailResponse;
import com.neostride.app.feature.community.tip.model.TipResponse;
import com.neostride.app.feature.community.tip.model.TipUploadRequest;
import com.neostride.app.feature.community.tip.model.TipUploadResponse;
import com.neostride.app.feature.community.tip.model.TipBookmarkResponse;
import com.neostride.app.feature.community.tip.model.TipCommentRequest;
import com.neostride.app.feature.community.tip.model.TipCommentResponse;
import com.neostride.app.feature.community.tip.model.TipLikeResponse;

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
 * 팁 Repository 클래스임
 * 팁 업로드, 팁 목록 조회, 팁 상세 조회 API 호출을 담당함
 */
public class TipRepository {

    private static final String TAG = "TipRepository";

    private final TipApi tipApi;

    /*
     * Context — uploadTip 에서 content:// URI 를 읽을 때 필요함
     * null 허용: uploadTip 을 호출하지 않는 곳은 기존 no-arg 생성자를 그대로 사용 가능
     */
    private final Context context;

    /*
     * 기존 호출부 호환용 no-arg 생성자 (uploadTip 외의 모든 기능에서 사용)
     */
    public TipRepository() {
        this(null);
    }

    /*
     * 이미지 업로드가 필요한 경우 Context 를 전달받는 생성자
     * TipUploadActivity 에서 new TipRepository(this) 로 생성함
     */
    public TipRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        tipApi = ApiClient
                .getInstance()
                .create(TipApi.class);
    }

    /*
     * 팁 업로드 함수임
     * 텍스트 필드는 @PartMap 으로, 이미지(GPS 경로 지도 + 첨부 사진)는 MultipartBody.Part 리스트로 전송함
     * 피드 업로드, 마이페이지 프로필 이미지와 동일한 multipart/form-data 방식임
     */
    public void uploadTip(
            TipUploadRequest request,
            TipUploadCallback callback
    ) {
        // ── 텍스트 필드 ─────────────────────────────────────────────────────────
        Map<String, RequestBody> fields = new HashMap<>();
        fields.put("category",      toPlainBody(request.getCategory()));
        fields.put("title",         toPlainBody(request.getTitle()));
        fields.put("content",       toPlainBody(request.getContent()));
        fields.put("gpsVisible",    toPlainBody(String.valueOf(request.isGpsVisible())));
        fields.put("courseAddress", toPlainBody(request.getCourseAddress() != null ? request.getCourseAddress() : ""));

        // ── 이미지 파트 ──────────────────────────────────────────────────────────
        List<MultipartBody.Part> imageParts = new ArrayList<>();

        // GPS 경로 지도 이미지 (file:// URI)
        if (request.isGpsVisible()
                && request.getRouteMapImageUrl() != null
                && !request.getRouteMapImageUrl().trim().isEmpty()) {
            MultipartBody.Part routeMapPart =
                    uriToMultipartPart(context, request.getRouteMapImageUrl(), "routeMapImage");
            if (routeMapPart != null) imageParts.add(routeMapPart);
        }

        // 첨부 사진 (content:// URI)
        if (request.getImageUrls() != null) {
            for (String uriString : request.getImageUrls()) {
                if (uriString == null || uriString.trim().isEmpty()) continue;
                MultipartBody.Part part = uriToMultipartPart(context, uriString, "images");
                if (part != null) imageParts.add(part);
            }
        }

        // ── API 호출 ─────────────────────────────────────────────────────────────
        tipApi.uploadTip(fields, imageParts)
                .enqueue(new Callback<TipUploadResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipUploadResponse> call,
                            Response<TipUploadResponse> response
                    ) {
                        Log.d(TAG, "uploadTip response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipUploadResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "uploadTip onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────────────────────────

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
                // file:// — TipRecordSelectActivity 가 캐시에 저장한 경로 지도 PNG
                File file = new File(uri.getPath());
                filename = file.getName();
                FileInputStream fis = new FileInputStream(file);
                bytes = readAllBytes(fis);
                fis.close();
            } else if ("content".equals(scheme)) {
                // content:// — 갤러리에서 선택한 사진
                if (context == null) return null;
                filename = queryDisplayName(context, uri);
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) return null;
                bytes = readAllBytes(is);
                is.close();
            } else {
                Log.w(TAG, "uriToMultipartPart: unsupported scheme — " + uriString);
                return null;
            }

            RequestBody body = RequestBody.create(
                    bytes,
                    MediaType.parse(guessMimeType(filename))
            );
            return MultipartBody.Part.createFormData(partName, filename, body);

        } catch (Exception e) {
            Log.e(TAG, "uriToMultipartPart failed: " + uriString, e);
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
     * 팁 목록 조회 함수임
     * 서버에서 팁 게시글 목록을 가져옴
     */
    public void getTips(
            TipListCallback callback
    ) {
        tipApi.getTips()
                .enqueue(new Callback<List<TipResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<TipResponse>> call,
                            Response<List<TipResponse>> response
                    ) {
                        Log.d(TAG, "getTips response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<TipResponse>> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "getTips onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 내가 북마크한 팁 목록 조회 함수임
     */
    public void getBookmarkedTips(Callback<List<TipResponse>> callback) {
        tipApi.getBookmarkedTips().enqueue(callback);
    }

    /*
     * 내가 좋아요한 팁 목록 조회 함수임
     */
    public void getLikedTips(Callback<List<TipResponse>> callback) {
        tipApi.getLikedTips().enqueue(callback);
    }

    /*
     * 내가 댓글 단 팁 목록 조회 함수임
     */
    public void getCommentedTips(Callback<List<TipResponse>> callback) {
        tipApi.getCommentedTips().enqueue(callback);
    }

    /*
     * 팁 상세 조회 함수임
     * tipId를 기준으로 서버에서 팁 상세 정보를 가져옴
     */
    public void getTipDetail(
            Long tipId,
            TipDetailCallback callback
    ) {
        tipApi.getTipDetail(tipId)
                .enqueue(new Callback<TipDetailResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipDetailResponse> call,
                            Response<TipDetailResponse> response
                    ) {
                        Log.d(TAG, "getTipDetail response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipDetailResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "getTipDetail onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 팁 좋아요 토글 함수임
     */
    public void toggleTipLike(
            Long tipId,
            TipLikeCallback callback
    ) {
        tipApi.toggleTipLike(tipId)
                .enqueue(new Callback<TipLikeResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipLikeResponse> call,
                            Response<TipLikeResponse> response
                    ) {
                        Log.d(TAG, "toggleTipLike response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipLikeResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "toggleTipLike onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 팁 수정 함수임 (PUT /api/community/tips/{tipId})
     */
    public void updateTip(
            Long tipId,
            TipUploadRequest request,
            TipUploadCallback callback
    ) {
        tipApi.updateTip(tipId, request).enqueue(new Callback<TipUploadResponse>() {
            @Override
            public void onResponse(Call<TipUploadResponse> call,
                                   Response<TipUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TipUploadResponse> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 팁 삭제 함수임
     */
    public void deleteTip(Long tipId, TipDeleteCallback callback) {
        tipApi.deleteTip(tipId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("오류 코드 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    public interface TipDeleteCallback {
        void onSuccess();
        void onFailure(String message);
    }

    /*
     * 팁 북마크 토글 함수임
     */
    public void toggleTipBookmark(
            Long tipId,
            TipBookmarkCallback callback
    ) {
        tipApi.toggleTipBookmark(tipId)
                .enqueue(new Callback<TipBookmarkResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipBookmarkResponse> call,
                            Response<TipBookmarkResponse> response
                    ) {
                        Log.d(TAG, "toggleTipBookmark response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipBookmarkResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "toggleTipBookmark onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 팁 댓글 작성 함수임
     */
    public void createTipComment(
            Long tipId,
            String content,
            TipCommentCreateCallback callback
    ) {
        TipCommentRequest request = new TipCommentRequest(content);

        tipApi.createTipComment(tipId, request)
                .enqueue(new Callback<TipCommentResponse>() {
                    @Override
                    public void onResponse(
                            Call<TipCommentResponse> call,
                            Response<TipCommentResponse> response
                    ) {
                        Log.d(TAG, "createTipComment response code = " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<TipCommentResponse> call,
                            Throwable t
                    ) {
                        Log.e(TAG, "createTipComment onFailure = " + t.getMessage());
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 팁 댓글 수정 API 호출
     */
    public void updateTipComment(
            Long tipId,
            Long commentId,
            String content,
            TipCommentCreateCallback callback
    ) {
        TipCommentRequest request = new TipCommentRequest(content);
        tipApi.updateTipComment(tipId, commentId, request)
                .enqueue(new Callback<TipCommentResponse>() {
                    @Override
                    public void onResponse(Call<TipCommentResponse> call, Response<TipCommentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("오류 코드 " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TipCommentResponse> call, Throwable t) {
                        callback.onFailure("서버에 연결할 수 없습니다");
                    }
                });
    }

    /*
     * 팁 댓글 삭제 API 호출
     */
    public void deleteTipComment(Long tipId, Long commentId, TipDeleteCallback callback) {
        tipApi.deleteTipComment(tipId, commentId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) callback.onSuccess();
                else callback.onFailure("오류 코드 " + response.code());
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                callback.onFailure("서버에 연결할 수 없습니다");
            }
        });
    }

    /*
     * 팁 업로드 콜백 인터페이스임
     */
    public interface TipUploadCallback {
        void onSuccess(TipUploadResponse response);

        void onFailure(String message);
    }

    /*
     * 팁 목록 조회 콜백 인터페이스임
     */
    public interface TipListCallback {
        void onSuccess(List<TipResponse> response);

        void onFailure(String message);
    }

    /*
     * 팁 상세 조회 콜백 인터페이스임
     */
    public interface TipDetailCallback {
        void onSuccess(TipDetailResponse response);

        void onFailure(String message);
    }

    /*
     * 팁 좋아요 토글 콜백 인터페이스임
     */
    public interface TipLikeCallback {
        void onSuccess(TipLikeResponse response);

        void onFailure(String message);
    }

    /*
     * 팁 북마크 토글 콜백 인터페이스임
     */
    public interface TipBookmarkCallback {
        void onSuccess(TipBookmarkResponse response);

        void onFailure(String message);
    }

    /*
     * 팁 댓글 작성 콜백 인터페이스임
     */
    public interface TipCommentCreateCallback {
        void onSuccess(TipCommentResponse response);

        void onFailure(String message);
    }
}
