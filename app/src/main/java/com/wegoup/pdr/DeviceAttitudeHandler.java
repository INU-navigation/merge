package com.wegoup.pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Arrays;

public class DeviceAttitudeHandler implements SensorEventListener {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private KalmanFilter accelerometerKalmanFilter; // 가속도계에 적용되는 칼만 필터
    private KalmanFilter magnetometerKalmanFilter; // 자기장계에 적용되는 칼만 필터
    private KalmanFilter gyroscopeKalmanFilter;
    private float azimuth = 0.0f;
    private float[] acceleration = new float[3];
    private float[] magneticField = new float[3];
    private long previousTimestamp = 0;
    private float[] rotationAngleArray = new float[3];
    private boolean isPDRStarted = false; // PDR 시작 여부를 나타내는 플래그
    private static final float ROTATION_THRESHOLD = 0.18f; // 회전 감지 임계값 설정
    private boolean isRotationDetected = false; // 회전 감지 여부를 나타내는 플래그

    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 각 센서에 대한 칼만 필터 초기화
        accelerometerKalmanFilter = new KalmanFilter(0.005f);
        magnetometerKalmanFilter = new KalmanFilter(0.005f);
        gyroscopeKalmanFilter = new KalmanFilter(0.005f);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            // 가속도계 데이터에 칼만 필터 적용
            float[] filteredAcceleration = accelerometerKalmanFilter.filter(event.values);
            acceleration = filteredAcceleration;
            /*if (!isPDRStarted) {
                // PDR을 시작할 때만 방위각 측정
                azimuth1 = updateAzimuth1(filteredAcceleration);
                Log.d("DeviceAttitudeHandler", "updateAzimuth1 : " + azimuth1);
            }*/
            Log.d("DeviceAttitudeHandler", "Accelerometer: " + Arrays.toString(acceleration));
        } else if (event.sensor == magnetometer) {
            // 자기장계 데이터에 칼만 필터 적용
            float[] filteredMagneticField = magnetometerKalmanFilter.filter(event.values);
            magneticField = filteredMagneticField;
            if (!isPDRStarted) {
                // PDR을 시작할 때만 방위각 측정
                azimuth = updateAzimuth();
                isPDRStarted = true;
                Log.d("DeviceAttitudeHandler", "updateAzimuth2 : " + azimuth);
            }
            Log.d("DeviceAttitudeHandler", "Magnetometer: " + Arrays.toString(magneticField));
        } else if (event.sensor == gyroscope) {
            // 자이로스코프 데이터에 칼만 필터 적용
            float[] filteredAngularVelocity = gyroscopeKalmanFilter.filter(event.values);

            long currentTimestamp = event.timestamp;
            if (previousTimestamp != 0) {
                float dt = (currentTimestamp - previousTimestamp) * NS2S;
                calculateRotationAngle(filteredAngularVelocity, dt); // 변경된 부분
            }
            Log.d("DeviceAttitudeHandler", "Gyroscope: " + Arrays.toString(filteredAngularVelocity));
            previousTimestamp = currentTimestamp;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public float updateAzimuth() {
        float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magneticField)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            return (float) Math.toDegrees(orientation[0]);
        } else {
            return 0.0f;
        }
    }

    private void calculateRotationAngle(float[] angularVelocity, float dt) {
        float[] deltaAngle = new float[3];
        deltaAngle[0] = angularVelocity[0] * dt;
        deltaAngle[1] = angularVelocity[1] * dt;
        deltaAngle[2] = angularVelocity[2] * dt;

        // 회전 감지 여부를 확인
        float angularVelocityMagnitude = (float) Math.sqrt(
                angularVelocity[0] * angularVelocity[0] +
                        angularVelocity[1] * angularVelocity[1] +
                        angularVelocity[2] * angularVelocity[2]);

        isRotationDetected = angularVelocityMagnitude > ROTATION_THRESHOLD;
        Log.d("DeviceAttitudeHandlers", "angularVelocityMagnitude : " + angularVelocityMagnitude);
        Log.d("DeviceAttitudeHandlers", "isRotatedDetected : " + isRotationDetected);

        // 회전 각도를 업데이트
        rotationAngleArray[0] += deltaAngle[0];
        rotationAngleArray[1] += deltaAngle[1];
        rotationAngleArray[2] += deltaAngle[2];
    }

    public float getAzimuth() {
        return azimuth;
    }

    public float getYawRotationAngle() {;
        Log.d("DeviceAttitudeHandler", "getYawRotationAngle : " + rotationAngleArray[0]);
        return rotationAngleArray[0];
    }

    public boolean isRotationDetected() {
        return isRotationDetected;
    }
}

