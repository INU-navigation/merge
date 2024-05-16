package com.wegoup.pdr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import com.wegoup.dijkstra.CSVReader;
import com.wegoup.dijkstra.DijkstraAlgorithm;
import com.wegoup.dijkstra.UndirectedGraph;
import com.wegoup.incheon_univ_map.IsInsidePlace;
import com.wegoup.incheon_univ_map.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public String selectedStartLocation;
    public String selectedDestinationLocation;
    private Marker PDRmarker;
    LatLng markerPosition;  // 사용자 선택 마커 위치
    private LatLng newLatLng;   // computeNextStep(다음 위치 계산)
    private LatLng lastKnownLatLng = null;
    private Marker currentLocationMarker; // 현재 위치를 표시하기 위한 마커 변수
    public static StepDetectionHandler sdh;
    public static StepPositioningHandler sph;
    public static DeviceAttitudeHandler dah;
    TextView totalDist;
    private boolean isFirstStep = true;
    private boolean message = true;
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

        // 버튼 설정
        ImageButton editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> {
            if (googleMap != null) {
                if (PDRmarker != null && userPath != null) {
                    PDRmarker.remove();
                    userPath.remove(); userPath = null;
                    stopPDR();
                }
                if (lastKnownLatLng != null) {
                addMarker(lastKnownLatLng.latitude, lastKnownLatLng.longitude);
            } else {
                addInitialMarkerAtCurLocation();
                Log.d("marker", "marker가 있습니다.");
            }
                Toast.makeText(PdrActivity.this, "마커를 꾹 눌러 움직여주세요", Toast.LENGTH_LONG).show();
                PDRmarker.setDraggable(true);
            }
        });

        ImageButton setButton = findViewById(R.id.set_button);
        setButton.setOnClickListener(v -> {
            if (userPath != null) {
                stopPDR();
                userPath.remove();
                userPath = null;
            }
            if (PDRmarker != null) {
                changeMarkerIcon();
                PDRmarker.setDraggable(false);
                googleMap.setOnMarkerDragListener(null);

                markerPosition = PDRmarker.getPosition();
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
            if (PDRmarker != null) {
                PDRmarker.remove();
            }
            Toast.makeText(PdrActivity.this, "수정 버튼을 누르면 PDR 마커가 다시 생성됩니다!", Toast.LENGTH_SHORT).show();
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
                    if (message) {
                        Toast.makeText(PdrActivity.this, "출발지, 도착지는 파란색 마커로 나타납니다", Toast.LENGTH_SHORT).show();
                        Toast.makeText(PdrActivity.this, "실시간 위치 추정은 초록색 마커를 출발 위치로 움직여주세요", Toast.LENGTH_SHORT).show();
                        Toast.makeText(PdrActivity.this, "설정하기 버튼을 누르면 시작하고, 끝내기를 누르면 멈춥니다", Toast.LENGTH_SHORT).show();
                        message = false;
                    }
                } else {
                    spinner.setVisibility(View.GONE); // Hide spinner when zoom level is too low
                    spinnerStartLocation.setVisibility(View.GONE);
                    spinnerDestinationLocation.setVisibility(View.GONE);
                    bt_search.setVisibility(View.GONE);
                    bt_searchOff.setVisibility(View.GONE);
                    isMapZoomedEnough = false;
                    message = true;
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
        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        LatLng defaultLocation = new LatLng(lat, lon);
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(icon)
                .position(defaultLocation)
                .title("현위치(출발지)");
        PDRmarker = googleMap.addMarker(markerOptions);
        isMarkerDraggable = true; // Enable marker dragging
        PDRmarker.setDraggable(isMarkerDraggable);
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
        if (PDRmarker != null) {
            PDRmarker.setIcon(icon);
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
            if (PDRmarker != null) {
                PDRmarker.setVisible(true);
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
        BitmapDescriptor icon2 = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);

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
                    /*// 사용자가 건물 내부에 있고 층 경로가 있는 경우 해당 층의 경로를 표시
                    if (isInsidePlace.building7(markerPosition) && floorPaths != null) {
                        showFloorPath(currentLevel);
                    }*/
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Spinner에서 아무것도 선택되지 않은 경우 기본 동작 수행
                googleMap.clear();
                if (PDRmarker != null) {
                    PDRmarker.setVisible(true);
                } else {
                    addInitialMarkerAtCurLocation();
                }
            }
        });

        AdapterView.OnItemSelectedListener startItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                selectedStartLocation = selected;
                Log.d("OnItemSelected", "Selected start location: " + selectedStartLocation);
                //MarkerFromCSV(MainActivity.this,selectedStartLocation);
                List<List<String>> SaveMarkerLocation = new ArrayList<>();
                List<Double> MarkerLocation = new ArrayList<>();
                SaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedStartLocation);

                for (List<String> innerList : SaveMarkerLocation) {
                    for (String str : innerList) {
                        // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                        String[] split = str.split(",");
                        for (String s : split) {
                            MarkerLocation.add(Double.parseDouble(s.trim()));
                        }
                    }
                }

                if (!SaveMarkerLocation.isEmpty())
                {
                    Double lat = MarkerLocation.get(1); Double lon = MarkerLocation.get(0);
                    LatLng markerPosition = new LatLng(lat,lon);

                    MarkerOptions StartMarkerOptions = new MarkerOptions();
                    StartMarkerOptions.position(markerPosition);
                    StartMarkerOptions.title(selectedStartLocation);
                    StartMarkerOptions.icon(icon2);
                    // DestinationMarkerOptions.position(markerPosition);
                    Marker marker = googleMap.addMarker(StartMarkerOptions);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 선택된 것이 없을 때의 동작을 정의합니다.
            }
        };

        // 출발지 스피너에 선택 이벤트 핸들러 설정
        spinnerStartLocation.setOnItemSelectedListener(startItemSelectedListener);

        // 도착지 스피너의 선택 이벤트 핸들러
        AdapterView.OnItemSelectedListener destinationItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                selectedDestinationLocation = selected;
                Log.d("OnItemSelected", "Selected destination location: " + selectedDestinationLocation);
                List<List<String>> SaveMarkerLocation = new ArrayList<>();
                List<Double> MarkerLocation = new ArrayList<>();
                SaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedDestinationLocation);

                for (List<String> innerList : SaveMarkerLocation) {
                    for (String str : innerList) {
                        // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                        String[] split = str.split(",");
                        for (String s : split) {
                            MarkerLocation.add(Double.parseDouble(s.trim()));
                        }
                    }
                }

                if(!SaveMarkerLocation.isEmpty())
                {
                    Double lat = MarkerLocation.get(1); Double lon = MarkerLocation.get(0);
                    LatLng markerPosition = new LatLng(lat,lon);

                    MarkerOptions DestinationMarkerOptions = new MarkerOptions();
                    DestinationMarkerOptions.position(markerPosition);
                    DestinationMarkerOptions.title(selectedDestinationLocation);
                    DestinationMarkerOptions.icon(icon2);
                    // DestinationMarkerOptions.position(markerPosition);
                    Marker marker = googleMap.addMarker(DestinationMarkerOptions);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 선택된 것이 없을 때의 동작을 정의합니다.
            }
        };

        // 도착지 스피너에 선택 이벤트 핸들러 설정
        spinnerDestinationLocation.setOnItemSelectedListener(destinationItemSelectedListener);

        bt_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedStartLocation == null || selectedDestinationLocation == null) {
                    Toast.makeText(PdrActivity.this, "Please select both start and destination locations", Toast.LENGTH_SHORT).show();
                    return;
                }
                googleMap.clear();
                Toast.makeText(PdrActivity.this, "수정 버튼을 누르면 PDR 마커가 다시 생성됩니다!", Toast.LENGTH_SHORT).show();

                // 선택된 출발지의 층을 가져옵니다.
                int selectedFloorStart = Integer.parseInt(selectedStartLocation.substring(0, 1));
                int selectedFloorDestination = Integer.parseInt(selectedDestinationLocation.substring(0, 1));

                // 선택된 층에 따라 지도를 보여줍니다.
                showMapForFloor(selectedFloorStart);


                UndirectedGraph graph = new UndirectedGraph();
                readGraphDataFromCSV(PdrActivity.this, graph, "path12345-좌표최종2.csv");

                //Log.d("Graph", graph.getGraphInfo());
                // getVertexCount 메서드 호출하여 노드의 수 가져오기

                DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph, PdrActivity.this);

                //버튼

                List<String> shortestPath = dijkstra.findShortestPath(selectedStartLocation, selectedDestinationLocation);
                // 최단 경로 출력
                Log.d("ShortestPath", "Shortest Path: " + shortestPath);

                int splitIndex = -1;
                List<String> firstHalf = new ArrayList<>();
                List<String> secondHalf = new ArrayList<>();
                List<Integer> splitIndices = new ArrayList<>(); // 모든 "계단입구"의 인덱스를 저장할 리스트

                for (int i = 0; i < shortestPath.size(); i++) {
                    if (shortestPath.get(i) instanceof String && ((String) shortestPath.get(i)).startsWith("계단입구")) {
                        splitIndices.add(i); // "계단입구"의 인덱스를 리스트에 추가
                    }
                }
                ;

                if (selectedFloorStart != selectedFloorDestination) {


                    // 첫 번째 "계단입구"를 기준으로 배열을 분리
                    int startIndex = 0;
                    int endIndex = splitIndices.get(0);
                    firstHalf = shortestPath.subList(startIndex, endIndex + 1);

                    // 두 번째 배열은 첫 번째 "계단입구" 다음부터 마지막 요소까지
                    startIndex = endIndex + 1;
                    endIndex = shortestPath.size() - 1;
                    secondHalf = shortestPath.subList(startIndex, endIndex + 1);


                    List<String> FirstEdgeName = dijkstra.GetEdgeName(firstHalf);
                    List<String> SecondEdgeName = dijkstra.GetEdgeName(secondHalf);

                    List<List<String>> FirstvaluesList = retrieveValuesFromCSV(PdrActivity.this, FirstEdgeName);
                    List<List<String>> SecondvaluesList = retrieveValuesFromCSV(PdrActivity.this, SecondEdgeName);

                    List<List<Double>> FirstconvertedValuesList = new ArrayList<>();
                    List<List<Double>> SecondconvertedValuesList = new ArrayList<>();

                    for (List<String> coordinates : FirstvaluesList) {
                        List<Double> convertedCoordinates = new ArrayList<>();
                        for (String coordinate : coordinates) {
                            String[] coords = coordinate.split(","); // 좌표를 쉼표로 분리
                            if (coords.length == 4) { // 좌표가 위도와 경도 쌍으로 구성된 경우
                                double latitude1 = Double.parseDouble(coords[0]);
                                double longitude1 = Double.parseDouble(coords[1]);
                                double latitude2 = Double.parseDouble(coords[2]);
                                double longitude2 = Double.parseDouble(coords[3]);

                                // double 값으로 변환된 좌표를 리스트에 추가합니다.
                                convertedCoordinates.add(latitude1);
                                convertedCoordinates.add(longitude1);
                                convertedCoordinates.add(latitude2);
                                convertedCoordinates.add(longitude2);

                            }
                            FirstconvertedValuesList.add(convertedCoordinates);
                        }

                    }

                    for (List<String> coordinates : SecondvaluesList) {
                        List<Double> convertedCoordinates = new ArrayList<>();
                        for (String coordinate : coordinates) {
                            String[] coords = coordinate.split(","); // 좌표를 쉼표로 분리
                            if (coords.length == 4) { // 좌표가 위도와 경도 쌍으로 구성된 경우
                                double latitude1 = Double.parseDouble(coords[0]);
                                double longitude1 = Double.parseDouble(coords[1]);
                                double latitude2 = Double.parseDouble(coords[2]);
                                double longitude2 = Double.parseDouble(coords[3]);

                                // double 값으로 변환된 좌표를 리스트에 추가합니다.
                                convertedCoordinates.add(latitude1);
                                convertedCoordinates.add(longitude1);
                                convertedCoordinates.add(latitude2);
                                convertedCoordinates.add(longitude2);

                            }
                            SecondconvertedValuesList.add(convertedCoordinates);
                        }

                    }

                    List<LatLng> Firstpoints = new ArrayList<>();
                    for (List<Double> coordinates : FirstconvertedValuesList) {
                        for (int i = 0; i < coordinates.size(); i += 2) { // 각 좌표쌍마다 2씩 증가
                            double longitude = coordinates.get(i);
                            double latitude = coordinates.get(i + 1);
                            Firstpoints.add(new LatLng(latitude, longitude)); // 위도와 경도를 바꿔서 추가
                        }
                    }

                    List<LatLng> Secondpoints = new ArrayList<>();
                    for (List<Double> coordinates : SecondconvertedValuesList) {
                        for (int i = 0; i < coordinates.size(); i += 2) { // 각 좌표쌍마다 2씩 증가
                            double longitude = coordinates.get(i);
                            double latitude = coordinates.get(i + 1);
                            Secondpoints.add(new LatLng(latitude, longitude)); // 위도와 경도를 바꿔서 추가
                        }
                    }

                    // 원래의 points 리스트를 복사하여 pointsCopy를 생성합니다.
                    List<LatLng> FirstpointsCopy = new ArrayList<>(Firstpoints);
                    List<LatLng> SecondpointsCopy = new ArrayList<>(Secondpoints);


                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            // 사용자가 항목을 선택했을 때 실행되는 코드
                            Marker previousMarker = null;
                            PolylineOptions polylineOptionsSecond = new PolylineOptions();
                            polylineOptionsSecond.width(10); polylineOptionsSecond .color(Color.BLUE);
                            String selectedItem = (String) parentView.getItemAtPosition(position);
                            selectedItem = selectedItem.substring(1, 2);


                            //도착지 층
                            if(selectedItem.equals(selectedDestinationLocation.substring(0, 1)))
                            {
                                if (previousMarker != null) {
                                    previousMarker.remove();
                                }
                                googleMap.clear();
                                for (int i = 0; i < SecondpointsCopy.size(); i += 2) {
                                    // 현재 좌표와 다음 좌표를 가져와서 PolylineOptions에 추가합니다.
                                    polylineOptionsSecond.add(SecondpointsCopy.get(i));
                                    if (i + 1 < SecondpointsCopy.size()) {
                                        polylineOptionsSecond.add(SecondpointsCopy.get(i + 1));
                                    }
                                    googleMap.addPolyline(polylineOptionsSecond); // 추가된 좌표를 포함한 Polyline을 지도에 표시합니다.
                                    // 추가된 좌표를 제거하여 중복 사용을 방지합니다.
                                    if (i + 1 < SecondpointsCopy.size()) {
                                        polylineOptionsSecond.getPoints().remove(SecondpointsCopy.get(i + 1));
                                    }
                                    polylineOptionsSecond.getPoints().remove(SecondpointsCopy.get(i));
                                }
                                //도착지 마커
                                List<List<String>> DestinationSaveMarkerLocation = new ArrayList<>();
                                List<Double> DestinationMarkerLocation = new ArrayList<>();
                                DestinationSaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedDestinationLocation);

                                for (List<String> innerList : DestinationSaveMarkerLocation) {
                                    for (String str : innerList) {
                                        // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                                        String[] split = str.split(",");
                                        for (String s : split) {
                                            DestinationMarkerLocation.add(Double.parseDouble(s.trim()));
                                        }
                                    }
                                }


                                if(!DestinationMarkerLocation.isEmpty())
                                {
                                    Double lat = DestinationMarkerLocation.get(1); Double lon = DestinationMarkerLocation.get(0);
                                    LatLng markerPosition = new LatLng(lat,lon);

                                    MarkerOptions StartMarkerOptions = new MarkerOptions();
                                    StartMarkerOptions.position(markerPosition);
                                    StartMarkerOptions.title(selectedDestinationLocation);
                                    StartMarkerOptions.icon(icon2);
                                    // DestinationMarkerOptions.position(markerPosition);
                                    Marker marker = googleMap.addMarker(StartMarkerOptions);
                                    previousMarker = marker;


                                }


                            }//출발지 층
                            else if(selectedItem.equals(selectedStartLocation.substring(0, 1)))
                            {
                                if (previousMarker != null) {
                                    previousMarker.remove();
                                }
                                //출발지 마커
                                List<List<String>> SaveMarkerLocation = new ArrayList<>();
                                List<Double> MarkerLocation = new ArrayList<>();
                                SaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedStartLocation);

                                for (List<String> innerList : SaveMarkerLocation) {
                                    for (String str : innerList) {
                                        // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                                        String[] split = str.split(",");
                                        for (String s : split) {
                                            MarkerLocation.add(Double.parseDouble(s.trim()));
                                        }
                                    }
                                }


                                if(!SaveMarkerLocation.isEmpty())
                                {
                                    Double lat = MarkerLocation.get(1); Double lon = MarkerLocation.get(0);
                                    LatLng markerPosition = new LatLng(lat,lon);

                                    MarkerOptions StartMarkerOptions = new MarkerOptions();
                                    StartMarkerOptions.position(markerPosition);
                                    StartMarkerOptions.title(selectedStartLocation);
                                    StartMarkerOptions.icon(icon2);
                                    // DestinationMarkerOptions.position(markerPosition);
                                    Marker marker = googleMap.addMarker(StartMarkerOptions);
                                    previousMarker = marker;

                                }

                                googleMap.clear();
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.width(10); polylineOptions .color(Color.BLUE);

                                for (int i = 0; i < FirstpointsCopy.size(); i += 2) {
                                    // 현재 좌표와 다음 좌표를 가져와서 PolylineOptions에 추가합니다.
                                    polylineOptions.add(FirstpointsCopy.get(i));
                                    if (i + 1 < FirstpointsCopy.size()) {
                                        polylineOptions.add(FirstpointsCopy.get(i + 1));
                                    }
                                    googleMap.addPolyline(polylineOptions); // 추가된 좌표를 포함한 Polyline을 지도에 표시합니다.
                                    // 추가된 좌표를 제거하여 중복 사용을 방지합니다.
                                    if (i + 1 < FirstpointsCopy.size()) {
                                        polylineOptions.getPoints().remove(FirstpointsCopy.get(i + 1));
                                    }
                                    polylineOptions.getPoints().remove(FirstpointsCopy.get(i));
                                }

                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            // 아무 항목도 선택되지 않았을 때 실행되는 코드
                        }
                    });

                }
                else if(selectedFloorStart == selectedFloorDestination)
                {

                    //출발지 마커
                    List<List<String>> SaveMarkerLocation = new ArrayList<>();
                    List<Double> MarkerLocation = new ArrayList<>();
                    SaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedStartLocation);

                    for (List<String> innerList : SaveMarkerLocation) {
                        for (String str : innerList) {
                            // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                            String[] split = str.split(",");
                            for (String s : split) {
                                MarkerLocation.add(Double.parseDouble(s.trim()));
                            }
                        }
                    }


                    if(!SaveMarkerLocation.isEmpty())
                    {
                        Double lat = MarkerLocation.get(1); Double lon = MarkerLocation.get(0);
                        LatLng markerPosition = new LatLng(lat,lon);

                        MarkerOptions StartMarkerOptions = new MarkerOptions();
                        StartMarkerOptions.position(markerPosition);
                        StartMarkerOptions.title(selectedStartLocation);
                        StartMarkerOptions.icon(icon2);
                        // DestinationMarkerOptions.position(markerPosition);
                        Marker marker = googleMap.addMarker(StartMarkerOptions);

                    }

                    List<List<String>> DestinationSaveMarkerLocation = new ArrayList<>();
                    List<Double> DestinationMarkerLocation = new ArrayList<>();
                    DestinationSaveMarkerLocation = MarkerFromCSV(PdrActivity.this,selectedDestinationLocation);

                    for (List<String> innerList : DestinationSaveMarkerLocation) {
                        for (String str : innerList) {
                            // 쉼표를 제거하고 공백을 없앤 후 더블로 변환합니다.
                            String[] split = str.split(",");
                            for (String s : split) {
                                DestinationMarkerLocation.add(Double.parseDouble(s.trim()));
                            }
                        }
                    }


                    if(!DestinationMarkerLocation.isEmpty())
                    {
                        Double lat = DestinationMarkerLocation.get(1); Double lon = DestinationMarkerLocation.get(0);
                        LatLng markerPosition = new LatLng(lat,lon);

                        MarkerOptions StartMarkerOptions = new MarkerOptions();
                        StartMarkerOptions.position(markerPosition);
                        StartMarkerOptions.title(selectedDestinationLocation);
                        StartMarkerOptions.icon(icon2);
                        // DestinationMarkerOptions.position(markerPosition);
                        Marker marker = googleMap.addMarker(StartMarkerOptions);

                    }


                    List<String> EdgeName = dijkstra.GetEdgeName(shortestPath);
                    Log.d("EdgeShortestPath", "EdgeName: " + EdgeName);

                    List<List<String>> valuesList = retrieveValuesFromCSV(PdrActivity.this, EdgeName);
                    Log.d("latlon", "laton: " + valuesList);

                    // 새로운 리스트를 생성하여 double 값으로 변환된 좌표를 저장합니다.
                    List<List<Double>> convertedValuesList = new ArrayList<>();

