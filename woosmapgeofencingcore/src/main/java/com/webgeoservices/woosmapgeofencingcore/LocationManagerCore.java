package com.webgeoservices.woosmapgeofencingcore;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.webgeoservices.woosmapgeofencingcore.database.MovingPosition;
import com.webgeoservices.woosmapgeofencingcore.database.Region;
import com.webgeoservices.woosmapgeofencingcore.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


class LocationManagerCore extends LocationManagerProvider{

    /** Android registers at most this many geofences per app; every one beyond it is refused. */
    static final int MAX_NATIVE_GEOFENCES = 100;
    private static final int GEOFENCE_TRANSITIONS =
            Geofence.GEOFENCE_TRANSITION_ENTER
                    | Geofence.GEOFENCE_TRANSITION_DWELL
                    | Geofence.GEOFENCE_TRANSITION_EXIT;
    private static final double EARTH_RADIUS_M = 6_371_000d;
    /** How long one Play Services geofencing call may take before the restore gives up on it. */
    private static final long GEOFENCE_TASK_TIMEOUT_S = 30;

    /** Outcome of one add request to Android. */
    enum AddOutcome { ADDED, TOO_MANY, FAILED }

    /** One add request to Android; lets the registration order be unit-tested without a client. */
    interface GeofenceAdder {
        AddOutcome add(List<Region> regions);
    }

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

    /**
     * Rebuilds Android's geofences from the circular rows of the regions table, then makes the table
     * match what Android actually holds: rows left out by the 100-per-app cap, or refused by Android
     * for that reason, are deleted so the table never claims a geofence that is not watched
     * (issue #77). When Android refuses the whole set for another reason (location off, Play
     * Services not ready) nothing is known about individual rows, so they are kept for the next attempt.
     * <p>
     * Waits for the Play Services calls, so it must run off the main thread.
     *
     * @return {@code true} when the removal succeeded and every circular row is now registered.
     */
    @Override
    public boolean setMonitoringRegions() {
        Logger.getInstance().d("Geofence Add on Reboot");
        if (Looper.getMainLooper().isCurrentThread()) {
            Logger.getInstance().e("setMonitoringRegions must not run on the main thread");
            return false;
        }
        try {
            // Circular rows only: an isochrone's radius is a travel time in seconds, not a distance.
            Region[] circles = db.getRegionsDAO().getRegionCircle();
            MovingPosition lastPosition = db.getMovingPositionsDao().getLastMovingPosition();
            List<Region> planned = selectRegionsToRegister(circles, lastPosition, MAX_NATIVE_GEOFENCES);
            // Android gives no ordering guarantee between a bulk removal and the adds that follow.
            Exception removal = await(mGeofencingClient.removeGeofences(getGeofencePendingIntent()));
            if (removal != null) {
                Logger.getInstance().e("Removing geofences before restore failed, adding anyway", removal);
            }
            Optional<List<Region>> registered = registerInPriorityOrder(planned, this::addToGeofencingClient);
            if (!registered.isPresent()) {
                Logger.getInstance().e("Restore aborted: Android refused the whole set. Rows kept for the next attempt.");
                return false;
            }
            Logger.getInstance().d("onSuccess: " + registered.get().size() + " geofences added");
            List<Region> dropped = dropUnregisteredRegions(circles, registered.get());
            return removal == null && dropped.isEmpty();
        } catch (Exception ex) {
            Logger.getInstance().e("Restoring geofences failed", ex);
            return false;
        }
    }

    /**
     * Registers {@code regions} in one request. Android counts the host app's own geofences too, so
     * the room left under the cap cannot be known in advance: when the batch is refused as too many,
     * the regions are added one at a time, in priority order, until Android refuses.
     *
     * @return the regions Android now holds, or empty when Android refused for a reason other than the
     *         count, in which case nothing is known about individual regions.
     */
    static Optional<List<Region>> registerInPriorityOrder(List<Region> regions, GeofenceAdder adder) {
        if (regions.isEmpty()) {
            return Optional.of(regions);
        }
        AddOutcome batch = adder.add(regions);
        if (batch == AddOutcome.ADDED) {
            return Optional.of(regions);
        }
        if (batch == AddOutcome.FAILED) {
            return Optional.empty();
        }
        List<Region> registered = new ArrayList<>();
        for (Region region : regions) {
            AddOutcome one = adder.add(Collections.singletonList(region));
            if (one == AddOutcome.FAILED) {
                return Optional.empty();
            }
            if (one == AddOutcome.TOO_MANY) {
                break;
            }
            registered.add(region);
        }
        return Optional.of(registered);
    }

