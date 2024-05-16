package com.wegoup.dijkstra;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LocationUtil {
    private String selectedStartLocation;
    private String selectedDestinationLocation;

    public LocationUtil() {
    }

    public static List<LatLng> getCoordinatesForLocation(Context context, String selectedLocation) {
        List<LatLng> coordinates = new ArrayList();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("1층.csv")));

            String line;
            try {
                label37:
                while((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    if (row[0].trim().equals(selectedLocation)) {
                        int i = 1;

                        while(true) {
                            if (i >= row.length) {
                                break label37;
                            }

                            double longitude = Double.parseDouble(row[i].trim());
                            double latitude = Double.parseDouble(row[i + 1].trim());
                            coordinates.add(new LatLng(latitude, longitude));
                            i += 2;
                        }
                    }
                }
            } catch (Throwable var12) {
                try {
                    reader.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }

                throw var12;
            }

            reader.close();
        } catch (IOException var13) {
            var13.printStackTrace();
        }

        return coordinates;
    }
}
