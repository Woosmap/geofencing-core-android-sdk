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
    protected Boolean isForegroundEnabled = false;
    protected String asyncTrackNotifOpened = null;

    public LocationReadyListener locationReadyListener = null;
    public SearchAPIReadyListener searchAPIReadyListener = null;
    public VisitReadyListener visitReadyListener = null;
    public DistanceReadyListener distanceReadyListener = null;
    public RegionReadyListener regionReadyListener = null;
    public RegionLogReadyListener regionLogReadyListener = null;

    public ProfileReadyListener profileReadyListener = null;



    protected void setupWoosmap(Context context) {
        this.context = context;
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
    public abstract void onResume();

    /**
     * Should be call on your mainActivity onPause method
     */
    public abstract void onPause();


    public abstract void onReboot();
    public abstract void onDestroy();

    protected abstract Boolean shouldTrackUser();

    public abstract Boolean enableTracking(boolean trackingEnable);

    public abstract void enableModeHighFrequencyLocation(boolean modeHighFrequencyLocationEnable);


    protected void woosmapInitFunctionality(){
        /* Send notifications is opened async if the app was killed */
        if (asyncTrackNotifOpened != null) {
            asyncTrackNotifOpened = null;
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

    public abstract void addGeofence(String id, LatLng latLng, float radius, String type);

    public abstract void addGeofence(String id, LatLng latLng, float radius);

    public abstract void addGeofence(String id, LatLng latLng, float radius, String idStore, String type);

    public abstract void removeGeofence(String id);
    public abstract void removeGeofence();

    public abstract void replaceGeofence(String oldId, String newId, LatLng latLng, float radius);

    public abstract void replaceGeofence(String oldId, String newId, LatLng latLng, float radius, String type);



    public abstract void getLastRegionState();

}
