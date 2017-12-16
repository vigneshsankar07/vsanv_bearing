package com.example.test.google;

/**
 * Created by test on 31/7/17.
 **/

public class Constants {
    public static final String MY_PREFS_NAME ="Mapview";

    public static final String Google_API_KEY   = "AIzaSyAD3Dz6tLDdFXLiiRqKu9ilkZOPVlbEOu4";



    // Splash screen timer
    public static int SPLASH_TIME_OUT = 2000;

    // Splash screen timer
    public static boolean MAP_ISSHOWING = false;

    // The minimum distance to change Updates in meters
    public static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    public static final long MIN_TIME_BW_UPDATES = 1000 * 2; //2 Seconds

    public static final long updateLocationToFBHandlerTime = 1000 * 10; //10 Seconds

    //gps turn on
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    public static final long SET_INTERVAL = 5000; //5 Seconds
    public static final long SET_FASTESTINTERVAL = 3000; //3 Seconds
    public static int GET_ZOOM_TIME = 4000;

    //Map Zooming Size
    public static final float MAP_ZOOM_SIZE = 14;

    public static final float MAP_ZOOM_SIZE_ONTRIP = 17;
}
