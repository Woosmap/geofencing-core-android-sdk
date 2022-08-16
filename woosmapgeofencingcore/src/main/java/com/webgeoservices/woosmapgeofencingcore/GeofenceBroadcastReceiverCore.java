package com.webgeoservices.woosmapgeofencingcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class GeofenceBroadcastReceiverCore extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (WoosmapSettingsCore.modeHighFrequencyLocation) {
            return;
        }
        WoosmapSettingsCore.checkGeofenceEventTrigger(context,intent);
    }
}