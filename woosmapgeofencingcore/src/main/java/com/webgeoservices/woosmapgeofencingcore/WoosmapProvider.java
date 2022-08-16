package com.webgeoservices.woosmapgeofencingcore;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.model.LatLng;
import com.webgeoservices.woosmapgeofencingcore.database.Distance;
import com.webgeoservices.woosmapgeofencingcore.database.POI;
import com.webgeoservices.woosmapgeofencingcore.database.Region;
import com.webgeoservices.woosmapgeofencingcore.database.RegionLog;
import com.webgeoservices.woosmapgeofencingcore.database.Visit;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;

public abstract class WoosmapProvider {

    protected Context context;
    protected LocationManagerCore locationManager;
    protected Boolean isForegroundEnabled = false;
    protected String asyncTrackNotifOpened = null;

    LocationReadyListener locationReadyListener = null;
    SearchAPIReadyListener searchAPIReadyListener = null;
    VisitReadyListener visitReadyListener = null;
    DistanceReadyListener distanceReadyListener = null;
    RegionReadyListener regionReadyListener = null;
    RegionLogReadyListener regionLogReadyListener = null;

    ProfileReadyListener profileReadyListener = null;

    private LocationUpdatesServiceCore mLocationUpdateService = null;

    protected void setupWoosmap(Context context) {
        this.context = context;
        this.locationManager = new LocationManagerCore(context, this);
    }

    public interface LocationReadyListener {
        /**
         * When Woosmap get a new location it calls this method
         *
         * @param location an user's location
         */
        void LocationReadyCallback(Location location);
    }

    /**
     * An interface to add callback on Search API retrieving
     */
    public interface SearchAPIReadyListener {
        /**
         * When Woosmap get a new POI it calls this method
         *
         * @param poi an user's location
         */
        void SearchAPIReadyCallback(POI poi);

    }


    /**
     * An interface to add callback on Distance API retrieving
     */
    public interface DistanceReadyListener {
        /**
         * When Woosmap get a new distance it calls this method
         *
         * @param distances array of distance reponse API
         */
        void DistanceReadyCallback(Distance[] distances);
    }

    /**
     * An interface to add callback on Visit retrieving
     */
    public interface VisitReadyListener {
        /**
         * When Woosmap get a new Visit it calls this method
         *
         * @param visit an user's location
         */
        void VisitReadyCallback(Visit visit);
    }

    /**
     * An interface to add callback on Region retrieving
     */
    public interface RegionReadyListener {
        /**
         * When Woosmap get a region when is create it calls this method
         *
         * @param region an user's location
         */
        void RegionReadyCallback(Region region);
    }

    /**
     * An interface to add callback on RegionLog retrieving
     */
    public interface RegionLogReadyListener {
        /**
         * When Woosmap get a region when event (enter,exit) it calls this method
         *
         * @param regionLog an user's location
         */
        void RegionLogReadyCallback(RegionLog regionLog);
    }

    /**
     * An interface to add callback on ProfileReadyListener to get Status
     */
    public interface ProfileReadyListener {
        /**
         * When Woosmap get a Status and error when Profile is loading
         *
         * @param status of the Loading profile
         * @param errors List of errors for the profile
         */
        void ProfileReadyCallback(Boolean status, ArrayList<String> errors);
    }

    public final class ConfigurationProfile {

        public static final String liveTracking = "liveTracking";
        public static final String passiveTracking = "passiveTracking";
        public static final String visitsTracking = "visitsTracking";

        private ConfigurationProfile() { }
    }


    /**
     * Add a listener to get callback on new locations
     *
     * @param locationReadyListener
     * @see LocationReadyListener
     */
    public void setLocationReadyListener(LocationReadyListener locationReadyListener) {
        this.locationReadyListener = locationReadyListener;
    }

    /**
     * Add a listener to get callback on new POI
     *
     * @param searchAPIReadyListener
     * @see SearchAPIReadyListener
     */
    public void setSearchAPIReadyListener(SearchAPIReadyListener searchAPIReadyListener) {
        this.searchAPIReadyListener = searchAPIReadyListener;
    }

    /**
     * Add a listener to get callback on new Distance
     *
     * @param distanceReadyListener
     * @see DistanceReadyListener
     */
    public void setDistanceReadyListener(DistanceReadyListener distanceReadyListener) {
        this.distanceReadyListener = distanceReadyListener;
    }

    /**
     * Add a listener to get callback on new Visit
     *
     * @param visitReadyListener
     * @see VisitReadyListener
     */
    public void setVisitReadyListener(VisitReadyListener visitReadyListener) {
        this.visitReadyListener = visitReadyListener;
    }

    /**
     * Add a listener to get callback on create region
     *
     * @param regionReadyListener
     * @see RegionReadyListener
     */
    public void setRegionReadyListener(RegionReadyListener regionReadyListener) {
        this.regionReadyListener = regionReadyListener;
    }

    /**
     * Add a listener to get callback on event region
     *
     * @param regionLogReadyListener
     * @see RegionLogReadyListener
     */
    public void setRegionLogReadyListener(RegionLogReadyListener regionLogReadyListener) {
        setRegionLogReadyListener(regionLogReadyListener,false);
    }

    /**
     * Add a listener to get callback on event region
     *
     * @param regionLogReadyListener
     * @see RegionLogReadyListener
     */
    public void setRegionLogReadyListener(RegionLogReadyListener regionLogReadyListener, Boolean sendCurrentState) {
        this.regionLogReadyListener = regionLogReadyListener;
        if(sendCurrentState) {
            getLastRegionState();
        }
    }

