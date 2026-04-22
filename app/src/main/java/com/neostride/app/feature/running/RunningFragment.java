package com.neostride.app.feature.running;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.neostride.app.R;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private CardView btnStart, btnStop, btnMyLocation; // 커스텀 버튼 변수 추가
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_running, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ── 뷰 바인딩 ──
        btnStart = view.findViewById(R.id.btn_start);
        btnStop = view.findViewById(R.id.btn_stop);
        btnMyLocation = view.findViewById(R.id.btn_my_location); // 커스텀 버튼 연결

        // ── 클릭 리스너 설정 ──
        btnStart.setOnClickListener(v -> startTracking());
        btnStop.setOnClickListener(v -> stopTracking());

        // 커스텀 내 위치 버튼 클릭 시 카메라 이동
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation(true));

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // ── 다크 스타일 적용 ──
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        // ── 구글 기본 UI 설정 ──
        // 오버레이에 가려지는 기본 버튼은 끄고, 우리 버튼을 사용합니다.
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // ── 권한 체크 후 초기 카메라 이동 ──
        checkPermissionAndMoveCamera();
    }

    private void checkPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            moveToCurrentLocation(false); // 처음 로드 시에는 애니메이션 없이 슥 이동
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // ── 현재 위치로 카메라를 이동시키는 공용 메소드 ──
    private void moveToCurrentLocation(boolean isAnimate) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (isAnimate) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionAndMoveCamera();
            } else {
                Toast.makeText(getContext(), "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTracking() {
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "러닝 측정을 시작합니다!", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        btnStop.setVisibility(View.GONE);
        btnStart.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "러닝이 종료되었습니다.", Toast.LENGTH_SHORT).show();
    }
}