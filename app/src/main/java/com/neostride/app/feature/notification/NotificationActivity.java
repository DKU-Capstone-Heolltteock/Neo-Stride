package com.neostride.app.feature.notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.badge.BadgeActivity;
import com.neostride.app.feature.community.common.util.TimeFormatter;
import com.neostride.app.feature.community.feed.FeedDetailActivity;
import com.neostride.app.feature.community.tip.TipDetailActivity;
import com.neostride.app.feature.community.friend.FriendActivity;
import com.neostride.app.feature.notification.model.NotificationResponse;
import com.neostride.app.feature.notification.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * 알림 목록 화면 Activity 클래스임
 *
 * 읽음 처리 방식:
 *   - 서버의 read 값이 true → 이미 확인한 알림 (회색)
 *   - 앱 세션 내 viewedIds 에 ID가 있으면 → 이번 세션에서 본 알림 (회색)
 *   - 위 두 경우 모두 해당 없으면 → 새 알림 (형광)
 *   - 화면을 벗어날 때(onPause) 현재 목록의 ID를 전부 viewedIds 에 기록함
 *     → 다음에 다시 열면 해당 알림들이 회색으로 표시됨
 */
public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    /*
     * 앱 세션 동안 이 화면에서 한 번이라도 본 알림 ID를 기억하는 Set임
     * static 이므로 앱이 종료되기 전까지 유지됨
     */
    private static final Set<Long> viewedIds = new HashSet<>();

    private RecyclerView rv;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private final List<NotificationItem> items = new ArrayList<>();
    private NotificationRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        rv      = findViewById(R.id.rv_notifications);
        tvEmpty = findViewById(R.id.tv_empty);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(items);
        adapter.setOnItemClickListener(this::handleNotificationClick);
        rv.setAdapter(adapter);

        repository = new NotificationRepository(this);
        // 알림 화면 진입 시 전체 읽음 처리 → CommunityActivity 복귀 때 빨간 점 사라짐
        repository.markAllRead(new NotificationRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { /* 읽음 처리 완료 */ }
            @Override public void onError(String message) { /* 실패해도 목록 조회는 진행 */ }
        });
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            loadNotifications();
        }
    }

    /*
     * 화면을 벗어날 때 현재 목록의 모든 ID를 viewedIds 에 기록함
     * 다음에 이 화면을 열면 해당 알림들이 회색으로 표시됨
     */
    @Override
    protected void onPause() {
        super.onPause();
        for (NotificationItem item : items) {
            if (item.notificationId != null) {
                viewedIds.add(item.notificationId);
            }
        }
    }

    /*
     * 서버에서 알림 목록을 조회하는 함수임
     * 서버 read 값 또는 viewedIds 에 포함된 경우 read=true 로 처리함
     */
    private void loadNotifications() {
        repository.getNotifications(new NotificationRepository.RepositoryCallback<List<NotificationResponse>>() {
            @Override
            public void onSuccess(List<NotificationResponse> response) {
                Log.d(TAG, "loadNotifications success, count=" + response.size());

                items.clear();
                for (NotificationResponse r : response) {
                    boolean read = r.isRead()
                            || (r.getNotificationId() != null && viewedIds.contains(r.getNotificationId()));

                    items.add(new NotificationItem(
                            r.getNotificationId(),
                            r.getType(),
                            r.getMessage(),
                            TimeFormatter.format(r.getCreatedAt()),
                            r.getTargetId(),
                            read
                    ));
                }

                updateEmptyState();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "loadNotifications error: " + message);
                Toast.makeText(NotificationActivity.this,
                        "알림 오류 : " + message,
                        Toast.LENGTH_SHORT).show();

                items.clear();
                updateEmptyState();
                adapter.notifyDataSetChanged();
            }
        });
    }

    /*
     * 알림 타입에 따라 해당 화면으로 이동하는 함수임
     *
     * FEED_TAG       : 태그된 피드 상세 화면 (targetId = feedId)
     * FRIEND_REQUEST : 친구창 받은 요청 탭
     * GRADE          : 배지 화면
     * COMMENT        : 댓글 달린 피드 상세 화면 (targetId = feedId)
     * LIKE           : 좋아요 받은 피드 상세 화면 (targetId = feedId)
     */
    private void handleNotificationClick(NotificationItem item) {
        switch (item.type) {

            case NotificationItem.TYPE_FEED_TAG:
            case NotificationItem.TYPE_FEED_COMMENT:
            case NotificationItem.TYPE_FEED_LIKE:
                if (item.targetId != null) {
                    Intent feedIntent = new Intent(this, FeedDetailActivity.class);
                    feedIntent.putExtra("feedId", item.targetId);
                    startActivity(feedIntent);
                }
                break;

            case NotificationItem.TYPE_TIP_COMMENT:
            case NotificationItem.TYPE_TIP_LIKE:
                if (item.targetId != null) {
                    Intent tipIntent = new Intent(this, TipDetailActivity.class);
                    tipIntent.putExtra("tipId", item.targetId);
                    startActivity(tipIntent);
                }
                break;

            case NotificationItem.TYPE_FRIEND_REQUEST:
                Intent friendIntent = new Intent(this, FriendActivity.class);
                friendIntent.putExtra("EXTRA_OPEN_TAB", "received");
                startActivity(friendIntent);
                break;

            case NotificationItem.TYPE_GRADE:
                startActivity(new Intent(this, BadgeActivity.class));
                break;
        }
    }

    private void updateEmptyState() {
        if (items.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }
}
