package com.wegoup.pdr;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class StepPositioningHandler {
    private static final double EARTH_RADIUS = 6371000;
    private float initialAzimuth = Float.NaN;
    private float direction;
    private float distance;
    private float previousRotationAngle = 0.0f;

    public void setInitialAzimuth(float azimuth) {
        direction = azimuth;
    }

    public LatLng computeNextStep(Location currentLocation, double stepSize, float currentRotationAngle) {

        Log.d("RotationAngle", "CurrentRotationAngle = " + currentRotationAngle + "previousRotationAngle = " + previousRotationAngle);
        Log.d("RotationAngle", "direction+= = " + Math.abs((float) (Math.toDegrees(currentRotationAngle) - (float) Math.toDegrees(previousRotationAngle))));

        direction += (float) Math.toDegrees(currentRotationAngle);

        PdrActivity.sdh.setDistanceStep(0.7f);
        double angularDistance = stepSize / EARTH_RADIUS;

        double oldLatitude = currentLocation.getLatitude();
        double oldLongitude = currentLocation.getLongitude();

        double newLatitude = Math.asin(Math.sin(Math.toRadians(oldLatitude)) * Math.cos(angularDistance) +
                    Math.cos(Math.toRadians(oldLatitude)) * Math.sin(angularDistance) * Math.cos(Math.toRadians(direction)));
        double newLongitude = Math.toRadians(oldLongitude) +
                    Math.atan2(Math.sin(Math.toRadians(direction)) * Math.sin(angularDistance) * Math.cos(Math.toRadians(oldLatitude)),
                            Math.cos(angularDistance) - Math.sin(Math.toRadians(oldLatitude)) * Math.sin(newLatitude));

        newLatitude = Math.toDegrees(newLatitude);
        newLongitude = Math.toDegrees(newLongitude);

        Location oldLocation = new Location("");
        oldLocation.setLatitude(oldLatitude);
        oldLocation.setLongitude(oldLongitude);
        Location newLocation = new Location("");
        newLocation.setLatitude(newLatitude);
        newLocation.setLongitude(newLongitude);
        distance = oldLocation.distanceTo(newLocation);

        if (Math.abs((float) (Math.toDegrees(currentRotationAngle) - (float) Math.toDegrees(previousRotationAngle))) >= 5.0) {
            Log.d("stepSize", "stepSize = " + 0.5);
            setInitialAzimuth(PdrActivity.dah.getAzimuth());
            PdrActivity.sdh.setDistanceStep(0.85f);
        } else if (Math.abs((float) (Math.toDegrees(currentRotationAngle) - (float) Math.toDegrees(previousRotationAngle))) >= 20.0) {
            setInitialAzimuth(PdrActivity.dah.getAzimuth());
            PdrActivity.sdh.setDistanceStep(0.05f);
            Log.d("stepSize", "stepSize = " + 0.01);
        }

        previousRotationAngle = currentRotationAngle;

        return new LatLng(newLatitude, newLongitude);
    }
}
