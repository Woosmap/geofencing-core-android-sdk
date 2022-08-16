package com.webgeoservices.woosmapgeofencingcore;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.webgeoservices.woosmapgeofencingcore.database.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class LocationManagerCore extends LocationManagerProvider{

    private PositionsManagerCore positionsManagerCore;

    public LocationManagerCore(Context context, WoosmapProvider woos) {
        super(context, woos);
        positionsManagerCore = new PositionsManagerCore(context, db, woos);
        createLocationCallback();
        createLocationPendingIntent();
    }


    @Override
    public void removeGeofences() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        db.getRegionsDAO().deleteAllRegions();
    }

    @Override
    public void removeGeofences(String id) {
        mGeofencingClient.removeGeofences(Collections.singletonList(id));
        positionsManagerCore.removeGeofence(id);
    }

    @Override
    public void replaceGeofence(String oldId, String newId, LatLng latLng, float radius, String type) {
        mGeofencingClient.removeGeofences(Collections.singletonList(oldId));
        Geofence geofence = geofenceHelper.getGeofence(newId, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        positionsManagerCore.replaceGeofenceCircle(oldId, geofenceHelper, geofencingRequest, getGeofencePendingIntent(), mGeofencingClient, newId, radius, latLng.latitude, latLng.longitude);
    }

    @Override
    public void addGeofence(String id, LatLng latLng, float radius, String idStore, String type) {
        Geofence geofence = geofenceHelper.getGeofence(id, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        positionsManagerCore.addGeofence(geofenceHelper, geofencingRequest, getGeofencePendingIntent(), mGeofencingClient, id, radius, latLng.latitude, latLng.longitude, idStore);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void setMonitoringRegions() {
        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Geofence Add on Reboot");
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        Region[] regions = db.getRegionsDAO().getAllRegions();
        for (Region regionToAdd : regions) {
            Geofence geofence = geofenceHelper.getGeofence(regionToAdd.identifier, new LatLng(regionToAdd.lng, regionToAdd.lat), (float) regionToAdd.radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
            GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
            mGeofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onSuccess: Geofence Added...");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String errorMessage = geofenceHelper.getErrorString(e);
                            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onFailure " + errorMessage);
                        }
                    });
        }
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location currentLocation = locationResult.getLastLocation();
                Log.d("WoosmapSdk", currentLocation.toString());

                List<Location> listLocations = new ArrayList<Location>();
                listLocations.add(currentLocation);
                if (woos.locationReadyListener != null) {
                    woos.locationReadyListener.LocationReadyCallback(currentLocation);
                }
                positionsManagerCore.asyncManageLocation(listLocations);
            }
        };
    }

    private void createLocationPendingIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(this.context, LocationUpdatesBroadcastReceiverCore.class);
            intent.setAction(LocationUpdatesBroadcastReceiverCore.ACTION_PROCESS_UPDATES);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
            mLocationIntent = PendingIntent.getBroadcast(this.context, 0, intent, flags);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(this.context, LocationUpdatesBroadcastReceiverCore.class);
            intent.setAction(LocationUpdatesBroadcastReceiverCore.ACTION_PROCESS_UPDATES);
            mLocationIntent = PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(this.context, LocationUpdatesIntentServiceCore.class);
            intent.setAction(LocationUpdatesIntentServiceCore.ACTION_PROCESS_UPDATES);
            mLocationIntent = PendingIntent.getService(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(context, GeofenceBroadcastReceiverCore.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(context, GeofenceBroadcastReceiverCore.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(context, GeofenceTransitionsIntentServiceCore.class);
            mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofencePendingIntent;
    }
}
