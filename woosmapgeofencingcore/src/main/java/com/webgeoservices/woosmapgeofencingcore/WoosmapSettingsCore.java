package com.webgeoservices.woosmapgeofencingcore;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class WoosmapSettingsCore {
    public static void saveSettings(Context context) {
        saveCoreSetting(context);
    }

    protected static void saveCoreSetting(Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.clear();
        prefsEditor.putBoolean("modeHighFrequencyLocationEnable", modeHighFrequencyLocation);
        prefsEditor.putBoolean("trackingEnable", trackingEnable);
        prefsEditor.putInt("currentLocationTimeFilter", currentLocationTimeFilter);
        prefsEditor.putInt("currentLocationDistanceFilter", currentLocationDistanceFilter);
        prefsEditor.putBoolean("visitEnable", visitEnable);
        prefsEditor.putBoolean("creationOfZOIEnable", creationOfZOIEnable);
        prefsEditor.putInt("distanceTimeFilter", distanceTimeFilter);
        prefsEditor.putInt("distanceMaxAirDistanceFilter", distanceMaxAirDistanceFilter);
        prefsEditor.putBoolean("distanceAPIEnable", distanceAPIEnable);
        prefsEditor.putString("distanceMode", distanceMode);
        prefsEditor.putString("trafficDistanceRouting", trafficDistanceRouting);
        prefsEditor.putString("distanceProvider", distanceProvider);
        prefsEditor.putString("distanceUnits", distanceUnits);
        prefsEditor.putString("distanceLanguage", distanceLanguage);
        prefsEditor.putInt("accuracyFilter", accuracyFilter);
        prefsEditor.putInt("outOfTimeDelay", outOfTimeDelay);
        prefsEditor.putFloat("distanceDetectionThresholdVisits", (float) distanceDetectionThresholdVisits);
        prefsEditor.putLong("minDurationVisitDisplay", minDurationVisitDisplay);
        prefsEditor.putLong("numberOfDayDataDuration", numberOfDayDataDuration);
        prefsEditor.putBoolean("classificationEnable", classificationEnable);
        prefsEditor.putInt("radiusDetectionClassifiedZOI", radiusDetectionClassifiedZOI);
        prefsEditor.putString("privateKeyGMPStatic", privateKeyGMPStatic);
        prefsEditor.putString("privateKeyWoosmapAPI", privateKeyWoosmapAPI);
        prefsEditor.putString("WoosmapURL", WoosmapURL);
        prefsEditor.putString("SearchAPIUrl", SearchAPIUrl);
        prefsEditor.putString("DistanceAPIUrl", DistanceAPIUrl);
        prefsEditor.putString("TrafficDistanceAPIUrl", TrafficDistanceAPIUrl);
        prefsEditor.putString("GoogleMapStaticUrl", GoogleMapStaticUrl);
        prefsEditor.putString("GoogleMapStaticUrl1POI", GoogleMapStaticUrl1POI);
        prefsEditor.putBoolean("checkIfPositionIsInsideGeofencingRegionsEnable", checkIfPositionIsInsideGeofencingRegionsEnable);
        prefsEditor.putBoolean("foregroundLocationServiceEnable", foregroundLocationServiceEnable);
        prefsEditor.putString("updateServiceNotificationTitle", updateServiceNotificationTitle);
        prefsEditor.putString("updateServiceNotificationText", updateServiceNotificationText);
        prefsEditor.putString("WoosmapNotificationChannelID", WoosmapNotificationChannelID);
        prefsEditor.putString("WoosmapNotificationChannelName", WoosmapNotificationChannelName);
        prefsEditor.putString("WoosmapNotificationDescriptionChannel", WoosmapNotificationDescriptionChannel);
        prefsEditor.putBoolean("WoosmapNotificationActive", WoosmapNotificationActive);


        //convert to string using gson
        Gson gson = new Gson();
        String searchAPIHashMapString = gson.toJson(searchAPIParameters);
        prefsEditor.putString("searchAPIParameters", searchAPIHashMapString).apply();
        String userPropertiesHashMapString = gson.toJson(userPropertiesFilter);
        prefsEditor.putString("userPropertiesFilter", userPropertiesHashMapString).apply();


        prefsEditor.commit();
    }

    public static void loadSettings(Context context) {
        loadCoreSettings(context);

    }

    protected static void loadCoreSettings(Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref", Context.MODE_PRIVATE);

        WoosmapSettingsCore.modeHighFrequencyLocation = mPrefs.getBoolean("modeHighFrequencyLocationEnable", WoosmapSettingsCore.modeHighFrequencyLocation);
        WoosmapSettingsCore.trackingEnable = mPrefs.getBoolean("trackingEnable", WoosmapSettingsCore.trackingEnable);
        WoosmapSettingsCore.currentLocationTimeFilter = mPrefs.getInt("currentLocationTimeFilter", WoosmapSettingsCore.currentLocationTimeFilter);
        WoosmapSettingsCore.currentLocationDistanceFilter = mPrefs.getInt("currentLocationDistanceFilter", WoosmapSettingsCore.currentLocationDistanceFilter);

        WoosmapSettingsCore.visitEnable = mPrefs.getBoolean("visitEnable", WoosmapSettingsCore.visitEnable);
        WoosmapSettingsCore.creationOfZOIEnable = mPrefs.getBoolean("creationOfZOIEnable", WoosmapSettingsCore.creationOfZOIEnable);
        WoosmapSettingsCore.distanceMaxAirDistanceFilter = mPrefs.getInt("distanceMaxAirDistanceFilter", WoosmapSettingsCore.distanceMaxAirDistanceFilter);
        WoosmapSettingsCore.distanceAPIEnable = mPrefs.getBoolean("distanceAPIEnable", WoosmapSettingsCore.distanceAPIEnable);
        WoosmapSettingsCore.trafficDistanceRouting = mPrefs.getString("trafficDistanceRouting", WoosmapSettingsCore.trafficDistanceRouting);
        WoosmapSettingsCore.trafficDistanceMethod = mPrefs.getString("trafficDistanceMethod", WoosmapSettingsCore.trafficDistanceMethod);
        WoosmapSettingsCore.distanceProvider = mPrefs.getString("distanceProvider", WoosmapSettingsCore.distanceProvider);
        WoosmapSettingsCore.distanceUnits = mPrefs.getString("distanceUnits", WoosmapSettingsCore.distanceUnits);
        WoosmapSettingsCore.distanceLanguage = mPrefs.getString("distanceLanguage", WoosmapSettingsCore.distanceLanguage);
        WoosmapSettingsCore.distanceMode = mPrefs.getString("distanceMode", WoosmapSettingsCore.distanceMode);
        WoosmapSettingsCore.accuracyFilter = mPrefs.getInt("accuracyFilter", WoosmapSettingsCore.accuracyFilter);
        WoosmapSettingsCore.outOfTimeDelay = mPrefs.getInt("outOfTimeDelay", WoosmapSettingsCore.outOfTimeDelay);
        WoosmapSettingsCore.distanceDetectionThresholdVisits = mPrefs.getFloat("distanceDetectionThresholdVisits", (float) WoosmapSettingsCore.distanceDetectionThresholdVisits);
        WoosmapSettingsCore.minDurationVisitDisplay = mPrefs.getLong("minDurationVisitDisplay", WoosmapSettingsCore.minDurationVisitDisplay);
        WoosmapSettingsCore.numberOfDayDataDuration = mPrefs.getLong("numberOfDayDataDuration", WoosmapSettingsCore.minDurationVisitDisplay);
        WoosmapSettingsCore.classificationEnable = mPrefs.getBoolean("classificationEnable", WoosmapSettingsCore.classificationEnable);
        WoosmapSettingsCore.radiusDetectionClassifiedZOI = mPrefs.getInt("radiusDetectionClassifiedZOI", WoosmapSettingsCore.radiusDetectionClassifiedZOI);
        WoosmapSettingsCore.privateKeyGMPStatic = mPrefs.getString("privateKeyGMPStatic", WoosmapSettingsCore.privateKeyGMPStatic);
        WoosmapSettingsCore.privateKeyWoosmapAPI = mPrefs.getString("privateKeyWoosmapAPI", WoosmapSettingsCore.privateKeyWoosmapAPI);
        WoosmapSettingsCore.WoosmapURL = mPrefs.getString("WoosmapURL", WoosmapSettingsCore.WoosmapURL);
        WoosmapSettingsCore.SearchAPIUrl = mPrefs.getString("SearchAPIUrl", WoosmapSettingsCore.SearchAPIUrl);
        WoosmapSettingsCore.DistanceAPIUrl = mPrefs.getString("DistanceAPIUrl", WoosmapSettingsCore.DistanceAPIUrl);
        WoosmapSettingsCore.TrafficDistanceAPIUrl = mPrefs.getString("TrafficDistanceAPIUrl", WoosmapSettingsCore.TrafficDistanceAPIUrl);
        WoosmapSettingsCore.GoogleMapStaticUrl = mPrefs.getString("GoogleMapStaticUrl", WoosmapSettingsCore.GoogleMapStaticUrl);
        WoosmapSettingsCore.GoogleMapStaticUrl1POI = mPrefs.getString("GoogleMapStaticUrl1POI", WoosmapSettingsCore.GoogleMapStaticUrl1POI);
        WoosmapSettingsCore.checkIfPositionIsInsideGeofencingRegionsEnable = mPrefs.getBoolean("checkIfPositionIsInsideGeofencingRegionsEnable", WoosmapSettingsCore.checkIfPositionIsInsideGeofencingRegionsEnable);
        WoosmapSettingsCore.foregroundLocationServiceEnable = mPrefs.getBoolean("foregroundLocationServiceEnable", WoosmapSettingsCore.foregroundLocationServiceEnable);
        WoosmapSettingsCore.updateServiceNotificationTitle = mPrefs.getString("updateServiceNotificationTitle", WoosmapSettingsCore.updateServiceNotificationTitle);
        WoosmapSettingsCore.updateServiceNotificationText = mPrefs.getString("updateServiceNotificationText", WoosmapSettingsCore.updateServiceNotificationText);
        WoosmapSettingsCore.WoosmapNotificationChannelID = mPrefs.getString("WoosmapNotificationChannelID", WoosmapSettingsCore.WoosmapNotificationChannelID);
        WoosmapSettingsCore.WoosmapNotificationChannelName = mPrefs.getString("WoosmapNotificationChannelName", WoosmapSettingsCore.WoosmapNotificationChannelID);
        WoosmapSettingsCore.WoosmapNotificationDescriptionChannel = mPrefs.getString("WoosmapNotificationDescriptionChannel", WoosmapSettingsCore.WoosmapNotificationDescriptionChannel);
        WoosmapSettingsCore.WoosmapNotificationActive = mPrefs.getBoolean("WoosmapNotificationActive", WoosmapSettingsCore.WoosmapNotificationActive);


        //convert to string using gson
        Gson gson = new Gson();

        String searchAPIHashMapString = gson.toJson(searchAPIParameters);
        String userPropertiesHashMapString = gson.toJson(userPropertiesFilter);
        String searchAPIParametersString = mPrefs.getString("searchAPIParameters", searchAPIHashMapString);
        WoosmapSettingsCore.searchAPIParameters = gson.fromJson(searchAPIParametersString, searchAPIParameters.getClass());
        String userPropertiesFilterString = mPrefs.getString("userPropertiesFilter", userPropertiesHashMapString);
        WoosmapSettingsCore.userPropertiesFilter = gson.fromJson(userPropertiesFilterString, userPropertiesFilter.getClass());


    }

    static public String AndroidDeviceModel = "android";
    static public String PositionDateFormat = "yyyy-MM-dd'T'HH:mm:ss Z";
    static public final String WoosmapNotification = "woosmapNotification";
    static public String WoosmapNotificationChannelID = "Location Channel ID";
    static public String WoosmapNotificationChannelName = "Location Channel Name";
    static public String WoosmapNotificationDescriptionChannel = "Location Channel";
    static public boolean WoosmapNotificationActive = false;

    //Enable/disable Location
    static public boolean modeHighFrequencyLocation = false;

    //Enable/disable Location
    static public boolean trackingEnable = false;

    //filter time to refresh user location
    static public int currentLocationTimeFilter = 0;

    //filter distance to refresh user location
    static public int currentLocationDistanceFilter = 0;

    //Enable/disable VisitEnable
    static public boolean visitEnable = false;

    //Enable/disable Creation of ZOI
    static public boolean creationOfZOIEnable = false;


    public static int getDistanceMaxAirDistanceFilter() {
        return distanceMaxAirDistanceFilter;
    }

    public static void setDistanceMaxAirDistanceFilter(int distanceMaxAirDistanceFilter) {
        WoosmapSettingsCore.distanceMaxAirDistanceFilter = distanceMaxAirDistanceFilter;
    }

    //filter distance to request Distance API
    static public int distanceMaxAirDistanceFilter = 1000000;

    public static int getDistanceTimeFilter() {
        return distanceTimeFilter;
    }

    public static void setDistanceTimeFilter(int distanceTimeFilter) {
        WoosmapSettingsCore.distanceTimeFilter = distanceTimeFilter;
    }

    //the minimum time to wait between 2 requests to the distance provider.
    static public int distanceTimeFilter = 0;


    //Enable/disable DistanceAPI
    static public boolean distanceAPIEnable = true;


    //Mode transportation DistanceAPI
    private static final String drivingMode = "driving";
    private static final String walkingMode = "walking";
    private static final String cyclingMode = "cycling";
    private static final String truckMode = "truck";
    static public String distanceMode = drivingMode;

    //Distance Provider
    /***
     * @deprecated
     * Setting the value of `distanceProvider` property is now deprecated.
     * Woosmap Distance API will always be used as the provider.
     */
    @Deprecated
    public static final String woosmapDistance = "WoosmapDistance";

    /***
     * @deprecated
     * Setting the value of `distanceProvider` property is now deprecated.
     * Woosmap Distance API will always be used as the provider.
     */
    @Deprecated
    public static final String woosmapTraffic = "WoosmapTraffic";
    /***
     * @deprecated `distanceProvider` property is now deprecated. Woosmap Distance API will always be used as the provider.
     */
    @Deprecated
    static public String distanceProvider = woosmapDistance;

    /***
     * @deprecated Set `time` value to `trafficDistanceMethod` setting instead
     */
    @Deprecated
    protected static final String fastest = "fastest";
    /***
     * @deprecated Set `distance` value to `trafficDistanceMethod` setting instead
     */
    @Deprecated
    protected static final String balanced = "balanced";
    /***
     * @deprecated Use `trafficDistanceMethod` instead.
     */
    @Deprecated
    static public String trafficDistanceRouting = fastest;

    //Disatnce method
    protected static final String time = "time";
    protected static final String distance = "distance";
    static protected String trafficDistanceMethod = time;

    //Distance Language
    static public String distanceLanguage = "en";

    //Distance Units
    private static final String metric = "metric";
    private static final String imperial = "imperial";
    static public String distanceUnits = metric;

    public static void setDistanceMode(String distanceMode) {
        if (distanceMode.equals(drivingMode) || distanceMode.equals(walkingMode) || distanceMode.equals(cyclingMode) || distanceMode.equals(truckMode)) {
            WoosmapSettingsCore.distanceMode = distanceMode;
        } else {
            WoosmapSettingsCore.distanceMode = drivingMode;
        }

    }

    /***
     * Setting the distance provider is now deprecated. The SDK will now always use Distance API as distance provider.
     * The value you set here will be ignored. If you need to get the distance with traffic considerations then pass `distanceWithTraffic` as `true` to
     * `PositionManagerCore` class' `calculateDistance` method
     * @param distanceProvider
     */
    @Deprecated
    public static void setDistanceProvider(String distanceProvider) {
        if (distanceProvider.equals(woosmapDistance) || distanceProvider.equals(woosmapTraffic)) {
            WoosmapSettingsCore.distanceProvider = distanceProvider;
        } else {
            WoosmapSettingsCore.distanceProvider = woosmapDistance;
        }
    }

    @Deprecated
    public static void setTrafficDistanceRouting(String trafficDistanceRouting) {
        if (trafficDistanceRouting.equals(fastest) || trafficDistanceRouting.equals(balanced)) {
            WoosmapSettingsCore.trafficDistanceRouting = trafficDistanceRouting;
        } else {
            WoosmapSettingsCore.trafficDistanceRouting = fastest;
        }
    }

    public static void setTrafficDistanceMethod(String trafficDistanceMethod) {
        if (trafficDistanceMethod.equals(time) || trafficDistanceMethod.equals(distance)) {
            WoosmapSettingsCore.trafficDistanceMethod = trafficDistanceMethod;
        } else {
            WoosmapSettingsCore.trafficDistanceMethod = time;
        }
    }

    public static String getTrafficDistanceMethod(){
        return WoosmapSettingsCore.trafficDistanceMethod;
    }

    public static void setDistanceLanguage(String distanceLanguage) {
        WoosmapSettingsCore.distanceLanguage = distanceLanguage;
    }

    public static void setDistanceUnits(String distanceUnits) {
        if (distanceUnits.equals(imperial) || distanceUnits.equals(metric)) {
            WoosmapSettingsCore.distanceUnits = distanceUnits;
        } else {
            WoosmapSettingsCore.distanceUnits = metric;
        }

    }

    public static String getDistanceMode() {
        return distanceMode;
    }

    /***
     * @deprecated Setting the distance provider is now deprecated. The SDK will now always use Distance API as distance provider.
     * If you need to get the distance with traffic considerations then pass `distanceWithTraffic` as `true` to
     * `PositionManagerCore` class' `calculateDistance` method.
     * @return the distane provider service.
     */
    @Deprecated
    public static String getDistanceProvider() {
        return distanceProvider;
    }

    public static String getTrafficDistanceRouting() {
        return trafficDistanceRouting;
    }

    public static String getDistanceLanguage() {
        return distanceLanguage;
    }

    public static String getDistanceUnits() {
        return distanceUnits;
    }

    //Filter Accuracy of the location
    static public int accuracyFilter = 100;

    // delay for outdated notification
    static public int outOfTimeDelay = 300;

    // Distance detection threshold for visits
    static public double distanceDetectionThresholdVisits = 25.0;

    // Distance detection threshold for visits
    static public long minDurationVisitDisplay = 60 * 5;
    static public long durationVisitFilter = 1000 * minDurationVisitDisplay;

    //Delay of Duration data
    static public long numberOfDayDataDuration = 30;// number of day
    static public long dataDurationDelay = numberOfDayDataDuration * 1000 * 86400;

    //Active Classification
    static public boolean classificationEnable = false;

    // Distance detection threshold for a ZOI classified
    static public int radiusDetectionClassifiedZOI = 50;

    // Key for APIs
    static public String privateKeyGMPStatic = "";
    static public String privateKeyWoosmapAPI = "";

    //Checking Position is inside a region
    static public boolean checkIfPositionIsInsideGeofencingRegionsEnable = true;

    //Notification ForegroundService
    static public boolean foregroundLocationServiceEnable = false;
    static public String updateServiceNotificationTitle = "Location updated";
    static public String updateServiceNotificationText = "This app use your location";

    static public HashMap<String, String> searchAPIParameters = new HashMap<String, String>();
    static public ArrayList<String> userPropertiesFilter = new ArrayList<String>();


    public static final String WoosmapSdkTag = "WoosmapSdk";
    public static final String WoosmapBackgroundTag = "WoosmapBackground";
    public static final String WoosmapVisitsTag = "WoosmapVisit";
    public static final String NotificationError = "NotificationError";
    public static final String WoosmapBroadcastTag = "WoosmapBroadcast";
    public static final String WoosmapGeofenceTag = "WoosmapGeofence";


    public static String WoosmapURL = "https://api.woosmap.com";
    public static String SearchAPIUrl = "%s/stores/search/?private_key=%s&lat=%s&lng=%s&stores_by_page=20";
    public static String DistanceAPIUrl = "%s/distance/distancematrix/json?mode=%s&units=%s&language=%s&origins=%s,%s&destinations=%s&private_key=%s&method=%s&elements=duration_distance";
    public static String DistanceAPIWithTrafficUrl = "%s/distance/distancematrix/json?mode=%s&units=%s&language=%s&origins=%s,%s&destinations=%s&private_key=%s&method=%s&departure_time=now&elements=duration_distance";
    public static String TrafficDistanceAPIUrl = "%s/traffic/distancematrix/json?mode=%s&units=%s&language=%s&routing=%s&origins=%s,%s&destinations=%s&private_key=%s";
    public static String GoogleMapStaticUrl = "https://maps.google.com/maps/api/staticmap?markers=color:red%%7C%s,%s&markers=color:blue%%7C%s,%s&zoom=14&size=400x400&sensor=true&key=%s";
    public static String GoogleMapStaticUrl1POI = "https://maps.google.com/maps/api/staticmap?markers=color:red%%7C%s,%s&zoom=14&size=400x400&sensor=true&key=%s";

    public static String getNotificationDefaultUri(Context context) {
        String notificationUri = "";
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            notificationUri = bundle.getString("woosmap_notification_defautl_uri");
            Log.d(WoosmapSdkTag, "notification defautl uri : " + notificationUri);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WoosmapSdkTag, "Failed to load project key, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(WoosmapSdkTag, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        return notificationUri;
    }

    protected static void checkGeofenceEventTrigger(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            Log.d(WoosmapSettingsCore.WoosmapGeofenceTag, String.valueOf(geofenceTransition));
            Log.d(WoosmapSettingsCore.WoosmapGeofenceTag, triggeringGeofences.toString());

            WoosmapDb db = WoosmapDb.getInstance(context);
            WoosmapSettingsCore.loadSettings(context);
            PositionsManagerCore positionsManagerCore = new PositionsManagerCore(context, db, WoosmapCore.getInstance());
            for (int i = 0; i < triggeringGeofences.size(); i++) {
                positionsManagerCore.didEventRegion(triggeringGeofences.get(i).getRequestId(), geofenceTransition);
            }
        }
    }

    protected static String GeofencingSDKVersion = "";
    protected static String getGeofencingSDKVersion(){
        if (!GeofencingSDKVersion.isEmpty()){
            return GeofencingSDKVersion;
        }
        return BuildConfig.CORE_SDK_VERSION;
    }

}