/*public class DeviceAttitudeHandler implements SensorEventListener {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private UnscentedKalmanFilter kalmanFilter;
    private double azimuth = 0.0;
    private float[] acceleration = new float[3];
    private float[] magneticField = new float[3];
    private long previousTimestamp = 0;
    private float[] rotationAngleArray = new float[3];


    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        double[] initialState = new double[]{0, 0, 0};
        double[][] initialCovariance = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        double[][] processNoise = new double[][]{{0.001f, 0, 0}, {0, 0.001f, 0}, {0, 0, 0.001f}};
        double[][] measurementNoise = new double[][]{{0.01f, 0, 0}, {0, 0.01f, 0}, {0, 0, 0.01f}};
        kalmanFilter = new UnscentedKalmanFilter(initialState, initialCovariance, processNoise, measurementNoise);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            acceleration = event.values;
            updateAzimuth();
        } else if (event.sensor == magnetometer) {
            magneticField = event.values;
            updateAzimuth();
        } else if (event.sensor == gyroscope) {
            float[] angularVelocity = event.values;
            kalmanFilter.predict();
            kalmanFilter.update(angularVelocity);
            long currentTimestamp = event.timestamp;
            if (previousTimestamp != 0) {
                float dt = (currentTimestamp - previousTimestamp) * NS2S;
                calculateRotationAngle(angularVelocity, dt);
            }
            previousTimestamp = currentTimestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateAzimuth() {
        float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magneticField)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = Math.toDegrees(orientation[0]);
        }
    }

    private void calculateRotationAngle(float[] angularVelocity, float dt) {
        float[] deltaAngle = new float[3];
        deltaAngle[0] = angularVelocity[0] * dt;
        deltaAngle[1] = angularVelocity[1] * dt;
        deltaAngle[2] = angularVelocity[2] * dt;

        // Apply low-pass filter to angular velocity
        rotationAngleArray[0] += deltaAngle[0];
        rotationAngleArray[1] += deltaAngle[1];
        rotationAngleArray[2] += deltaAngle[2];
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getYawRotationAngle() {
        return Math.toDegrees(rotationAngleArray[0]);
    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private ExtendedKalmanFilter kalmanFilter;
    //private UnscentedKalmanFilter kalmanFilter;
    private float azimuth = 0.0f;
    private boolean initialAzimuthSet = false;
    private float[] acceleration = new float[3];
    private float[] magneticField = new float[3];
    private long previousTimestamp = 0;
    private float[] previousAngularVelocity = new float[3];
    private float[] rotationAngleArray = new float[3]; // 회전 방향각 배열 변수 추가
    // 제자리 회전 감지 여부
    private boolean isStationaryRotationDetected = false;
    // 제자리 회전 패턴 식별을 위한 회전 각속도 임계값 (라디안/초 단위)
    //private static float stepLength;
    //private static final float ROTATION_THRESHOLD = 1.2f; // 예시 임계값

    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Kalman 필터 초기화
        //float[][] initialCovariance = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        //float[][] processNoise = {{0.001f, 0, 0}, {0, 0.001f, 0}, {0, 0, 0.001f}};
        //float[][] measurementNoise = {{0.01f, 0, 0}, {0, 0.01f, 0}, {0, 0, 0.01f}};
        //kalmanFilter = new UnscentedKalmanFilter(new float[]{0, 0, 0}, initialCovariance, processNoise, measurementNoise);
        kalmanFilter = new ExtendedKalmanFilter(new float[]{0, 0, 0}, new float[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}},
                new float[][]{{0.001f, 0, 0}, {0, 0.001f, 0}, {0, 0, 0.001f}},
                new float[][]{{0.01f, 0, 0}, {0, 0.01f, 0}, {0, 0, 0.01f}});
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            Log.d("DeviceAttitudeHandler", "Accelerometer event received");
            acceleration = event.values;
            updateAzimuth();
        } else if (event.sensor == magnetometer) {
            Log.d("DeviceAttitudeHandler", "Magnetometer event received");
            magneticField = event.values;
            updateAzimuth();
        } else if (event.sensor == gyroscope) {
            Log.d("DeviceAttitudeHandler", "Gyroscope event received");
            float[] angularVelocity = event.values;
            long currentTimestamp = event.timestamp;
            if (previousTimestamp != 0) {
                float dt = (currentTimestamp - previousTimestamp) * (1.0e-9f); // nanoseconds to seconds
                Log.d("DeviceAttitudeHandler", "dt : " + dt);
                calculateRotationAngle(angularVelocity, dt); // 회전 방향각 배열 업데이트
                //identifyRotationPattern(angularVelocity);
                kalmanFilter.predict(rotationAngleArray); // 회전 방향각 배열로 예측
                //kalmanFilter.predict();
                //kalmanFilter.update(angularVelocity);
                Log.d("DeviceAttitudeHandler", "Predicted rotation angle: " + Arrays.toString(rotationAngleArray));
            }
            previousTimestamp = currentTimestamp;
            previousAngularVelocity = angularVelocity.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateAzimuth() {
        float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magneticField)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
            if (!initialAzimuthSet) {
                Log.d("DeviceAttitudeHandler", "Initial azimuth: " + azimuth);
                initialAzimuthSet = true;
            }
        }
    }

    private void calculateRotationAngle(float[] angularVelocity, float dt) {
        // Calculate rotation angle using gyroscope data
        for (int i = 0; i < 3; i++) {
            rotationAngleArray[i] += angularVelocity[i] * dt;
        }
        Log.d("DeviceAttitudeHandler", "calculateRotationAngle : " + Arrays.toString(rotationAngleArray));
    }

    public float getAzimuth() {
        Log.d("DeviceAttitudeHandler", "azimuth : " + azimuth);
        return azimuth;
    }

    public float getYawRotationAngle() {
        return (float) (rotationAngleArray[0] * (180 / (Math.PI))); // Yaw 값 반환
    }
}*/


    // 회전 패턴 식별 메서드
    /*private void identifyRotationPattern(float[] angularVelocity) {
        // 회전 각속도의 크기 계산
        double magnitude = Math.sqrt(angularVelocity[0] * angularVelocity[0] + angularVelocity[1] * angularVelocity[1] + angularVelocity[2] * angularVelocity[2]);
        double magnitude2 = magnitude * (180 / (Math.PI));
        Log.d("Angle", "magnitude2 : " + magnitude2);

        // 이전 회전 각도
        float previousRotationAngle = getYawRotationAngle();
        Log.d("Angle", "previousRotationAngle : " + previousRotationAngle);

        // 현재 회전 각도
        float currentRotationAngle = (float)(previousRotationAngle + magnitude); // 간단히 예시로 현재 각속도를 더해 계산
        Log.d("Angle", "currentRotationAngle : " + currentRotationAngle);

        // 현재 회전 각도와 이전 회전 각도 사이의 변화량 계산
        float rotationChange = Math.abs(currentRotationAngle - previousRotationAngle);
        Log.d("Angle", "rotationChange : " + rotationChange);

        // 변화량이 ROTATION_THRESHOLD보다 큰지 확인
        boolean isRotationDetected = rotationChange >= 5;

        if (isRotationDetected) {
            // 회전이 감지됨
            PdrActivity.sdh.distanceStep = 0.3f;
            Log.d("DeviceAttitudeHandler", "Rotation detected.");
            // 방향 추정을 다시 수행하는 로직 추가
            recalibrateDirectionEstimation();
        } else {
            // 회전이 감지되지 않음
            Log.d("DeviceAttitudeHandler", "No rotation detected.");
            PdrActivity.sdh.distanceStep = 0.75f;
        }
    }

    // 방향 추정 다시 수행하는 메서드
    private void recalibrateDirectionEstimation() {
        // 가속도와 자기장 데이터로부터 회전 행렬 계산
        float[] rotationMatrix = new float[9];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magneticField)) {
            // 회전 행렬을 이용하여 방향 추정
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
            if (!initialAzimuthSet) {
                Log.d("DeviceAttitudeHandler", "Initial azimuth: " + azimuth);
                initialAzimuthSet = true;
            }

            // 현재 회전 속도와 이전 회전 속도의 차이를 계산하여 회전 속도의 변화율을 추정
            float[] deltaAngularVelocity = new float[3];
            for (int i = 0; i < 3; i++) {
                deltaAngularVelocity[i] = previousAngularVelocity[i] - rotationAngleArray[i];
            }

            // 회전 속도의 변화율을 이용하여 다음 위치를 예측
            // 예시로 간단하게 현재 위치에 회전 속도의 변화율을 더하여 다음 위치를 계산하겠습니다.
            for (int i = 0; i < 3; i++) {
                rotationAngleArray[i] += deltaAngularVelocity[i];
            }
            Log.d("DeviceAttitudeHandler", "Next predicted rotation angle: " + Arrays.toString(rotationAngleArray));
        }
    }

    // 보폭 길이를 설정하는 메서드
    public static void setStepLength(float length) {
        stepLength = length;
    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private ExtendedKalmanFilter kalmanFilter;
    private float azimuth = 0.0f;
    private boolean initialAzimuthSet = false;
    private long previousTimestamp = 0;
    private float[] previousAngularVelocity = new float[3];
    private float[] rotationAngleArray = new float[3]; // 회전 방향각 배열 변수 추가

    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        kalmanFilter = new ExtendedKalmanFilter(new float[]{0, 0, 0}, new float[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}},
                new float[][]{{0.001f, 0, 0}, {0, 0.001f, 0}, {0, 0, 0.001f}},
                new float[][]{{0.01f, 0, 0}, {0, 0.01f, 0}, {0, 0, 0.01f}});
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            Log.d("DeviceAttitudeHandler", "Accelerometer event received");
            float[] acceleration = event.values;
            updateOrientation(acceleration);
        } else if (event.sensor == gyroscope) {
            Log.d("DeviceAttitudeHandler", "Gyroscope event received");
            float[] angularVelocity = event.values;
            long currentTimestamp = event.timestamp;
            if (previousTimestamp != 0) {
                float dt = (currentTimestamp - previousTimestamp) * 1.0e-9f; // nanoseconds to seconds
                Log.d("DeviceAttitudeHandler", "dt : " + dt);
                calculateRotationAngle(angularVelocity, dt); // 회전 방향각 배열 업데이트
                kalmanFilter.predict(rotationAngleArray); // 회전 방향각 배열로 예측
                Log.d("DeviceAttitudeHandler", "Predicted rotation angle: " + Arrays.toString(rotationAngleArray));
            }
            previousTimestamp = currentTimestamp;
            previousAngularVelocity = angularVelocity.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateOrientation(float[] acceleration) {
        if (!initialAzimuthSet) {
            float fx = acceleration[0];
            float fy = acceleration[1];
            azimuth = (float) Math.toDegrees(Math.atan2(fy, fx));
            Log.d("DeviceAttitudeHandler", "updateOrientation : " + azimuth);
            initialAzimuthSet = true;
        }
    }

    private void calculateRotationAngle(float[] angularVelocity, float dt) {
        // Calculate rotation angle using gyroscope data
        for (int i = 0; i < 3; i++) {
            rotationAngleArray[i] += angularVelocity[i] * dt;
        }
        Log.d("DeviceAttitudeHandler", "calculateRotationAngle : " + Arrays.toString(rotationAngleArray));
    }

    public float getAzimuth() {
        Log.d("DeviceAttitudeHandler", "azimuth : " + azimuth);
        return azimuth;
    }

    public float getYawRotationAngle() {
        return (float) (rotationAngleArray[0] * (180 / Math.PI)); // Yaw 값 반환
    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationVals = new float[3];
    private float[] angularVelocity = new float[3]; // 각속도
    private static final float ANGULAR_VELOCITY_THRESHOLD = 0.5f; // 회전 감지 임계값
    private boolean isRotating = false; // 회전 중인지 여부를 나타내는 변수
    private float currentOrientation; // 현재 방향 값

    // 상보 필터 관련 변수
    private static final float ALPHA = 0.3f; // 가중치
    private float[] filteredOrientation = new float[3]; // 상보 필터로 보정된 방향 값

    // 방향 값을 반환하는 메서드 추가
    public float getCurrentOrientation() {
        return currentOrientation;
    }

    public DeviceAttitudeHandler(SensorManager sensorManager) {

        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // 자이로스코프 센서 초기화
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
        } else if (event.sensor == gyroscope) { // 자이로스코프 센서 데이터 처리
            // 자이로스코프 데이터를 각속도로 변환
            angularVelocity = event.values.clone();

            // 각속도를 이용하여 회전 감지 등의 작업 수행
            detectRotation();
        }
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationVals);

            // 상보 필터를 사용하여 방향 값을 보정
            filteredOrientation[0] = ALPHA * orientationVals[0] + (1 - ALPHA) * filteredOrientation[0];
            filteredOrientation[1] = ALPHA * orientationVals[1] + (1 - ALPHA) * filteredOrientation[1];
            filteredOrientation[2] = ALPHA * orientationVals[2] + (1 - ALPHA) * filteredOrientation[2];

            // 방향 값을 더 정교하게 결정
            currentOrientation = calculateDirection(filteredOrientation[0], filteredOrientation[1], filteredOrientation[2]);

            // Do something with azimuth, pitch, and roll (e.g., pedestrian estimation)
            Log.d("PedestrianEstimation", "Azimuth: " + filteredOrientation[0] + ", Pitch: " + filteredOrientation[1] + ", Roll: " + filteredOrientation[2]);
        }
    }

    private void detectRotation() {
        float angularVelocityMagnitude = calculateAngularVelocityMagnitude();

        // 회전 감지 여부를 확인
        if (angularVelocityMagnitude > ANGULAR_VELOCITY_THRESHOLD) {
            // 회전 감지됨
            if (!isRotating) {
                // 회전이 시작된 경우
                Log.d("RotationDetection", "Rotation started");
                isRotating = true;
                // 회전 시작에 대한 작업 수행
            }
        } else {
            // 회전이 감지되지 않음
            if (isRotating) {
                // 회전이 끝난 경우
                Log.d("RotationDetection", "Rotation stopped");
                isRotating = false;
                // 회전 종료에 대한 작업 수행
            }
        }
    }

    private float calculateAngularVelocityMagnitude() {
        // 벡터의 크기(매개 변수로 전달된 각속도 벡터의 크기를 계산하는 메서드)
        return (float) Math.sqrt(angularVelocity[0] * angularVelocity[0] + angularVelocity[1] * angularVelocity[1] + angularVelocity[2] * angularVelocity[2]);
    }

    private float calculateDirection(float azimuth, float pitch, float roll) {
        // pitch와 roll을 고려하여 방향을 결정할 수 있습니다.

        // pitch와 roll의 절대값이 특정 임계값보다 크면 사용자가 움직인 것으로 간주합니다.
        float threshold = 0.2f; // 임계값 설정
        if (Math.abs(pitch) > threshold || Math.abs(roll) > threshold) {
            // pitch와 roll이 일정 임계값보다 크면 사용자가 움직인 것으로 간주하여 azimuth를 반환합니다.
            return azimuth;
        } else {
            // pitch와 roll이 작을 경우에는 사용자가 정지한 것으로 간주하여 0을 반환합니다.
            return 0;
        }
    }

    // 기존의 start() 및 stop() 메서드는 그대로 유지됩니다.
    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI); // 자이로스코프 센서 등록
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}*/