    /**
     * Add a listener to get callback on status profile
     *
     * @param profileReadyListener
     * @see ProfileReadyListener
     */
    public void setProfileReadyListener(ProfileReadyListener profileReadyListener) {
        this.profileReadyListener = profileReadyListener;

    }

    /**
     * Should be call on your mainActivity onResume method
     */
    public void onResume() {
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }
        this.isForegroundEnabled = true;
        if (this.shouldTrackUser()) {
            this.locationManager.updateLocationForeground();
        } else {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                WoosmapDb.getInstance(context).cleanOldGeographicData(context);
            }
        });

        if(mLocationUpdateService != null && WoosmapSettingsCore.foregroundLocationServiceEnable) {
            mLocationUpdateService.removeLocationUpdates();
        }

    }

    /**
     * Should be call on your mainActivity onPause method
     */
    public void onPause() {
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }

        WoosmapSettingsCore.saveSettings(context);

        if(WoosmapSettingsCore.foregroundLocationServiceEnable){
            if(mLocationUpdateService != null ) {
                mLocationUpdateService.enableLocationBackground( true );
            }else {
                // Bind to the service. If the service is in foreground mode, this signals to the service
                // that since this activity is in the foreground, the service can exit foreground mode.
                context.getApplicationContext().bindService(new Intent(context.getApplicationContext(), LocationUpdatesServiceCore.class), mServiceConnection,Context.BIND_AUTO_CREATE);
            }
        }


        try {
            if (this.shouldTrackUser()) {
                this.isForegroundEnabled = false;
                this.locationManager.updateLocationBackground();
            } else {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
            }
        } catch (NullPointerException e) {
            Log.d("WoosmapGeofencing", "Foreground inactive");
        }
    }


    void onReboot() {
        this.isForegroundEnabled = false;
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }
        if (this.shouldTrackUser()) {
            this.locationManager.setmLocationRequest();
            this.locationManager.updateLocationBackground();
            this.locationManager.setMonitoringRegions();
        } else {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
        }
    }

    public void onDestroy() {
        if (mLocationUpdateService != null && WoosmapSettingsCore.foregroundLocationServiceEnable) {
            mLocationUpdateService.removeLocationUpdates();
        }
        if (WoosmapSettingsCore.trackingEnable && mLocationUpdateService != null) {
            context.getApplicationContext().unbindService(mServiceConnection);
        }
        mLocationUpdateService = null;
    }


    private Boolean shouldTrackUser() {
        return this.locationManager.checkPermissions();
    }

    public Boolean enableTracking(boolean trackingEnable) {
        WoosmapSettingsCore.trackingEnable = trackingEnable;
        if(WoosmapSettingsCore.trackingEnable) {
            onResume();
            return true;
        } else {
            this.locationManager.removeLocationUpdates();
            return false;
        }
    }

    public void enableModeHighFrequencyLocation(boolean modeHighFrequencyLocationEnable) {
        WoosmapSettingsCore.modeHighFrequencyLocation = modeHighFrequencyLocationEnable;
        if(WoosmapSettingsCore.modeHighFrequencyLocation) {
            WoosmapSettingsCore.visitEnable = false;
            WoosmapSettingsCore.classificationEnable = false;
        }

        onResume();
    }


    protected void woosmapInitFunctionality(){
        /* Send notifications is opened async if the app was killed */
        if (asyncTrackNotifOpened != null) {
            asyncTrackNotifOpened = null;
        }
        if (isForegroundEnabled) {
            onResume();
        }
    }
    @RequiresApi(26)
    public void createWoosmapNotifChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = WoosmapSettingsCore.WoosmapNotificationChannelID;
        CharSequence name = "WoosmapGeofencing";
        String description = "WoosmapGeofencing Notifs";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        Objects.requireNonNull(mNotificationManager).createNotificationChannel(mChannel);
    }

    public void addGeofence(String id, LatLng latLng, float radius, String type) {
        addGeofence( id,latLng,radius, "", type);
    }

    public void addGeofence(String id, LatLng latLng, float radius) {
        addGeofence( id,latLng,radius, "", "circle" );
    }

    public void addGeofence(String id, LatLng latLng, float radius, String idStore, String type) {
        locationManager.addGeofence( id,latLng,radius, idStore, type );
    }

    public void removeGeofence(String id) {
        locationManager.removeGeofences(id);
    }
    public void removeGeofence() { locationManager.removeGeofences();}

    public void replaceGeofence(String oldId, String newId, LatLng latLng, float radius){
        locationManager.replaceGeofence(oldId, newId, latLng, radius, "circle");
    }

    public void replaceGeofence(String oldId, String newId, LatLng latLng, float radius, String type){
        locationManager.replaceGeofence(oldId, newId, latLng, radius, type);
    }

    // Monitors the state of the connection to the service.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("LocationUpdatesService", "onServiceConnected");
            LocationUpdatesServiceCore.LocalBinder binder = (LocationUpdatesServiceCore.LocalBinder) service;
            mLocationUpdateService = binder.getService();
            mLocationUpdateService.enableLocationBackground( true );
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("LocationUpdatesService", "onServiceDisconnected");
            mLocationUpdateService = null;
        }
    };

    public void getLastRegionState() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                RegionLog rLog = WoosmapDb.getInstance(context).getRegionLogsDAO().getLastRegionLog();
                if (regionLogReadyListener != null && rLog != null) {
                    regionLogReadyListener.RegionLogReadyCallback(rLog);
                }
            }
        });
    }

}
