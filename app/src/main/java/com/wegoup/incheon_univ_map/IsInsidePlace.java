package com.wegoup.incheon_univ_map;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class IsInsidePlace {
    public boolean building7(LatLng location) {
        // 건물의 서쪽, 동쪽, 남쪽, 북쪽 경계를 설정합니다.
        LatLng southwest = new LatLng(37.37401, 126.63301); // 건물 서쪽 남쪽 경계
        LatLng northeast = new LatLng(37.3749, 126.63428); // 건물 동쪽 북쪽 경계
        LatLngBounds buildingBoundary = new LatLngBounds(southwest, northeast);

        if (location != null) {
            return buildingBoundary.contains(location);
        } else {
            Log.d("isInsideBuilding", "location is null");
            return false;
        }
    }

    public boolean univ(LatLng mapCenter) {
        // 건물의 서쪽, 동쪽, 남쪽, 북쪽 경계를 설정합니다.
        LatLng southwest = new LatLng(37.370589, 126.625092); // 건물 서쪽 남쪽 경계
        LatLng northeast = new LatLng(37.379550, 126.638119); // 건물 동쪽 북쪽 경계

        // 지도의 중심 위치가 특정 영역 안에 있는지 확인합니다.
        return (mapCenter.latitude >= southwest.latitude &&
                mapCenter.latitude <= northeast.latitude &&
                mapCenter.longitude >= southwest.longitude &&
                mapCenter.longitude <= northeast.longitude);
    }

}
