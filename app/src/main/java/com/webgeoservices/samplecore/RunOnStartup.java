package com.webgeoservices.samplecore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.webgeoservices.woosmapgeofencingcore.WoosmapRebootJobServiceCore;


public class RunOnStartup extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            WoosmapRebootJobServiceCore.enqueueWork(context, new Intent());
        }
    }
}