/*public class DeviceAttitudeHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationVals = new float[3];
    private float[] angularVelocity = new float[3]; // 각속도
    private static final float ANGULAR_VELOCITY_THRESHOLD = 0.1f; // 회전 감지 임계값
    private boolean isRotating = false; // 회전 중인지 여부를 나타내는 변수
    private float currentOrientation; // 현재 방향 값

    // 상보 필터 관련 변수
    private static float ALPHA = 0.5f; // 가중치
    private float[] filteredOrientation = new float[3]; // 상보 필터로 보정된 방향 값

    // 방향 값을 반환하는 메서드 추가
    public float getCurrentOrientation() {
        return currentOrientation;
    }

    public DeviceAttitudeHandler(SensorManager sensorManager) {

        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // 자이로스코프 센서 초기화
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
        } else if (event.sensor == gyroscope) { // 자이로스코프 센서 데이터 처리
            // 자이로스코프 데이터를 각속도로 변환
            angularVelocity = event.values.clone();

            // 사용자의 움직임에 따라 가중치 조정
            adjustAlpha();

            // 각속도를 이용하여 회전 감지 등의 작업 수행
            detectRotation();
        }
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationVals);

            // 상보 필터를 사용하여 방향 값을 보정
            filteredOrientation[0] = ALPHA * orientationVals[0] + (1 - ALPHA) * filteredOrientation[0];
            filteredOrientation[1] = ALPHA * orientationVals[1] + (1 - ALPHA) * filteredOrientation[1];
            filteredOrientation[2] = ALPHA * orientationVals[2] + (1 - ALPHA) * filteredOrientation[2];

            // 방향 값을 더 정교하게 결정
            currentOrientation = calculateDirection(filteredOrientation[0], filteredOrientation[1], filteredOrientation[2]);

            // Do something with azimuth, pitch, and roll (e.g., pedestrian estimation)
            Log.d("PedestrianEstimation", "Azimuth: " + filteredOrientation[0] + ", Pitch: " + filteredOrientation[1] + ", Roll: " + filteredOrientation[2]);
        }
    }

    private void adjustAlpha() {
        // 사용자의 움직임에 따라 가중치 조정
        // 사용자의 움직임이 크면 ALPHA 값을 낮추어서 가속도계와 자기장 센서 데이터에 더 큰 가중치를 줌
        // 여기서는 단순히 각속도의 크기를 기준으로 ALPHA 값을 조정하는 예시를 보여줍니다.
        float angularVelocityMagnitude = calculateAngularVelocityMagnitude();
        if (angularVelocityMagnitude > ANGULAR_VELOCITY_THRESHOLD) {
            ALPHA = 0.05f; // 움직임이 크면 가중치를 낮춤
        } else {
            ALPHA = 0.2f; // 움직임이 작으면 기본 가중치 사용
        }
    }

    private void detectRotation() {
        float angularVelocityMagnitude = calculateAngularVelocityMagnitude();

        // 회전 감지 여부를 확인
        if (angularVelocityMagnitude > ANGULAR_VELOCITY_THRESHOLD) {
            // 회전 감지됨
            if (!isRotating) {
                // 회전이 시작된 경우
                Log.d("RotationDetection", "Rotation started");
                isRotating = true;
                // 회전 시작에 대한 작업 수행
            }
        } else {
            // 회전이 감지되지 않음
            if (isRotating) {
                // 회전이 끝난 경우
                Log.d("RotationDetection", "Rotation stopped");
                isRotating = false;
                // 회전 종료에 대한 작업 수행
            }
        }
    }

    private float calculateAngularVelocityMagnitude() {
        // 벡터의 크기(매개 변수로 전달된 각속도 벡터의 크기를 계산하는 메서드)
        return (float) Math.sqrt(angularVelocity[0] * angularVelocity[0] + angularVelocity[1] * angularVelocity[1] + angularVelocity[2] * angularVelocity[2]);
    }

    private float calculateDirection(float azimuth, float pitch, float roll) {
        // pitch와 roll을 고려하여 방향을 결정할 수 있습니다.

        // pitch와 roll의 절대값이 특정 임계값보다 크면 사용자가 움직인 것으로 간주합니다.
        float threshold = 0.2f; // 임계값 설정
        if (Math.abs(pitch) > threshold || Math.abs(roll) > threshold) {
            // pitch와 roll이 일정 임계값보다 크면 사용자가 움직인 것으로 간주하여 azimuth를 반환합니다.
            return azimuth;
        } else {
            // pitch와 roll이 작을 경우에는 사용자가 정지한 것으로 간주하여 0을 반환합니다.
            return 0;
        }
    }

    // 기존의 start() 및 stop() 메서드는 그대로 유지됩니다.
    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI); // 자이로스코프 센서 등록
    }
    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationVals = new float[3];
    private float currentOrientation; // 현재 방향 값

    // 칼만 필터 관련 변수
    private KalmanFilter kalmanFilter;
    private double[][] transitionMatrix = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    private double[][] processNoise = {{0.01, 0, 0}, {0, 0.01, 0}, {0, 0, 0.01}};
    private double[][] measurementMatrix = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    private double[][] measurementNoise = {{0.01, 0, 0}, {0, 0.01, 0}, {0, 0, 0.01}};

    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 칼만 필터 초기화
        DefaultProcessModel processModel = new DefaultProcessModel(transitionMatrix, processNoise, processNoise);
        DefaultMeasurementModel measurementModel = new DefaultMeasurementModel(measurementMatrix, measurementNoise);
        kalmanFilter = new KalmanFilter(processModel, measurementModel);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationVals);

            // 칼만 필터를 사용하여 방향 값을 보정
            double[] measurement = {orientationVals[0], orientationVals[1], orientationVals[2]};
            kalmanFilter.predict();
            kalmanFilter.correct(measurement);

            // 보정된 방향 값을 얻기 위해 getStateEstimation() 메서드를 사용하여 상태를 가져옵니다.
            double[] correctedState = kalmanFilter.getStateEstimation();

            // 보정된 방향 값을 얻습니다.
            float correctedAzimuth = (float) correctedState[0]; // 예시로 첫 번째 요소를 azimuth로 사용

            // 방향 값을 더 정교하게 결정
            currentOrientation = calculateDirection(correctedAzimuth, orientationVals[1], orientationVals[2]);

            // Do something with azimuth, pitch, and roll (e.g., pedestrian estimation)
            Log.d("PedestrianEstimation", "Azimuth: " + correctedAzimuth + ", Pitch: " + orientationVals[1] + ", Roll: " + orientationVals[2]);
        }
    }
    private float calculateDirection(float azimuth, float pitch, float roll) {
        // pitch와 roll을 고려하여 방향을 결정할 수 있습니다.

        // pitch와 roll의 절대값이 특정 임계값보다 크면 사용자가 움직인 것으로 간주합니다.
        float threshold = 0.2f; // 임계값 설정
        if (Math.abs(pitch) > threshold || Math.abs(roll) > threshold) {
            // pitch와 roll이 일정 임계값보다 크면 사용자가 움직인 것으로 간주하여 azimuth를 반환합니다.
            return azimuth;
        } else {
            // pitch와 roll이 작을 경우에는 사용자가 정지한 것으로 간주하여 0을 반환합니다.
            return 0;
        }
    }
    // 방향 값을 반환하는 메서드 추가
    public float getCurrentOrientation() {
        return currentOrientation;
    }

    // 기존의 start() 및 stop() 메서드는 그대로 유지됩니다.
    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 시 필요한 작업 수행
    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationVals = new float[3];
    private float currentOrientation; // 현재 방향 값

    // 확장 칼만 필터 관련 변수
    private ExtendedKalmanFilter extendedKalmanFilter;

    // 기존의 filteredOrientationVals 배열 삭제

    // 방향 값을 반환하는 메서드 추가
    public float getCurrentOrientation() {
        return currentOrientation;
    }

    public DeviceAttitudeHandler(SensorManager sensorManager) {

        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 확장 칼만 필터 초기화
        double[][] transitionMatrix = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}; // 3차원 상태 벡터를 사용하기 때문에 행렬의 크기를 수정합니다.
        double[][] processNoise = {{0.01, 0, 0}, {0, 0.01, 0}, {0, 0, 0.01}}; // 예시로 설정된 값 사용
        double[][] measurementMatrix = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}; // 3차원 측정 벡터를 사용하기 때문에 행렬의 크기를 수정합니다.
        double[][] measurementNoise = {{0.01, 0, 0}, {0, 0.01, 0}, {0, 0, 0.01}}; // 예시로 설정된 값 사용
        extendedKalmanFilter = new ExtendedKalmanFilter(
                transitionMatrix, processNoise, measurementMatrix, measurementNoise);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationVals);

            // 확장 칼만 필터를 사용하여 방향 값을 보정
            extendedKalmanFilter.update(orientationVals);

            // 보정된 방향 값을 얻기 위해 getState() 메서드를 사용하여 상태를 가져옵니다.
            double[] correctedState = extendedKalmanFilter.getState();

            // 확장 칼만 필터에서는 보정된 방향 값을 직접 얻을 수 있습니다.
            float correctedAzimuth = (float) correctedState[0]; // 예시로 첫 번째 요소를 azimuth로 사용

            // 방향 값을 더 정교하게 결정
            currentOrientation = calculateDirection(correctedAzimuth, orientationVals[1], orientationVals[2]);

            // Do something with azimuth, pitch, and roll (e.g., pedestrian estimation)
            Log.d("PedestrianEstimation", "Azimuth: " + correctedAzimuth + ", Pitch: " + orientationVals[1] + ", Roll: " + orientationVals[2]);
        }
    }

    private float calculateDirection(float azimuth, float pitch, float roll) {
        // pitch와 roll을 고려하여 방향을 결정할 수 있습니다.

        // pitch와 roll의 절대값이 특정 임계값보다 크면 사용자가 움직인 것으로 간주합니다.
        float threshold = 0.25f; // 임계값 설정
        if (Math.abs(pitch) > threshold || Math.abs(roll) > threshold) {
            // pitch와 roll이 일정 임계값보다 크면 사용자가 움직인 것으로 간주하여 azimuth를 반환합니다.
            return azimuth;
        } else {
            // pitch와 roll이 작을 경우에는 사용자가 정지한 것으로 간주하여 0을 반환합니다.
            return 0;
        }
    }

    // 기존의 start() 및 stop() 메서드는 그대로 유지됩니다.
    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}*/

