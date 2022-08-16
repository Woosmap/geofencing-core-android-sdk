package com.webgeoservices.woosmapgeofencingcore;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.List;

/**
 * Handles incoming location updates and displays a notification with the location data.
 * <p>
 * For apps targeting API level 25 ("Nougat") or lower, location updates may be requested
 * using {@link android.app.PendingIntent#getService(Context, int, Intent, int)} or
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)}. For apps targeting
 * API level O, only {@code getBroadcast} should be used.
 * <p>
 * Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 * less frequently than the interval specified in the
 * {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 * foreground.
 */
public class LocationUpdatesIntentServiceCore extends IntentService {

    static final String ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.locationupdatespendingintent.action" +
                    ".PROCESS_UPDATES";
    private static final String TAG = LocationUpdatesIntentServiceCore.class.getSimpleName();

    public LocationUpdatesIntentServiceCore() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    WoosmapSettingsCore.loadSettings(context);
                    WoosmapCore woos = WoosmapCore.getInstance().initializeWoosmapInBackground(context);
                    if (WoosmapSettingsCore.visitEnable) {
                        WoosmapDb db = WoosmapDb.getInstance(context);
                        PositionsManagerCore positionsManager = new PositionsManagerCore(context, db,WoosmapCore.getInstance());
                        positionsManager.asyncManageLocation(locations);
                    }
                }
            }
        }
    }
}