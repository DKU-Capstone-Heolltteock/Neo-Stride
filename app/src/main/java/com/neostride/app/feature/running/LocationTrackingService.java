package com.neostride.app.feature.running;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.neostride.app.R;
import com.neostride.app.activity.MainActivity;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "running_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static NotificationManager notificationManager;

    public interface LocationListener {
        void onLocationReceived(Location location);
    }

    private static LocationListener locationListener;

    public static void setLocationListener(LocationListener listener) {
        locationListener = listener;
    }

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PowerManager.WakeLock wakeLock; // 화면 꺼져도 CPU 유지

    // 채널 생성 + 초기 알림 즉시 표시 (서비스 시작 전에도 호출 가능)
    public static void postImmediateNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        // 채널이 없을 수 있으므로 여기서도 생성
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "달리기 기록 중", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("화면이 꺼져도 GPS 기록이 계속됩니다.");
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Neo-Stride 달리기 기록 중")
                .setContentText("0m  |  00:00  |  --'--\"")
                .setSmallIcon(R.drawable.ic_running)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi)
                .build();

        nm.notify(NOTIFICATION_ID, notification);
        notificationManager = nm; // 이후 updateNotification에서도 사용
    }

    // RunningFragment에서 매 초 호출해서 알림 갱신
    public static void updateNotification(Context context, String time, String distance, String pace) {
        if (notificationManager == null) {
            // 서비스가 아직 안 떴어도 즉시 표시
            postImmediateNotification(context);
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Neo-Stride 달리기 기록 중")
                .setContentText(distance + "  |  " + time + "  |  " + pace)
                .setSmallIcon(R.drawable.ic_running)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        notificationManager = getSystemService(NotificationManager.class);

        // WakeLock 획득 - 배터리 최적화가 서비스를 죽이지 못하게 함
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NeoStride:RunningTracker"
        );
        wakeLock.acquire(3 * 60 * 60 * 1000L); // 최대 3시간 (일반 러닝 초과 시간)

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationListener == null) return;
                for (Location location : locationResult.getLocations()) {
                    locationListener.onLocationReceived(location);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startLocationUpdates();
        return START_STICKY; // 시스템이 종료해도 자동 재시작
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        locationListener = null;
    }

    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(1000)
                    .build();
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "달리기 기록 중",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("화면이 꺼져도 GPS 기록이 계속됩니다.");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Neo-Stride 달리기 기록 중")
                .setContentText("화면이 꺼져도 GPS가 계속 기록됩니다.")
                .setSmallIcon(R.drawable.ic_running)
                .setOngoing(true)
                .build();
    }
}
