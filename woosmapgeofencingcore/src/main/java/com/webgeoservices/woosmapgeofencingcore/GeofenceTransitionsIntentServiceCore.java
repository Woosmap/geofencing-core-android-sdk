package com.webgeoservices.woosmapgeofencingcore;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;



public class GeofenceTransitionsIntentServiceCore extends IntentService {

    private static final String TAG = "GeofenceTransitionsIS";

    public GeofenceTransitionsIntentServiceCore() {
        super(TAG);
    }

    //On Android 8.0 (API level 26) and lower
    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        WoosmapSettingsCore.checkGeofenceEventTrigger(context,intent);
    }
}