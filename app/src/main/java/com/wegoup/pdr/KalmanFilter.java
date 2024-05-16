package com.wegoup.pdr;

public class KalmanFilter {

    private float processNoise; // Process noise covariance

    private float[] state; // State vector [azimuth, pitch, roll]
    private float[][] covariance; // Covariance matrix

    public KalmanFilter(float processNoise) {
        this.processNoise = processNoise;

        // Initialize state vector and covariance matrix
        state = new float[3];
        covariance = new float[3][3];
        // Initialize covariance with high uncertainty for azimuth, pitch, and roll
        covariance[0][0] = 2;
        covariance[1][1] = 2;
        covariance[2][2] = 2;
    }

    public float[] filter(float[] measurement) {
        // Time update (Prediction)
        float[][] predictionCovariance = new float[3][3];
        for (int i = 0; i < 3; i++) {
            predictionCovariance[i][i] = covariance[i][i] + processNoise; // Process noise on each dimension
        }

        // Measurement update (Correction)
        float[] innovation = new float[3];
        for (int i = 0; i < 3; i++) {
            innovation[i] = measurement[i] - state[i]; // Innovation (measurement - predicted state)
        }
        float[][] innovationCovariance = new float[3][3];
        for (int i = 0; i < 3; i++) {
            innovationCovariance[i][i] = predictionCovariance[i][i] + 1; // Innovation covariance (predicted covariance + measurement noise)
        }
        float[] kalmanGain = new float[3];
        for (int i = 0; i < 3; i++) {
            kalmanGain[i] = predictionCovariance[i][i] / innovationCovariance[i][i]; // Kalman gain
        }

        for (int i = 0; i < 3; i++) {
            state[i] = state[i] + kalmanGain[i] * innovation[i]; // Update state
            covariance[i][i] = (1 - kalmanGain[i]) * predictionCovariance[i][i]; // Update covariance
        }

        return state;
    }
}