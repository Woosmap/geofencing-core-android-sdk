package com.webgeoservices.woosmapgeofencingcore;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.webgeoservices.woosmapgeofencingcore.database.RegionLog;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.concurrent.Executors;

public class WoosmapCore extends WoosmapProvider{
    private String fcmToken = "";
    private static volatile WoosmapCore instance;
    private LocationManagerCore locationManagerCore;
    private LocationUpdatesServiceCore mLocationUpdateService = null;


    private WoosmapCore() {
        // Prevent form the reflection api
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    /**
     * Initialize Woosmap singleton (use automatically FCM to notify)
     *
     * @param context Your application context
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    public WoosmapCore initializeWoosmap(final Context context) {
        WoosmapCore instance = initializeWoosmap(context, null);
        return instance;
    }

    /**
     * Initialize Woosmap singleton (notify manual could use GCM or FCM). Use this method only to initialized
     *
     * @param context       Your application context
     * @param messageToken, token give by notification service (GCM or FCM)
     * @return the Woosmap instance which has been initialized
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    private WoosmapCore initializeWoosmap(Context context, String messageToken) {
        this.setupWoosmap(context);
        locationManagerCore=new LocationManagerCore(context,instance);
        if (messageToken != null) {
            this.fcmToken = messageToken;
        }
        this.initWoosmap();

        return instance;
    }

    /**
     * Initialize Woosmap singleton in Background. Use this method only to initialized
     *
     * @param context Your application context
     */
    WoosmapCore initializeWoosmapInBackground(Context context) {
        this.setupWoosmap(context);
        locationManagerCore=new LocationManagerCore(context,instance);
        return instance;
    }
    public static WoosmapCore getInstance() {
        if  (instance == null) {
            synchronized (WoosmapCore.class) {
                if (instance == null) {
                    instance = new WoosmapCore();
                }
            }
        }
        return instance;
    }
    public static void setMessageToken(String messageToken) {
        getInstance().fcmToken = messageToken;
    }
    private void initWoosmap() {
        if (fcmToken == null) {
            Log.i(WoosmapSettingsCore.WoosmapSdkTag, "Message Token is null");
        }
        super.woosmapInitFunctionality();
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

    @Override
    protected Boolean shouldTrackUser() {
        return locationManagerCore.checkPermissions();
    }

    @Override
    public void onResume() {
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }
        if (this.shouldTrackUser()) {
            this.locationManagerCore.updateLocationForeground();
        } else {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
        }
        this.isForegroundEnabled = true;
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

    @Override
    public void onPause() {
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }
        WoosmapSettingsCore.saveSettings(context);
        try {
            if (this.shouldTrackUser()) {
                this.isForegroundEnabled = false;
                this.locationManagerCore.updateLocationBackground();
            } else {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
            }
        } catch (NullPointerException e) {
            Log.d("WoosmapGeofencing", "Foreground inactive");
        }
        if(WoosmapSettingsCore.foregroundLocationServiceEnable){
            if(mLocationUpdateService != null ) {
                mLocationUpdateService.enableLocationBackground( true );
            }else {
                // Bind to the service. If the service is in foreground mode, this signals to the service
                // that since this activity is in the foreground, the service can exit foreground mode.
                context.getApplicationContext().bindService(new Intent(context.getApplicationContext(), LocationUpdatesServiceCore.class), mServiceConnection,Context.BIND_AUTO_CREATE);
            }
        }
    }

    @Override
    public void onReboot() {
        this.isForegroundEnabled = false;
        if(!WoosmapSettingsCore.trackingEnable) {
            return;
        }
        if (this.shouldTrackUser()) {
            this.locationManagerCore.setmLocationRequest();
            this.locationManagerCore.updateLocationBackground();
            this.locationManagerCore.setMonitoringRegions();
        } else {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Get Location permissions error");
        }
    }

    @Override
    public void onDestroy() {
        if (mLocationUpdateService != null && WoosmapSettingsCore.foregroundLocationServiceEnable) {
            mLocationUpdateService.removeLocationUpdates();
        }
        if (WoosmapSettingsCore.trackingEnable && mLocationUpdateService != null) {
            context.getApplicationContext().unbindService(mServiceConnection);
        }
        mLocationUpdateService = null;
    }

    @Override
    public Boolean enableTracking(boolean trackingEnable) {
        WoosmapSettingsCore.trackingEnable = trackingEnable;
        if(WoosmapSettingsCore.trackingEnable) {
            onResume();
            return true;
        } else {
            this.locationManagerCore.removeLocationUpdates();
            return false;
        }
    }

    @Override
    public void enableModeHighFrequencyLocation(boolean modeHighFrequencyLocationEnable) {
        WoosmapSettingsCore.modeHighFrequencyLocation = modeHighFrequencyLocationEnable;
        if(WoosmapSettingsCore.modeHighFrequencyLocation) {
            WoosmapSettingsCore.visitEnable = false;
            WoosmapSettingsCore.classificationEnable = false;
        }
        onResume();
    }

    @Override
    protected void woosmapInitFunctionality() {
        super.woosmapInitFunctionality();
        if (isForegroundEnabled) {
            onResume();
        }
    }

    @Override
    public void addGeofence(String id, LatLng latLng, float radius, String type) {
        addGeofence( id,latLng,radius, "", type);
    }

    @Override
    public void addGeofence(String id, LatLng latLng, float radius) {
        addGeofence( id,latLng,radius, "", "circle" );
    }

    @Override
    public void addGeofence(String id, LatLng latLng, float radius, String idStore, String type) {
        locationManagerCore.addGeofence( id,latLng,radius, idStore, type );
    }

    @Override
    public void removeGeofence(String id) {
        locationManagerCore.removeGeofences(id);
    }

    @Override
    public void removeGeofence() {
        locationManagerCore.removeGeofences();
    }

    @Override
    public void replaceGeofence(String oldId, String newId, LatLng latLng, float radius) {
        locationManagerCore.replaceGeofence(oldId, newId, latLng, radius, "circle");
    }

    @Override
    public void replaceGeofence(String oldId, String newId, LatLng latLng, float radius, String type) {
        locationManagerCore.replaceGeofence(oldId, newId, latLng, radius, type);
    }

    @Override
    public void getLastRegionState() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                RegionLog rLog = WoosmapDb.getInstance(context).getRegionLogsDAO().getLastRegionLog();
                if (WoosmapCore.getInstance().regionLogReadyListener != null && rLog != null) {
                    WoosmapCore.getInstance().regionLogReadyListener.RegionLogReadyCallback(rLog);
                }
            }
        });
    }
}