/*public class DeviceAttitudeHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] gyroscopeData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationVals = new float[3];

    private float currentOrientation; // 현재 방향 값

    // 칼만 필터 관련 변수
    private KalmanFilter kalmanFilter;
    private float[] filteredOrientationVals = new float[3];

    // 방향 값을 반환하는 메서드 추가
    public float getCurrentOrientation() {
        return currentOrientation;
    }

    public DeviceAttitudeHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 칼만 필터 초기화
        kalmanFilter = new KalmanFilter(0.01f); // Process noise 값을 적절히 조정해야 합니다.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeData, 0, event.values.length);
                break;
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationVals);

            // 칼만 필터를 사용하여 방향 값을 보정
            filteredOrientationVals = kalmanFilter.filter(orientationVals);

            float azimuth = filteredOrientationVals[0]; // Yaw
            float pitch = filteredOrientationVals[1]; // Pitch
            float roll = filteredOrientationVals[2]; // Roll

            // 방향 값을 더 정교하게 결정
            currentOrientation = calculateDirection(azimuth, pitch, roll);

            // Do something with azimuth, pitch, and roll (e.g., pedestrian estimation)
            Log.d("PedestrianEstimation", "Azimuth: " + azimuth + ", Pitch: " + pitch + ", Roll: " + roll);
        }
    }

    private float calculateDirection(float azimuth, float pitch, float roll) {
        // 여기에 방향을 정확하게 계산하는 코드를 추가합니다.
        // pitch와 roll을 고려하여 방향을 결정할 수 있습니다.

        // pitch와 roll의 절대값이 특정 임계값보다 크면 사용자가 움직인 것으로 간주합니다.
        float threshold = 0.3f; // 임계값 설정
        if (Math.abs(pitch) > threshold || Math.abs(roll) > threshold) {
            // pitch와 roll이 일정 임계값보다 크면 사용자가 움직인 것으로 간주하여 azimuth를 반환합니다.
            return azimuth;
        } else {
            // pitch와 roll이 작을 경우에는 사용자가 정지한 것으로 간주하여 0을 반환합니다.
            return 0;
        }
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}*/