    /**
     * Keeps at most {@code limit} regions to hand to Android: every region the device is currently
     * inside first, then the nearest to {@code lastPosition}. Without a known position, the most
     * recently touched come first. The old restore took the first rows in ascending {@code dateTime}
     * order, which kept the least recently used and refused the ones just crossed.
     */
    static List<Region> selectRegionsToRegister(Region[] regions, MovingPosition lastPosition, int limit) {
        Comparator<Region> byDistanceOrRecency = lastPosition == null
                ? Comparator.comparingLong((Region region) -> -region.dateTime)
                : Comparator.comparingDouble((Region region) ->
                        distanceMeters(lastPosition.lat, lastPosition.lng, region.lat, region.lng));
        List<Region> sorted = new ArrayList<>(Arrays.asList(regions));
        sorted.sort(Comparator.comparing((Region region) -> !region.isCurrentPositionInside)
                .thenComparing(byDistanceOrRecency));
        return sorted.size() <= limit ? sorted : new ArrayList<>(sorted.subList(0, limit));
    }

    /** Great-circle distance in metres. Plain maths, so it also runs in unit tests without Android. */
    static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    /**
     * Deletes the rows Android does not hold, so the table only claims what is actually watched, and
     * names them in the log. Returns the dropped rows.
     */
    private List<Region> dropUnregisteredRegions(Region[] all, List<Region> registered) {
        Set<String> kept = new HashSet<>();
        for (Region region : registered) {
            kept.add(region.identifier);
        }
        List<Region> dropped = new ArrayList<>();
        for (Region region : all) {
            if (!kept.contains(region.identifier)) {
                db.getRegionsDAO().deleteRegionFromId(region.identifier);
                dropped.add(region);
            }
        }
        if (!dropped.isEmpty()) {
            Logger.getInstance().w(dropped.size() + " geofences not registered: Android allows " + MAX_NATIVE_GEOFENCES
                    + " per app. Removed from the regions table: " + identifiers(dropped));
        }
        return dropped;
    }

    @SuppressLint("MissingPermission")
    private AddOutcome addToGeofencingClient(List<Region> regions) {
        // LatLng takes latitude first; the old restore passed (lng, lat) and put every geofence on
        // the wrong side of the world.
        GeofencingRequest.Builder request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        for (Region regionToAdd : regions) {
            request.addGeofence(geofenceHelper.getGeofence(regionToAdd.identifier,
                    new LatLng(regionToAdd.lat, regionToAdd.lng), (float) regionToAdd.radius, GEOFENCE_TRANSITIONS));
        }
        Exception failure = await(mGeofencingClient.addGeofences(request.build(), getGeofencePendingIntent()));
        if (failure == null) {
            return AddOutcome.ADDED;
        }
        Logger.getInstance().e("onFailure: " + geofenceHelper.getErrorString(failure) + " for " + regions.size()
                + " geofences: " + identifiers(regions), failure);
        return isTooManyGeofences(failure) ? AddOutcome.TOO_MANY : AddOutcome.FAILED;
    }

    private static boolean isTooManyGeofences(Exception e) {
        return e instanceof ApiException
                && ((ApiException) e).getStatusCode() == GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES;
    }

    /** Waits for a Play Services call. Returns {@code null} on success, otherwise the failure. */
    private static Exception await(Task<Void> task) {
        try {
            Tasks.await(task, GEOFENCE_TASK_TIMEOUT_S, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return e;
        } catch (TimeoutException e) {
            return e;
        }
    }

    private static String identifiers(List<Region> regions) {
        StringBuilder ids = new StringBuilder();
        for (Region region : regions) {
            ids.append(ids.length() == 0 ? "" : ", ").append(region.identifier);
        }
        return ids.toString();
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
                Logger.getInstance().d("createLocationCallback: " + currentLocation);

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
