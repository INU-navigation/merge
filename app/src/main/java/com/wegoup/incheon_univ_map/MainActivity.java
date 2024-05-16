package com.wegoup.incheon_univ_map;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.data.kml.KmlLayer;
import com.wegoup.pdr.PdrActivity;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String API_KEY = "AIzaSyDJfEWw-TdLIL4lrp-lrM1YNsH8Eu-fQK4";
    private MapView mapView;
    private GoogleMap googleMap;
    private Spinner spinner;
    private KmlLayer kmlLayer;
    private Marker currentLocationMarker; // 현재 위치를 표시하기 위한 마커 변수
    private AutocompleteSupportFragment search_fragment;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;
    int currentLevel = -1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        locationInit();

        spinner = findViewById(R.id.spinner);
        spinner.setVisibility(View.GONE); // 초기에는 스피너를 숨깁니다.
        setupSpinner();

        // Places API 클라이언트 설정
        Places.initialize(getApplicationContext(), API_KEY); // YOUR_API_KEY를 발급받은 API 키로 대체
        PlacesClient placesClient = Places.createClient(this);

        // 장소검색 자동완성 프래그먼트 초기화
        search_fragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        search_fragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        search_fragment.setHint("장소를 검색하세요");
        search_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // 선택된 장소에서 정보 추출
                String placeName = place.getName();
                LatLng placeLatLng = place.getLatLng();

                if (placeLatLng != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(placeLatLng, 15);
                    googleMap.animateCamera(cameraUpdate);

                    // 지도에 마커 추가
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(placeLatLng)
                            .title(placeName);
                    googleMap.addMarker(markerOptions);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("AutoCompleteFragment", "An error occurred: " + status);
            }
        });



        ImageButton myLocationButton = findViewById(R.id.Pdr);

        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 사용자의 위치를 가져옴
                if (checkLocationPermission()) {
                    fusedLocationClient.getLastLocation()
                            .addOnCompleteListener(MainActivity.this, task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Location location = task.getResult();
                                    double currentLatitude = location.getLatitude();
                                    double currentLongitude = location.getLongitude();
                                    float currentZoomLevel = googleMap.getCameraPosition().zoom;

                                    // Intent에 데이터를 추가하여 PdrActivity로 전달
                                    Intent intent = new Intent(MainActivity.this, PdrActivity.class);
                                    intent.putExtra("latitude", currentLatitude);
                                    intent.putExtra("longitude", currentLongitude);
                                    intent.putExtra("zoomLevel", currentZoomLevel);
                                    startActivity(intent);
                                } else {
                                    // 위치를 가져올 수 없을 때 처리할 내용 추가
                                    Log.e("Location Error", "Unable to get current location");
                                    Toast.makeText(MainActivity.this, "현재 위치를 가져올 수 없습니다!", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    requestLocationPermission();
                }
            }
        });

        ImageButton curLocationButton = findViewById(R.id.current_location_button);
        curLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAndUpdateLocation();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // 맵에 마커 추가
        LatLng markerLocation = new LatLng(37.3752495, 126.6321764);
        googleMap.addMarker(new MarkerOptions().position(markerLocation).title("인천대학교 송도캠퍼스"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(markerLocation));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerLocation, 15.0f));

        googleMap.setOnCameraIdleListener(() -> {

            LatLng mapCenter = googleMap.getCameraPosition().target;

            if (googleMap.getCameraPosition().zoom >= 18.5 && isInsideUniv(mapCenter)) {
                spinner.setVisibility(View.VISIBLE);
                int selectedLevel = spinner.getSelectedItemPosition();
                showFeaturesForLevel(selectedLevel);
            } else {
                spinner.setVisibility(View.GONE);
                if (kmlLayer != null) {
                    kmlLayer.removeLayerFromMap();
                }
            }
        });
    }

    private boolean isInsideUniv(LatLng mapCenter) {
        // 건물의 서쪽, 동쪽, 남쪽, 북쪽 경계를 설정합니다.
        LatLng southwest = new LatLng(37.370589, 126.625092); // 건물 서쪽 남쪽 경계
        LatLng northeast = new LatLng(37.379550, 126.638119); // 건물 동쪽 북쪽 경계

        // 지도의 중심 위치가 특정 영역 안에 있는지 확인합니다.
        return (mapCenter.latitude >= southwest.latitude &&
                mapCenter.latitude <= northeast.latitude &&
                mapCenter.longitude >= southwest.longitude &&
                mapCenter.longitude <= northeast.longitude);
    }

    private void showFeaturesForLevel(int selectedLevel) {
        try {
            if (kmlLayer != null) {
                kmlLayer.removeLayerFromMap();
            }
            String kmlFilePath = "F" + (selectedLevel + 1) + ".kml";
            kmlLayer = new KmlLayer(googleMap, getResources().getAssets().open(kmlFilePath), getApplicationContext());
            kmlLayer.addLayerToMap();

        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.level_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // 스피너가 초기에 "X"를 선택하도록 설정합니다.
        spinner.setSelection(adapter.getPosition("X"));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 5) {
                    // 이전에 선택한 레벨과 현재 선택한 레벨이 다를 때만 실행합니다.
                    int selectedLevel = position + 1;
                    if (selectedLevel != currentLevel) {
                        currentLevel = selectedLevel;
                        showFeaturesForLevel(selectedLevel);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                googleMap.clear();
            }
        });
    }

    private void locationInit() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initLocationRequest();
        getLastLocation();
    }

    private void initLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void getLastLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            handleLocationResult(task.getResult());
                        } else {
                            requestAndUpdateLocation();
                        }
                    });
        } else {
            requestLocationPermission();
        }
    }

    private void requestAndUpdateLocation() {
        if (checkLocationPermission()) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    handleLocationResult(locationResult.getLastLocation());
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            requestLocationPermission();
        }
    }

    private void handleLocationResult(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            LatLng currentLocation = new LatLng(latitude, longitude);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

            // 현재 위치를 표시하는 마커 생성 및 추가
            if (currentLocationMarker != null) {
                currentLocationMarker.remove(); // 기존 마커가 있으면 삭제
            }
            currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.cur_location))); // 아이콘 설정
        }
        // 깜빡이는 애니메이션 적용
        blinkMarker(currentLocationMarker);
    }

    private void blinkMarker(Marker marker) {
        final Handler handler = new Handler();
        final long startTime = SystemClock.uptimeMillis();
        final long duration = 1000; // 깜빡이는 주기 (밀리초)

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                // 알파값을 조절하여 깜빡이는 효과를 줍니다.
                marker.setAlpha((float) Math.abs(Math.sin(t * Math.PI)));

                // 지속적으로 반복하도록 합니다.
                if (elapsed < duration) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private boolean checkLocationPermission() {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST_LOCATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}