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
import java.util.Collection;
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
    private static final long GEOFENCE_TASK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
    /**
     * Budget for one whole restore, including the one-at-a-time fallback. Kept well under the
     * 10 minutes a JobIntentService gets on API 26+ so the reboot job is never killed mid-way.
     */
    private static final long RESTORE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

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
     * Rebuilds Android's geofences from the circular rows of the regions table (issue #77). Rows the
     * SDK itself leaves out because of the 100-per-app cap are deleted, so the table never claims a
     * geofence the SDK decided not to watch. Rows Android refuses as too many are kept: that count
     * includes the host app's own geofences, which may shrink before the next restore. When Android
     * refuses the whole set for another reason (location off, Play Services not ready) nothing is
     * known about individual rows, so they are kept as well.
     * <p>
     * Waits for the Play Services calls, so it must run off the main thread. The whole restore is
     * bounded by {@link #RESTORE_TIMEOUT_MS}.
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
            return restoreGeofences(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(RESTORE_TIMEOUT_MS));
        } catch (RuntimeException ex) {
            // Room and Play Services throw unchecked; the restore must never take the reboot job down.
            Logger.getInstance().e("Restoring geofences failed", ex);
            return false;
        }
    }

    /** The restore proper; see {@link #setMonitoringRegions()}. */
    private boolean restoreGeofences(long deadlineNanos) {
        // Circular rows only: an isochrone's radius is a travel time in seconds, not a distance.
        Region[] circles = db.getRegionsDAO().getRegionCircle();
        MovingPosition lastPosition = db.getMovingPositionsDao().getLastMovingPosition();
        List<Region> planned = selectRegionsToRegister(circles, lastPosition, MAX_NATIVE_GEOFENCES);
        // Android gives no ordering guarantee between a bulk removal and the adds that follow.
        Exception removal = await(mGeofencingClient.removeGeofences(getGeofencePendingIntent()), deadlineNanos);
        if (removal instanceof InterruptedException) {
            Logger.getInstance().e("Restore aborted: interrupted while removing geofences. Rows kept for the next attempt.");
            return false;
        }
        if (removal != null) {
            Logger.getInstance().e("Removing geofences before restore failed, adding anyway", removal);
        }
        Optional<List<Region>> registered =
                registerInPriorityOrder(planned, regions -> addToGeofencingClient(regions, deadlineNanos));
        if (!registered.isPresent()) {
            Logger.getInstance().e("Restore aborted: Android refused the whole set. Rows kept for the next attempt.");
            return false;
        }
        Logger.getInstance().d("onSuccess: " + registered.get().size() + " geofences added");
        dropRegionsOverCap(circles, planned);
        logRefusedRegions(planned, registered.get());
        return removal == null && registered.get().size() == circles.length;
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
     * <p>
     * Only the enterprise SDK writes {@code isCurrentPositionInside}; in core it is always
     * {@code false}, so the distance (or recency) rule alone decides.
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

    /** The regions of {@code all} whose identifier is not in {@code kept}, in the order of {@code all}. */
    static List<Region> missingFrom(Collection<Region> all, List<Region> kept) {
        Set<String> keptIds = new HashSet<>();
        for (Region region : kept) {
            keptIds.add(region.identifier);
        }
        List<Region> missing = new ArrayList<>();
        for (Region region : all) {
            if (!keptIds.contains(region.identifier)) {
                missing.add(region);
            }
        }
        return missing;
    }

    /**
     * Deletes the rows the SDK's own cap left out of {@code planned}, so the table only claims what
     * the SDK asked Android to watch, and names them in the log.
     */
    private void dropRegionsOverCap(Region[] circles, List<Region> planned) {
        List<Region> dropped = missingFrom(Arrays.asList(circles), planned);
        for (Region region : dropped) {
            db.getRegionsDAO().deleteRegionFromId(region.identifier);
        }
        if (!dropped.isEmpty()) {
            Logger.getInstance().w(dropped.size() + " geofences over the " + MAX_NATIVE_GEOFENCES
                    + " per app Android allows. Removed from the regions table: " + identifiers(dropped));
        }
    }

    /** Names the planned rows Android refused as too many; they stay in the table for the next restore. */
    private static void logRefusedRegions(List<Region> planned, List<Region> registered) {
        List<Region> refused = missingFrom(planned, registered);
        if (!refused.isEmpty()) {
            Logger.getInstance().w(refused.size() + " geofences refused by Android as too many (the host app's own"
                    + " geofences count too). Kept in the regions table for the next restore: " + identifiers(refused));
        }
    }

    @SuppressLint("MissingPermission")
    private AddOutcome addToGeofencingClient(List<Region> regions, long deadlineNanos) {
        // LatLng takes latitude first; the old restore passed (lng, lat) and put every geofence on
        // the wrong side of the world.
        List<Geofence> geofences = new ArrayList<>();
        for (Region regionToAdd : regions) {
            geofences.add(geofenceHelper.getGeofence(regionToAdd.identifier,
                    new LatLng(regionToAdd.lat, regionToAdd.lng), (float) regionToAdd.radius, GEOFENCE_TRANSITIONS));
        }
        Exception failure = await(mGeofencingClient.addGeofences(
                geofenceHelper.getGeofencingRequest(geofences), getGeofencePendingIntent()), deadlineNanos);
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

    /**
     * Waits for a Play Services call: at most {@link #GEOFENCE_TASK_TIMEOUT_MS}, and never past
     * {@code deadlineNanos} (a {@link System#nanoTime()} value). Returns {@code null} on success,
     * otherwise the failure; a {@link TimeoutException} once the deadline has passed.
     */
    private static Exception await(Task<Void> task, long deadlineNanos) {
        long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
        long timeoutMs = Math.max(1, Math.min(GEOFENCE_TASK_TIMEOUT_MS, remainingMs));
        try {
            Tasks.await(task, timeoutMs, TimeUnit.MILLISECONDS);
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
