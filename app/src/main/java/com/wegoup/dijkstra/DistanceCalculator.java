package com.wegoup.dijkstra;

public class DistanceCalculator {
    private static final double RADIUS_OF_EARTH_KM = 6371.0;
    private static final double M_PER_KM = 1000.0;

    public DistanceCalculator() {
    }

    private static double toRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2.0), 2.0) + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.pow(Math.sin(dLon / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distanceKm = 6371.0 * c;
        double distanceM = distanceKm * 1000.0;
        return distanceM;
    }
}

