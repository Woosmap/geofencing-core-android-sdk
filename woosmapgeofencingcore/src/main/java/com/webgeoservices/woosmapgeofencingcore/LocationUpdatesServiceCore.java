package com.webgeoservices.woosmapgeofencingcore;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.Collections;

public class LocationUpdatesServiceCore extends BaseLocationUpdateService {
    private static final String PACKAGE_NAME =
            "com.webgeoservices.woosmapgeofencingcore";

    private static final String TAG = LocationUpdatesServiceCore.class.getSimpleName();

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();


    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;


    /**
     * The current location.
     */
    private Location mLocation;

    public LocationUpdatesServiceCore(){

    }
    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesServiceCore getService() {
            return LocationUpdatesServiceCore.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };
        if (!shouldTrackUser()) {
            removeLocationUpdates();
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            boolean startedFromNotification = intent.getBooleanExtra( EXTRA_STARTED_FROM_NOTIFICATION,
                    false );
            Log.i( TAG, "onStartCommand" );
            // We got here because the user decided to remove location updates from the notification.
            if (startedFromNotification) {
                removeLocationUpdates();
                stopSelf();
            }
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeLocationUpdates();
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void enableLocationBackground(boolean enable) {
        Log.i(TAG, "enableLocationBackground");
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            if(WoosmapSettingsCore.foregroundLocationServiceEnable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService( new Intent( getApplicationContext(), LocationUpdatesServiceCore.class ) );
                } else {
                    startService( new Intent( getApplicationContext(), LocationUpdatesServiceCore.class ) );
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground( NOTIFICATION_ID, getNotification(LocationUpdatesServiceCore.class,mLocation,EXTRA_STARTED_FROM_NOTIFICATION,WoosmapCore.getInstance().getIconFromManifestVariable()), FOREGROUND_SERVICE_TYPE_LOCATION );
            } else {
                startForeground( NOTIFICATION_ID,getNotification(LocationUpdatesServiceCore.class,mLocation,EXTRA_STARTED_FROM_NOTIFICATION,WoosmapCore.getInstance().getIconFromManifestVariable()) );
            }
            mNotificationManager.notify(NOTIFICATION_ID, getNotification(LocationUpdatesServiceCore.class,mLocation,EXTRA_STARTED_FROM_NOTIFICATION,WoosmapCore.getInstance().getIconFromManifestVariable()));
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    @Override
    public void removeLocationUpdates() {
        removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onNewLocation(Location location) {
        updateLocation(location);
    }

    private void updateLocation(Location location) {
        Log.i(TAG, "New location: " + location);
        mLocation = location;
        WoosmapDb db = WoosmapDb.getInstance(getApplicationContext());
        PositionsManagerCore positionsManagerCore = new PositionsManagerCore(getApplicationContext(), db,WoosmapCore.getInstance());
        positionsManagerCore.asyncManageLocation( Collections.singletonList( mLocation ) );

    }
}