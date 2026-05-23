package com.neostride.wear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WearLocationService extends Service {

    public static final String ACTION_LOCATION_UPDATE = "com.neostride.wear.LOCATION_UPDATE";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_PERMISSION_DENIED = "permission_denied";
    public static final String EXTRA_ACCURACY = "accuracy";

    private static final String CHANNEL_ID = "wear_running_channel";
    private LocationManager locationManager;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            float acc = location.hasAccuracy() ? location.getAccuracy() : -1f;

            Intent intent = new Intent(ACTION_LOCATION_UPDATE);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
            intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
            intent.putExtra(EXTRA_TIME, System.currentTimeMillis());
            intent.putExtra(EXTRA_ACCURACY, acc);
            sendBroadcast(intent);
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NeoStride 러닝 중")
                .setContentText("GPS 측정 중...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        startForeground(1, notification);
        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000, 5f,
                    locationListener, Looper.getMainLooper()
            );
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000, 5f,
                        locationListener, Looper.getMainLooper()
                );
            }
        } catch (SecurityException e) {
            Intent intent = new Intent(ACTION_LOCATION_UPDATE);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_PERMISSION_DENIED, true);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "러닝 GPS", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}
