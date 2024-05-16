package com.wegoup.pdr;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepDetectionHandler extends Activity implements SensorEventListener {
    private SensorManager sm;
    private Sensor sensor;
    private StepDetectionListener mStepDetectionListener;
    private DeviceAttitudeHandler dah;
    private int step = 0;
    private float[] filteredValues = {0.0f, 0.0f}; // x, y 축의 지수 이동 평균 필터링된 값
    private static final float ALPHA = 0.2f; // 지수 이동 평균 필터의 가중치
    private static final float THRESHOLD_LOWERX = 0.05f; // 초기 임계값의 하한
    private static final float THRESHOLD_LOWERY = 0.5f; // 초기 임계값의 하한
    private static final float THRESHOLD_UPPER = 1.0f; // 초기 임계값의 상한
    private float initialThresholdX = 0.15f; // 초기 임계값
    private float initialThresholdY = 1.0f; // 초기 임계값
    public float distanceStep = 0.75f;

    public void setDistanceStep(float stepSize){
        distanceStep = stepSize;
    }

    public StepDetectionHandler(SensorManager sm) {
        super();
        this.sm = sm;
        sensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        dah = new DeviceAttitudeHandler(sm);
    }

    public void start() {
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sm.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 시 동작을 정의할 수 있습니다.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x, y;

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            x = event.values[0];
            y = event.values[1];

            // x 축과 y 축에 대해 각각 지수 이동 평균 필터링 적용
            filteredValues[0] = applyExponentialMovingAverageFilter(x, filteredValues[0]);
            filteredValues[1] = applyExponentialMovingAverageFilter(y, filteredValues[1]);


            // 걸음 감지를 위한 임계값 동적 조절
            adjustThreshold();

            // 걸음 감지를 위한 임계값 설정
            float thresholdX = initialThresholdX;
            float thresholdY = initialThresholdY;
            Log.d("Walking", "thrsholdX : " + filteredValues[0] + ", thrsholdY : " + filteredValues[1]);
            Log.d("Walking", "walking : " + (filteredValues[0] > thresholdX && filteredValues[1] > thresholdY));

            // 걸음이 시작될 때만 isWalking을 true로 변경
            if (filteredValues[0] > thresholdX && filteredValues[1] > thresholdY && mStepDetectionListener != null) {
                if (!WalkingStatusManager.isWalking()) {
                    // 걸음이 시작될 때만 isWalking을 true로 변경
                    WalkingStatusManager.setWalking(true);
                    onNewStepDetected();
                }
            } else {
                if (WalkingStatusManager.isWalking()) {
                    // 걸음이 끝났을 때 isWalking을 false로 변경
                    WalkingStatusManager.setWalking(false);
                }
            }
        }
    }

    // 지수 이동 평균 필터 적용 메서드
    private float applyExponentialMovingAverageFilter(float rawValue, float previousFilteredValue) {
        return ALPHA * rawValue + (1 - ALPHA) * previousFilteredValue;
    }

    // 임계값 동적 조절 메서드
    private void adjustThreshold() {
        // 걸음 수가 일정 범위 내에서 변화하는지 확인하고 임계값 동적으로 조절
        // 예를 들어, 현재는 임의의 조건을 기반으로 임계값을 동적으로 조절하는 예시를 보여줍니다.
        if (step >= 100 && step <= 200) {
            // 걸음 수가 100에서 200 사이인 경우, 임계값을 높입니다.
            initialThresholdX = Math.min(initialThresholdX + 0.1f, THRESHOLD_UPPER);
            initialThresholdY= Math.min(initialThresholdY + 0.1f, THRESHOLD_UPPER);
            distanceStep = Math.min(distanceStep + 0.1f, THRESHOLD_UPPER); // 보폭 길이도 조절
        } else if (step > 200) {
            // 걸음 수가 200보다 큰 경우, 임계값을 더 높입니다.
            initialThresholdX = Math.min(initialThresholdX + 0.2f, THRESHOLD_UPPER);
            initialThresholdY = Math.min(initialThresholdY + 0.2f, THRESHOLD_UPPER);
            distanceStep = Math.min(distanceStep + 0.2f, THRESHOLD_UPPER); // 보폭 길이도 조절
        } else {
            // 그 외의 경우, 임계값을 초기 값으로 복원합니다.
            initialThresholdX = THRESHOLD_LOWERX;
            initialThresholdY = THRESHOLD_LOWERY;
        }
    }

    // 새로운 걸음이 감지될 때 호출되는 메서드
    private void onNewStepDetected() {
        step++;
        if (mStepDetectionListener != null) {
            mStepDetectionListener.newStep(distanceStep); // 전달할 stepSize 값 변경
        }
    }

    // 걸음 감지 리스너 설정
    public void setStepListener(StepDetectionListener listener) {
        mStepDetectionListener = listener;
    }

    // 걸음 감지 리스너 인터페이스 정의
    public interface StepDetectionListener {
        void newStep(float stepSize);
    }
}