// valuesList에 저장된 좌표들을 순회하면서 double 값으로 변환하여 convertedValuesList에 저장합니다.
                    for (List<String> coordinates : valuesList) {
                        List<Double> convertedCoordinates = new ArrayList<>();
                        for (String coordinate : coordinates) {
                            String[] coords = coordinate.split(","); // 좌표를 쉼표로 분리
                            if (coords.length == 4) { // 좌표가 위도와 경도 쌍으로 구성된 경우
                                double latitude1 = Double.parseDouble(coords[0]);
                                double longitude1 = Double.parseDouble(coords[1]);
                                double latitude2 = Double.parseDouble(coords[2]);
                                double longitude2 = Double.parseDouble(coords[3]);

                                // double 값으로 변환된 좌표를 리스트에 추가합니다.
                                convertedCoordinates.add(latitude1);
                                convertedCoordinates.add(longitude1);
                                convertedCoordinates.add(latitude2);
                                convertedCoordinates.add(longitude2);
                            }
                        }
                        // 변환된 좌표를 리스트에 추가합니다.
                        convertedValuesList.add(convertedCoordinates);
                    }

                    Log.d("double", "double: " + convertedValuesList);

                    List<LatLng> points = new ArrayList<>();
                    for (List<Double> coordinates : convertedValuesList) {
                        for (int i = 0; i < coordinates.size(); i += 2) { // 각 좌표쌍마다 2씩 증가
                            double longitude = coordinates.get(i);
                            double latitude = coordinates.get(i + 1);
                            points.add(new LatLng(latitude, longitude)); // 위도와 경도를 바꿔서 추가
                        }
                    }
                    // 원래의 points 리스트를 복사하여 pointsCopy를 생성합니다.
                    List<LatLng> pointsCopy = new ArrayList<>(points);
                    Log.d("polyline","polyline: "+pointsCopy);

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .width(10)
                            .color(Color.BLUE);
                    // pointsCopy 리스트에 저장된 좌표를 2개씩 PolylineOptions에 추가합니다.
                    for (int i = 0; i < pointsCopy.size(); i += 2) {
                        // 현재 좌표와 다음 좌표를 가져와서 PolylineOptions에 추가합니다.
                        polylineOptions.add(pointsCopy.get(i));
                        if (i + 1 < pointsCopy.size()) {
                            polylineOptions.add(pointsCopy.get(i + 1));
                        }
                        googleMap.addPolyline(polylineOptions); // 추가된 좌표를 포함한 Polyline을 지도에 표시합니다.
                        // 추가된 좌표를 제거하여 중복 사용을 방지합니다.
                        if (i + 1 < pointsCopy.size()) {
                            polylineOptions.getPoints().remove(pointsCopy.get(i + 1));
                        }
                        polylineOptions.getPoints().remove(pointsCopy.get(i));
                    }


                }
            }
        });

        bt_searchOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // bt_searchoff 버튼이 클릭되었을 때 실행되는 코드를 여기에 추가합니다.
                // marker.remove();
                // 폴리라인 제거 예:
                googleMap.clear();

                // 기타 관련된 작업들을 초기화하는 코드를 추가합니다.
            }
        });
    }

    public List<List<String>> MarkerFromCSV(Context context, String SpinnerName) {
        List<List<String>> MarkerLocation = new ArrayList<>();
        try {
            // CSV 파일을 읽어오기 위해 CSVReader 클래스 사용
            List<List<String>> csvData = CSVReader.pathCSV(context, "path12345-좌표최종2.csv");

            // selectedStartLocation 변수의 값과 CSV 파일의 1열 또는 2열과 비교하여 일치하는 경우 해당 행의 6번째 열 값을 추가
            for (List<String> row : csvData) {
                if (row.size() >= 2 && (row.get(0).equals(SpinnerName) || row.get(1).equals(SpinnerName))) {
                    // 선택한 시작 위치와 CSV 파일의 1열 또는 2열이 일치하는 경우
                    if (row.size() >= 6) {
                        // 6번째 열 값이 있는 경우 해당 값을 추가
                        MarkerLocation.add(Collections.singletonList(row.get(5)));
                    } else {
                        // 6번째 열 값이 없는 경우 빈 값을 추가
                        MarkerLocation.add(Collections.singletonList(""));
                    }
                    // 선택한 시작 위치와 일치하는 행을 찾았으므로 더 이상 반복할 필요 없음
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            // 예외 처리 로직 추가
        }
        return MarkerLocation;
    }

    private void showMapForFloor(int floor) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.level_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        switch (floor) {
            case 1:
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition("F1"));
                showFeaturesForLevel(0); // 레벨에 따른 특징을 보여주는 메서드 호출
                break;
            case 2:
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition("F2"));
                showFeaturesForLevel(1);
                break;
            // 필요한 만큼 층에 대한 case를 추가할 수 있습니다.
            case 3:
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition("F3"));
                showFeaturesForLevel(2);
                break;
            case 4:
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition("F4"));
                showFeaturesForLevel(3);
                break;
            case 5:
                spinner.setAdapter(adapter);
                spinner.setSelection(adapter.getPosition("F5"));
                showFeaturesForLevel(4);
                break;
        }
    }

    private void readGraphDataFromCSV(Context context, UndirectedGraph graph, String fileName) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open("path12345-좌표최종2.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length >= 4) {
                    String startNode = columns[0].trim();
                    String endNode = columns[1].trim();
                    String edgeName = columns[2].trim();
                    double length = Double.parseDouble(columns[3].trim());

                    if (startNode != null && endNode != null && edgeName != null) {
                        graph.addEdge(startNode, endNode, edgeName, length);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<List<String>> retrieveValuesFromCSV(Context context, List<String> edgeNames) {
        List<List<String>> valuesList = new ArrayList<>();
        try {
            // CSV 파일을 읽어오기 위해 CSVReader 클래스 사용
            List<List<String>> csvData = CSVReader.pathCSV(context, "path12345-좌표최종2.csv");

            // edgeNames 리스트를 순회하며 각 값과 CSV 파일의 3번째 열 값을 비교
            for (String edgeName : edgeNames) {
                for (List<String> row : csvData) {
                    // CSV 파일의 3번째 열 값과 edgeName이 일치하는 경우
                    if (row.size() >= 3 && row.get(2).equals(edgeName)) {
                        // 이차원 리스트에 해당 행의 5번째 열 값을 추가
                        if (row.size() >= 5) {
                            valuesList.add(Collections.singletonList(row.get(4)));
                        } else {
                            // 5번째 열 값이 없는 경우 빈 값을 추가
                            valuesList.add(Collections.singletonList(""));
                        }
                        break; // 일치하는 행을 찾았으므로 더 이상 반복할 필요 없음
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 예외 처리 로직 추가
        }
        Log.d("위경도 추가","위경도 추가"+valuesList);
        return valuesList;
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

            drawUserPath(location, isInsidePlace.building7(location));
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
            /*if (insideBuilding) {
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
            } else {*/
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
