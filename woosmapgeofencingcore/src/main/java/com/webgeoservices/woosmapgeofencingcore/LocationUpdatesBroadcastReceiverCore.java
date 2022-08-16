package com.webgeoservices.woosmapgeofencingcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;

import java.util.List;

public class LocationUpdatesBroadcastReceiverCore extends BroadcastReceiver {

    static final String ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.locationupdatespendingintent.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    WoosmapCore woos = WoosmapCore.getInstance().initializeWoosmapInBackground(context);
                    WoosmapDb db = WoosmapDb.getInstance(context);
                    WoosmapSettingsCore.loadSettings(context);

                    PositionsManagerCore positionsManager = new PositionsManagerCore(context, db,WoosmapCore.getInstance());
                    positionsManager.asyncManageLocation(locations);
                }
            }
        }
    }
}
