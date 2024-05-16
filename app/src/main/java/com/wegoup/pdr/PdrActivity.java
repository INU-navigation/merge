package com.wegoup.pdr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.data.kml.KmlLayer;
import com.wegoup.incheon_univ_map.IsInsidePlace;
import com.wegoup.incheon_univ_map.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdrActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener {
    private MapView mapView;
    private GoogleMap googleMap;
    private IsInsidePlace isInsidePlace = new IsInsidePlace();
    private KmlLayer kmlLayer;
    private Spinner spinner;
    private Spinner spinnerStartLocation;
    private Spinner spinnerDestinationLocation;
    private ImageButton bt_search;
    private ImageButton bt_searchOff;
    private Marker marker;
    LatLng markerPosition;  // 사용자 선택 마커 위치
    private LatLng newLatLng;   // computeNextStep(다음 위치 계산)
    private LatLng lastKnownLatLng = null;
    private Marker currentLocationMarker; // 현재 위치를 표시하기 위한 마커 변수
    public static StepDetectionHandler sdh;
    public static StepPositioningHandler sph;
    public static DeviceAttitudeHandler dah;
    TextView totalDist;
    private boolean isFirstStep = true;
    private Location markerLocation;
    private Polyline userPath; // 실시간 움직임을 표시할 Polyline
    private Polyline floorPath; // 실시간 움직임을 표시할 Polyline
    private List<Polyline> floorPaths = new ArrayList<>(5);
    private double totalDistance = 0.0;
    private boolean isMapZoomedEnough = false;
    private boolean isMarkerDraggable = false;
    private int currentLevel = -1;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        spinner = findViewById(R.id.spinner2);
        spinnerStartLocation = findViewById(R.id.spinnerStartLocation);
        spinnerDestinationLocation = findViewById(R.id.spinnerDestinationLocation);
        bt_search = findViewById(R.id.bt_search);
        bt_searchOff = findViewById(R.id.bt_searchOff);
        spinner.setVisibility(View.GONE); // Initially hide spinner
        spinnerStartLocation.setVisibility(View.GONE);
        spinnerDestinationLocation.setVisibility(View.GONE);
        bt_search.setVisibility(View.GONE);
        bt_searchOff.setVisibility(View.GONE);

        totalDist = findViewById(R.id.distanceView);

        setupSpinner();

        spinnerStartLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 선택된 항목을 가져와서 변수에 저장
                //selectedStartLocation = parent.getItemAtPosition(position).toString();
                // 저장된 값을 로그로 출력
                //Log.d(TAG, "Selected start location: " + selectedStartLocation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 스피너에서 아무 항목도 선택되지 않았을 때의 동작을 정의합니다.
            }
        });

        spinnerDestinationLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 선택된 항목을 가져와서 변수에 저장
                //selectedDestinationLocation = parent.getItemAtPosition(position).toString();
                // 저장된 값을 로그로 출력
                // Log.d(TAG, "Selected destination: " + selectedDestinationLocation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 스피너에서 아무 항목도 선택되지 않았을 때의 동작을 정의합니다.
            }
        });

        // 버튼 설정
        ImageButton editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> {
            if (googleMap != null) {
                if (isInsidePlace.building7(markerPosition)) {
                if (marker != null && userPath != null) {
                    marker.remove();
                    userPath.remove(); userPath = null;
                    stopPDR();
                }

                if (lastKnownLatLng != null) {
                addMarker(lastKnownLatLng.latitude, lastKnownLatLng.longitude);
            } else {
                addInitialMarkerAtCurLocation();
                Log.d("marker", "marker가 있습니다.");
            }
                marker.setIcon(BitmapDescriptorFactory.defaultMarker());
                Toast.makeText(PdrActivity.this, "마커를 꾹 눌러 움직여주세요", Toast.LENGTH_LONG).show();
                marker.setDraggable(true);
                }
            }
        });

        ImageButton setButton = findViewById(R.id.set_button);
        setButton.setOnClickListener(v -> {
            if (userPath != null) {
                stopPDR();
                userPath.remove();
                userPath = null;
            }
            if (marker != null) {
                changeMarkerIcon();
                marker.setDraggable(false);
                googleMap.setOnMarkerDragListener(null);

                markerPosition = marker.getPosition();
                double latitude = markerPosition.latitude;
                double longitude = markerPosition.longitude;
                Log.d("loc", "lat : " + latitude + ", lon : " + longitude);

                markerLocation = new Location("");
                markerLocation.setLatitude(markerPosition.latitude);
                markerLocation.setLongitude(markerPosition.longitude);      // markerLocation == markerPosition (marker 고정 위치)
                Log.d("insideP", "markerPosition.lat = " + markerPosition.latitude + "markerPosition.lon = " + markerPosition.longitude);

                //floorPaths = new ArrayList<>();  // 각 층에서의 이동 경로를 저장할 리스트 초기화
                startPedestrianDetection(markerPosition);
            } else {
                Toast.makeText(PdrActivity.this, "현 위치(마커)를 먼저 설정해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        ImageButton curLocationButton = findViewById(R.id.current_location_button2);
        curLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkLocationPermission()) {
                    fusedLocationClient.getLastLocation()
                            .addOnCompleteListener(PdrActivity.this, task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Location location = task.getResult();
                                    handleLocationResult(location);
                                }
                            });
                } else {
                    requestLocationPermission();
                }
            }
        });

        ImageButton closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            stopPDR();
            if (userPath != null) {
                userPath.remove();
                userPath = null;
            }
            if (floorPaths != null) {
                floorPaths.clear();
                floorPaths = null;
            }
            if (marker != null) {
                marker.remove();
            }
            Toast.makeText(PdrActivity.this, "수정 버튼을 누르면 마커가 다시 생성됩니다!", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setOnMarkerDragListener(this);

        // Load data from MainActivity
        Intent intent = getIntent();
        if (intent != null && googleMap != null) {
            double latitude = intent.getDoubleExtra("latitude", 37.3752495);
            double longitude = intent.getDoubleExtra("longitude", 126.6321764);
            float zoomLevel = intent.getFloatExtra("zoomLevel", 15.0f);

            LatLng location = new LatLng(latitude, longitude);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
            addInitialMarkerAtCurLocation();

            // Handle zoom level changes
            googleMap.setOnCameraIdleListener(() -> {

                LatLng mapCenter = googleMap.getCameraPosition().target;

                if (googleMap.getCameraPosition().zoom >= 18.5 && isInsidePlace.univ(mapCenter)) {
                    spinner.setVisibility(View.VISIBLE); // Show spinner when zoom level is high enough
                    spinnerStartLocation.setVisibility(View.VISIBLE);
                    spinnerDestinationLocation.setVisibility(View.VISIBLE);
                    bt_search.setVisibility(View.VISIBLE);
                    bt_searchOff.setVisibility(View.VISIBLE);
                    isMapZoomedEnough = true;

                    // Show features for the selected level
                    int selectedLevel = spinner.getSelectedItemPosition();
                    showFeaturesForLevel(selectedLevel);
                } else {
                    spinner.setVisibility(View.GONE); // Hide spinner when zoom level is too low
                    spinnerStartLocation.setVisibility(View.GONE);
                    spinnerDestinationLocation.setVisibility(View.GONE);
                    bt_search.setVisibility(View.GONE);
                    bt_searchOff.setVisibility(View.GONE);
                    isMapZoomedEnough = false;
                    if (kmlLayer != null) {
                        kmlLayer.removeLayerFromMap();
                    }
                }
            });
        }
    }

    // PDR 중지
    private void stopPDR() {
        if (sdh != null) {
            sdh.stop();
        }
        if (dah != null) {
            dah.stop();
        }
        if (sph != null) {
            sph = null;
        }
        if (totalDistance != 0.0) {
            totalDistance = 0;
            totalDist.setText("약 " + String.format("%.2f", totalDistance) + " m 이동");
        }
        isFirstStep = true;
    }

    private void addMarker(double lat, double lon) {
        LatLng defaultLocation = new LatLng(lat, lon);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(defaultLocation)
                .title("현위치(출발지)");
        marker = googleMap.addMarker(markerOptions);
        isMarkerDraggable = true; // Enable marker dragging
        marker.setDraggable(isMarkerDraggable);
    }

    // 초기 위치를 가져와서 마커를 추가하는 메서드
    private void addInitialMarkerAtCurLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            LatLng currentLocation = new LatLng(latitude, longitude);
                            addMarker(currentLocation.latitude, currentLocation.longitude);
                        }
                    });
        } else {
            requestLocationPermission();
        }
    }

    private void changeMarkerIcon() {
        // Change marker icon
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.point);
        if (marker != null) {
            marker.setIcon(icon);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        // Handle marker drag start
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        // Handle marker drag
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        // Handle marker drag end
    }

    private void showFeaturesForLevel(int selectedLevel) {
        try {
            if (kmlLayer != null) {
                kmlLayer.removeLayerFromMap();
            }
            String kmlFilePath = "F" + (selectedLevel + 1) + ".kml";
            kmlLayer = new KmlLayer(googleMap, getResources().getAssets().open(kmlFilePath), getApplicationContext());
            kmlLayer.addLayerToMap();

            // 마커가 없는 경우에만 마커 추가
            if (marker != null) {
                marker.setVisible(true);
            } else {
                addInitialMarkerAtCurLocation();
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.level_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition("X")); // Initially select "X"

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // "X"가 아닌 경우에만 작업 수행
                if (!spinner.getSelectedItem().toString().equals("X")) {
                    // 층을 선택한 경우 해당 층의 KML 파일을 로드하고 표시
                    if (position != currentLevel) {
                        currentLevel = position;
                        showFeaturesForLevel(currentLevel);
                    }
                    // 사용자가 건물 내부에 있고 층 경로가 있는 경우 해당 층의 경로를 표시
                    if (isInsidePlace.building7(markerPosition) && floorPaths != null) {
                        showFloorPath(currentLevel);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Spinner에서 아무것도 선택되지 않은 경우 기본 동작 수행
                googleMap.clear();
                if (marker != null) {
                    marker.setVisible(true);
                } else {
                    addInitialMarkerAtCurLocation();
                }
            }
        });
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

    private void startPedestrianDetection(LatLng location) {
        if (location != null) {
            if (userPath != null && floorPaths != null) {
                userPath.remove(); // userPath 제거
                floorPaths.clear();
                userPath = null; // userPath 초기화
                floorPaths = null;
            }

            // 현재 위치가 건물 내부인지 확인합니다.
            boolean insideBuilding = isInsidePlace.building7(location);

            markerLocation = new Location("");
            markerLocation.setLatitude(location.latitude);
            markerLocation.setLongitude(location.longitude);

            dah = new DeviceAttitudeHandler((SensorManager) getSystemService(SENSOR_SERVICE));
            dah.start();

            // 보행자 탐지를 시작합니다.
            sdh = new StepDetectionHandler((SensorManager) getSystemService(SENSOR_SERVICE));
            sph = new StepPositioningHandler();
            sdh.setStepListener(mStepDetectionListener);
            sdh.start();

            if (insideBuilding) {
                drawUserPath(location, isInsidePlace.building7(location));

            } else {
                // 건물 외부인 경우 사용자 경로를 그립니다.
                drawUserPath(location, isInsidePlace.building7(location));
            }
        }
    }

    // 보행자 탐지 이벤트 리스너에서 사용자 경로를 그리는 부분 수정
    private final StepDetectionHandler.StepDetectionListener mStepDetectionListener = new StepDetectionHandler.StepDetectionListener() {
        public void newStep(float stepSize) {
            Location newLocation = new Location("");
            if (WalkingStatusManager.isWalking) {
                if (isFirstStep) {
                    sph.setInitialAzimuth(dah.getAzimuth()); // 초기 방위각 설정
                    drawUserPath(markerPosition, isInsidePlace.building7(markerPosition));
                    newLatLng = sph.computeNextStep(markerLocation, stepSize, dah.getYawRotationAngle());
                    Log.d("azimuth", "initialazimuth = " + dah.getAzimuth());
                    isFirstStep = false;
                }
                newLocation.setLatitude(newLatLng.latitude);
                newLocation.setLongitude(newLatLng.longitude);

                newLatLng = sph.computeNextStep(newLocation, sdh.distanceStep, dah.getYawRotationAngle());
                drawUserPath(newLatLng, isInsidePlace.building7(newLatLng));

                // 이동 거리 계산 및 표시
                if (markerLocation != null) {
                    totalDistance += sdh.distanceStep;
                    totalDist.setText("약 " + String.format("%.2f", totalDistance) + " m 이동");
                    markerLocation = newLocation;
                }

                // 이동 거리 계산 및 표시
                /*if (markerLocation != null) {
                    double distance = markerLocation.distanceTo(newLocation);
                    if (distance > 0) {
                        totalDistance += distance;
                        totalDist.setText("약 " + String.format("%.2f", totalDistance) + " m 이동");
                        markerLocation = newLocation;
                    }
                }*/
            }
        }
    };

    // 사용자 경로를 그리는 함수
    public void drawUserPath(LatLng location, boolean insideBuilding) {
        if (googleMap != null) {
            if (insideBuilding) {
                int curfloor = spinner.getSelectedItemPosition();
                if (floorPaths == null) {
                    // 건물 내 처음 사용자 경로를 그릴 때
                    PolylineOptions options = new PolylineOptions()
                            .width(14)
                            .color(Color.rgb(0, 0, 128))
                            .pattern(Arrays.asList(new Dash(20), new Gap(10)))
                            .jointType(JointType.ROUND)
                            .startCap(new RoundCap())
                            .endCap(new RoundCap());
                    floorPath = googleMap.addPolyline(options);
                } else {
                    // Polyline에 현재 위치를 추가합니다.
                    List<LatLng> points = floorPath.getPoints();
                    points.add(location);
                    floorPath.setPoints(points);
                    lastKnownLatLng = points.get(points.size() - 1);
                }
                floorPaths.add(curfloor, floorPath);
            } else {
                if (userPath == null) {
                    // 처음 사용자 경로를 그릴 때
                    PolylineOptions options = new PolylineOptions()
                            .width(14)
                            .color(Color.rgb(0, 128, 0))
                            .pattern(Arrays.asList(new Dash(20), new Gap(10)))
                            .jointType(JointType.ROUND)
                            .startCap(new RoundCap())
                            .endCap(new RoundCap());
                    userPath = googleMap.addPolyline(options);
                } else {
                    // Polyline에 현재 위치를 추가합니다.
                    List<LatLng> points = userPath.getPoints();
                    points.add(location);
                    userPath.setPoints(points);
                    lastKnownLatLng = points.get(points.size() - 1);
                    Log.d("lastKnownLatLng", " lastKnownLatLng = " + points.get(points.size() - 1));
                }
            }
        }
    }

    // 사용자가 층을 선택할 때 해당 층에서의 이동 경로를 표시하는 메서드
    private void showFloorPath(int floorIndex) {
        if (floorPaths != null) {
            for (int i = 0; i < floorPaths.size(); i++) {
                Polyline floorPath = floorPaths.get(i);
                if (floorPath != null) {
                    // 선택된 층인 경우 활성화, 그 외의 층은 비활성화
                    floorPath.setVisible(i == floorIndex);
                }
            }
        }
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
