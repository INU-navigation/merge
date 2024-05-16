package com.wegoup.pdr;

import org.ejml.simple.SimpleMatrix;

public class ExtendedKalmanFilter {
    private SimpleMatrix stateVector; // 상태 벡터
    private SimpleMatrix covarianceMatrix; // 공분산 행렬
    private SimpleMatrix processNoiseMatrix; // 공정 잡음 행렬
    private SimpleMatrix measurementNoiseMatrix; // 측정 잡음 행렬

    public ExtendedKalmanFilter(float[] initialState, float[][] initialCovariance, float[][] processNoise,
                                float[][] measurementNoise) {
        stateVector = new SimpleMatrix(initialState.length, 1, true, initialState);
        covarianceMatrix = new SimpleMatrix(initialCovariance);
        processNoiseMatrix = new SimpleMatrix(processNoise);
        measurementNoiseMatrix = new SimpleMatrix(measurementNoise);
    }

    // 예측 메서드
    public void predict(float[] angularVelocity) {
        // 비선형 함수로 정의된 상태 전이 모델을 선형화하여 상태 전이 행렬 구성
        SimpleMatrix stateTransitionMatrix = computeStateTransitionMatrix(angularVelocity);

        // 상태 전이 행렬을 사용하여 예측 수행
        stateVector = stateTransitionMatrix.mult(stateVector);
        covarianceMatrix = stateTransitionMatrix.mult(covarianceMatrix).mult(stateTransitionMatrix.transpose())
                .plus(processNoiseMatrix);
    }

    // 업데이트 메서드
    public void update(float[] measurement) {
        // 측정 행렬을 구성 (선형 함수이므로 수정 불필요)
        SimpleMatrix measurementMatrix = new SimpleMatrix(measurement.length, stateVector.getNumElements());
        measurementMatrix.set(0, 0, 1);
        measurementMatrix.set(1, 1, 1);
        measurementMatrix.set(2, 2, 1);

        // 칼만 이득 계산
        SimpleMatrix innovationCovariance = measurementMatrix.mult(covarianceMatrix)
                .mult(measurementMatrix.transpose()).plus(measurementNoiseMatrix);
        SimpleMatrix kalmanGain = covarianceMatrix.mult(measurementMatrix.transpose())
                .mult(innovationCovariance.invert());

        // 예측값과 측정값의 차이 계산
        SimpleMatrix innovation = new SimpleMatrix(measurement.length, 1, true, measurement)
                .minus(measurementMatrix.mult(stateVector));

        // 상태 업데이트
        stateVector = stateVector.plus(kalmanGain.mult(innovation));
        covarianceMatrix = covarianceMatrix.minus(kalmanGain.mult(measurementMatrix).mult(covarianceMatrix));

        // 공분산 행렬의 대각 성분을 증가시킴 (예시로 모든 대각 성분을 2배로 증가)
        for (int i = 0; i < covarianceMatrix.numRows(); i++) {
            covarianceMatrix.set(i, i, covarianceMatrix.get(i, i) * 5);
        }
    }

    // 비선형 함수로 정의된 상태 전이 모델을 선형화하는 함수
    private SimpleMatrix computeStateTransitionMatrix(float[] angularVelocity) {
        // 비선형 함수를 사용하여 Jacobian 행렬을 계산
        double p = stateVector.get(0);
        double q = stateVector.get(1);
        double r = stateVector.get(2);
        double phi = Math.asin(-angularVelocity[1] / (Math.sqrt(p * p + r * r)));
        double theta = Math.asin(angularVelocity[0] / 9.81);

        double[][] jacobianArray = {
                {1, Math.sin(phi) * Math.tan(theta), Math.cos(phi) * Math.tan(theta)},
                {0, Math.cos(phi), -Math.sin(phi)},
                {0, Math.sin(phi) / Math.cos(theta), Math.cos(phi) / Math.cos(theta)}
        };

        // Jacobian 행렬을 SimpleMatrix 형태로 반환
        return new SimpleMatrix(jacobianArray);
    }
}

