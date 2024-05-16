package com.wegoup.pdr;

import org.ejml.simple.SimpleMatrix;

public class UnscentedKalmanFilter {
    private SimpleMatrix stateVector; // 상태 벡터
    private SimpleMatrix covarianceMatrix; // 공분산 행렬
    private SimpleMatrix processNoiseMatrix; // 공정 잡음 행렬
    private SimpleMatrix measurementNoiseMatrix; // 측정 잡음 행렬
    private double alpha = 0.001; // 스케일링 파라미터 (튜닝 필요)
    private double kappa = 0; // 스케일링 파라미터 (튜닝 필요)
    private double beta = 2; // 최적화 파라미터 (가우시안 분포를 위해)

    public UnscentedKalmanFilter(double[] initialState, double[][] initialCovariance,
                                 double[][] processNoise, double[][] measurementNoise) {
        stateVector = new SimpleMatrix(initialState.length, 1, true, initialState);
        covarianceMatrix = new SimpleMatrix(initialCovariance);
        processNoiseMatrix = new SimpleMatrix(processNoise);
        measurementNoiseMatrix = new SimpleMatrix(measurementNoise);
    }

    // UKF의 상태 예측 메서드
    public void predict() {
        int n = stateVector.numRows(); // 상태 벡터의 차원
        double lambda = alpha * alpha * (n + kappa) - n;

        SimpleMatrix sigmaPoints = computeSigmaPoints(stateVector, covarianceMatrix, lambda, n);
        SimpleMatrix predictedSigmaPoints = predictSigmaPoints(sigmaPoints);

        updatePredictedState(predictedSigmaPoints, lambda, n);
    }

    private SimpleMatrix computeSigmaPoints(SimpleMatrix state, SimpleMatrix covariance, double lambda, int n) {
        SimpleMatrix sigmaPoints = new SimpleMatrix(n, 2 * n + 1);
        SimpleMatrix sqrtMatrix = covariance.scale(lambda + n).svd().getU().scale(Math.sqrt(lambda + n));

        sigmaPoints.insertIntoThis(0, 0, state);
        for (int i = 0; i < n; i++) {
            sigmaPoints.insertIntoThis(0, 1 + i, state.plus(sqrtMatrix.extractVector(false, i)));
            sigmaPoints.insertIntoThis(0, 1 + n + i, state.minus(sqrtMatrix.extractVector(false, i)));
        }
        return sigmaPoints;
    }

    private SimpleMatrix predictSigmaPoints(SimpleMatrix sigmaPoints) {
        int n = sigmaPoints.numRows();
        SimpleMatrix predictedSigmaPoints = new SimpleMatrix(n, 2 * n + 1);

        // 비선형 모델을 사용하여 각 Sigma 포인트를 전이 (임시로 같은 값)
        for (int i = 0; i < 2 * n + 1; i++) {
            predictedSigmaPoints.setColumn(i, 0, nonLinearTransition(sigmaPoints.extractVector(false, i).getDDRM().getData()));
        }
        return predictedSigmaPoints;
    }

    // 비선형 전이 함수 예제
    private double[] nonLinearTransition(double[] state) {
        // 사용자의 비선형 모델에 따라 수정
        double[] newState = new double[state.length];
        for (int i = 0; i < state.length; i++) {
            newState[i] = state[i]; // 임시 코드
        }
        return newState;
    }

    private void updatePredictedState(SimpleMatrix predictedSigmaPoints, double lambda, int n) {
        double lambdaPlusN = lambda + n;
        double w0_m = lambda / lambdaPlusN;
        double w0_c = w0_m + (1 - alpha * alpha + beta);
        double wi = 1 / (2 * lambdaPlusN);

        SimpleMatrix newState = new SimpleMatrix(n, 1);
        SimpleMatrix newCovariance = new SimpleMatrix(n, n);

        newState = predictedSigmaPoints.extractVector(false, 0).scale(w0_m);
        for (int i = 1; i < 2 * n + 1; i++) {
            newState = newState.plus(predictedSigmaPoints.extractVector(false, i).scale(wi));
        }

        SimpleMatrix diff;
        newCovariance = newCovariance.plus(predictedSigmaPoints.extractVector(false, 0).minus(newState).mult(predictedSigmaPoints.extractVector(false, 0).minus(newState).transpose()).scale(w0_c));
        for (int i = 1; i < 2 * n + 1; i++) {
            diff = predictedSigmaPoints.extractVector(false, i).minus(newState);
            newCovariance = newCovariance.plus(diff.mult(diff.transpose()).scale(wi));
        }

        stateVector = newState;
        covarianceMatrix = newCovariance.plus(processNoiseMatrix);
    }

    // UKF의 상태 업데이트 메서드
    public void update(float[] measurement) {
        SimpleMatrix measurementVector = new SimpleMatrix(measurement.length, 1, true, measurement);
        SimpleMatrix innovation = measurementVector.minus(stateVector);
        SimpleMatrix innovationCovariance = covarianceMatrix.plus(measurementNoiseMatrix);
        SimpleMatrix kalmanGain = covarianceMatrix.mult(innovationCovariance.invert());
        stateVector = stateVector.plus(kalmanGain.mult(innovation));
        covarianceMatrix = covarianceMatrix.minus(kalmanGain.mult(innovationCovariance).mult(kalmanGain.transpose()));
    }
}


