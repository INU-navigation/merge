package com.wegoup.pdr;

public class WalkingStatusManager {
    public static boolean isWalking = false;

    public static boolean isWalking() {
        return isWalking;
    }

    public static void setWalking(boolean walking) {
        isWalking = walking;
    }
}