/*public class ExtendedKalmanFilter {

    // 확장 칼만 필터 관련 변수
    private KalmanFilter extendedKalmanFilterCore;

    public ExtendedKalmanFilter(double[][] transitionMatrix,
                                double[][] processNoise,
                                double[][] measurementMatrix,
                                double[][] measurementNoise) {

        // 초기 상태 추정 (3차원 상태 벡터 사용)
        RealVector state = new ArrayRealVector(new double[]{0, 0, 0});

        // 초기 오차 공분산 행렬 설정
        RealMatrix initialErrorCovariance = new Array2DRowRealMatrix(new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}});

        try {
            extendedKalmanFilterCore = new KalmanFilter(
                    new MyProcessModel(transitionMatrix, processNoise, state.toArray(), initialErrorCovariance),
                    new MyMeasurementModel(measurementMatrix, measurementNoise)
            );
        } catch (DimensionMismatchException e) {
            Log.e("ExtendedKalmanFilter", "Dimension mismatch during filter initialization");
            e.printStackTrace();
        }
    }

    public void update(double[] measurement) {
        // 초기 상태 추정 (3차원 상태 벡터 사용)
        RealVector state = new ArrayRealVector(new double[]{0, 0, 0});

        // 변환된 double[]을 사용하여 RealVector 생성
        RealVector measurementVector = new ArrayRealVector(measurement);

        if (extendedKalmanFilterCore != null) {
            extendedKalmanFilterCore.predict();

            // 칼만 필터의 상태 벡터 크기와 입력 벡터의 크기가 일치해야 합니다.
            if (measurementVector.getDimension() == state.getDimension()) {
                extendedKalmanFilterCore.correct(measurementVector);
            } else {
                // 오류 처리 로직 추가
                Log.e("ExtendedKalmanFilter", "Dimension mismatch between filter state and measurement vector");
            }
        } else {
            Log.e("ExtendedKalmanFilter", "Kalman filter is not initialized");
        }
    }

    public double[] getState() {
        if (extendedKalmanFilterCore != null) {
            return extendedKalmanFilterCore.getStateEstimation();
        } else {
            Log.e("ExtendedKalmanFilter", "Kalman filter is not initialized");
            return new double[0];
        }
    }

    private static class MyProcessModel implements ProcessModel {

        private final RealMatrix transitionMatrix;
        private final RealMatrix processNoise;
        private final RealVector initialState;
        private final RealMatrix initialErrorCovariance;

        public MyProcessModel(double[][] transitionMatrix,
                              double[][] processNoise,
                              double[] initialState,
                              RealMatrix initialErrorCovariance) {
            this.transitionMatrix = new Array2DRowRealMatrix(transitionMatrix);
            this.processNoise = new Array2DRowRealMatrix(processNoise);
            this.initialState = new ArrayRealVector(initialState);
            this.initialErrorCovariance = initialErrorCovariance;
        }

        @Override
        public RealMatrix getStateTransitionMatrix() {
            return transitionMatrix;
        }

        @Override
        public RealMatrix getControlMatrix() {
            return null;
        }

        @Override
        public RealMatrix getProcessNoise() {
            return processNoise;
        }

        @Override
        public RealVector getInitialStateEstimate() {
            return initialState;
        }

        @Override
        public RealMatrix getInitialErrorCovariance() {
            return initialErrorCovariance;
        }
    }

    private static class MyMeasurementModel implements MeasurementModel {

        private final RealMatrix measurementMatrix;
        private final RealMatrix measurementNoise;

        public MyMeasurementModel(double[][] measurementMatrix,
                                  double[][] measurementNoise) {
            this.measurementMatrix = new Array2DRowRealMatrix(measurementMatrix);
            this.measurementNoise = new Array2DRowRealMatrix(measurementNoise);
        }

        @Override
        public RealMatrix getMeasurementMatrix() {
            return measurementMatrix;
        }

        @Override
        public RealMatrix getMeasurementNoise() {
            return measurementNoise;
        }
    }
}*/