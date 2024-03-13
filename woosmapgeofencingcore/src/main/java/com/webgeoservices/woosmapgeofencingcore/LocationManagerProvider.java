package com.webgeoservices.woosmapgeofencingcore;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;
import com.webgeoservices.woosmapgeofencingcore.logging.Logger;

public abstract class LocationManagerProvider {
    protected final Context context;
    protected final GeofencingClient mGeofencingClient;
    protected PendingIntent mGeofencePendingIntent;
    protected final GeofenceHelper geofenceHelper;
    protected LocationCallback mLocationCallback;
    protected PendingIntent mLocationIntent;
    protected LocationRequest mLocationRequest;
    protected final FusedLocationProviderClient mFusedLocationClient;
    protected final WoosmapProvider woos;
    protected WoosmapDb db;

    public LocationManagerProvider(Context context,WoosmapProvider woos) {
        this.context = context;
        this.woos=woos;
        db = WoosmapDb.getInstance(context);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        geofenceHelper = new GeofenceHelper(context);
    }

    public boolean checkPermissions() {
        int finePermissionState = ActivityCompat.checkSelfPermission(this.context,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermissionState = ActivityCompat.checkSelfPermission(this.context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return finePermissionState == PackageManager.PERMISSION_GRANTED || coarsePermissionState == PackageManager.PERMISSION_GRANTED;
    }

    public abstract void removeGeofences();

    public abstract void removeGeofences(String id);

    public abstract void replaceGeofence(String oldId, String newId, final LatLng latLng, final float radius, final String type);

    public abstract void addGeofence(final String id, final LatLng latLng, final float radius, final String idStore, String type);

    public void setmLocationRequest() {
        mLocationRequest = new LocationRequest();
    }

    public void updateLocationForeground() {
        if (mLocationRequest == null) {
            this.setmLocationRequest();
        }
        if (WoosmapSettingsCore.modeHighFrequencyLocation) {
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
        } else {
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
        }
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, null);//Looper.myLooper());
        } catch (SecurityException e) {
            Logger.getInstance().e("security exception: " + e, e);
        }
    }

    public void updateLocationBackground() {
        if (WoosmapSettingsCore.foregroundLocationServiceEnable) {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            return;
        }
        if (WoosmapSettingsCore.modeHighFrequencyLocation) {
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setMaxWaitTime(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else {
            mLocationRequest.setInterval(240000);
            mLocationRequest.setFastestInterval(60000);
            mLocationRequest.setMaxWaitTime(480000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationIntent);
        } catch (SecurityException e) {
            Logger.getInstance().e("security exception: " + e, e);
        }
    }

    public void removeLocationUpdates() {
        try {
            if (mFusedLocationClient != null && mLocationIntent != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationIntent);
            }
            if (mFusedLocationClient != null && mLocationCallback != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        } catch (SecurityException e) {
            Logger.getInstance().e("security exception: " + e, e);
        }
    }

    public void removeLocationCallback() {
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    public abstract void setMonitoringRegions();
}