/*public class UnscentedKalmanFilter {
    private SimpleMatrix x;  // State estimate
    private SimpleMatrix P;  // State covariance
    private final SimpleMatrix Q;  // Process noise covariance
    private final SimpleMatrix R;  // Measurement noise covariance

    private final int n;  // Dimension of the state
    private final double alpha;
    private final double beta;
    private final double kappa;

    private final double lambda_;

    private final double[] Wm;
    private final double[] Wc;

    public UnscentedKalmanFilter(double[] x0, double[][] P0, double[][] Q, double[][] R,
                                 double alpha, double beta, double kappa) {
        this.x = new SimpleMatrix(x0.length, 1, true, x0);  // State estimate
        this.P = new SimpleMatrix(P0);  // State covariance
        this.Q = new SimpleMatrix(Q);   // Process noise covariance
        this.R = new SimpleMatrix(R);   // Measurement noise covariance

        this.n = x0.length;  // Dimension of the state
        this.alpha = alpha;
        this.beta = beta;
        this.kappa = kappa;

        this.lambda_ = Math.pow(alpha, 2) * (n + kappa) - n;

        this.Wm = new double[2 * n + 1];
        this.Wc = new double[2 * n + 1];

        this.Wm[0] = lambda_ / (n + lambda_);
        for (int i = 1; i < 2 * n + 1; i++) {
            this.Wm[i] = 0.5 / (n + lambda_);
        }

        this.Wc[0] = lambda_ / (n + lambda_) + (1 - Math.pow(alpha, 2) + beta);
        for (int i = 1; i < 2 * n + 1; i++) {
            this.Wc[i] = 0.5 / (n + lambda_);
        }
    }

    public void predict(Function<SimpleMatrix, SimpleMatrix> f, SimpleMatrix B, SimpleMatrix u) {
        // Calculate sigma points
        SimpleMatrix[] sigma_points = calculate_sigma_points(x, P);

        // Propagate sigma points through the state transition function
        SimpleMatrix[] transformed_sigma_points = new SimpleMatrix[2 * n + 1];
        for (int i = 0; i < 2 * n + 1; i++) {
            transformed_sigma_points[i] = f.apply(sigma_points[i]);
        }

        // Calculate the predicted state and covariance
        SimpleMatrix predicted_x = new SimpleMatrix(n, 1);
        for (int i = 0; i < 2 * n + 1; i++) {
            predicted_x = predicted_x.plus(transformed_sigma_points[i].scale(Wm[i]));
        }

        SimpleMatrix predicted_P = new SimpleMatrix(n, n);
        for (int i = 0; i < 2 * n + 1; i++) {
            SimpleMatrix diff = transformed_sigma_points[i].minus(predicted_x);
            predicted_P = predicted_P.plus(diff.mult(diff.transpose()).scale(Wc[i]));
        }
        predicted_P = predicted_P.plus(Q);

        this.x = predicted_x;
        this.P = predicted_P;
    }

    public void update(float[] z, Function<SimpleMatrix, SimpleMatrix> h) {
        // Calculate sigma points
        SimpleMatrix[] sigma_points = calculate_sigma_points(x, P);

        // Propagate sigma points through the measurement function
        SimpleMatrix[] transformed_sigma_points = new SimpleMatrix[2 * n + 1];
        for (int i = 0; i < 2 * n + 1; i++) {
            transformed_sigma_points[i] = h.apply(sigma_points[i]);
        }

        // Calculate the predicted measurement and its covariance
        SimpleMatrix y = new SimpleMatrix(n, 1);
        for (int i = 0; i < 2 * n + 1; i++) {
            y = y.plus(transformed_sigma_points[i].scale(Wm[i]));
        }

        SimpleMatrix Pyy = new SimpleMatrix(n, n);
        SimpleMatrix Pxy = new SimpleMatrix(n, n);
        for (int i = 0; i < 2 * n + 1; i++) {
            SimpleMatrix diff_y = transformed_sigma_points[i].minus(y);
            SimpleMatrix diff_sigma = sigma_points[i].minus(x);
            Pyy = Pyy.plus(diff_y.mult(diff_y.transpose()).scale(Wc[i]));
            Pxy = Pxy.plus(diff_sigma.mult(diff_y.transpose()).scale(Wc[i]));
        }
        Pyy = Pyy.plus(R);

        // Calculate the Kalman gain and update the state and covariance
        SimpleMatrix K;
        try {
            K = Pxy.mult(Pyy.invert());
        } catch (Exception e) {
            // Handle exception
            return;
        }
        SimpleMatrix z_matrix = new SimpleMatrix(z.length, 1, true, z);
        SimpleMatrix innovation = z_matrix.minus(y);

        this.x = x.plus(K.mult(innovation));
        this.P = P.minus(K.mult(Pyy).mult(K.transpose()));
    }

    private SimpleMatrix[] calculate_sigma_points(SimpleMatrix x, SimpleMatrix P) {
        int n = x.getNumElements();
        SimpleMatrix[] sigma_points = new SimpleMatrix[2 * n + 1];

        sigma_points[0] = x;

        SimpleMatrix S = P.scale(n + lambda_);
        SimpleMatrix sqrt_S;
        try {
            CholeskyDecomposition_F64 cholesky = DecompositionFactory_DDRM.chol(n, true);
            cholesky.decompose(S.getDDRM());
            sqrt_S = SimpleMatrix.wrap(cholesky.getT(null)).transpose();
        } catch (Exception e) {
            // Handle exception
            return sigma_points;
        }

        for (int i = 0; i < n; i++) {
            double[] sqrt_S_column = new double[n];
            for (int j = 0; j < n; j++) {
                sqrt_S_column[j] = sqrt_S.get(j, i);
            }
            sigma_points[i + 1] = x.plus(new SimpleMatrix(n, 1, true, sqrt_S_column));
            for (int j = 0; j < n; j++) {
                sqrt_S_column[j] = -sqrt_S.get(j, i);
            }
            sigma_points[i + 1 + n] = x.plus(new SimpleMatrix(n, 1, true, sqrt_S_column));
        }

        return sigma_points;
    }
}*/