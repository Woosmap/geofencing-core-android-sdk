package com.webgeoservices.woosmapgeofencingcore;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


public class WoosmapRebootJobServiceCore extends JobIntentService {
    private static final int JOB_ID = 0x01;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WoosmapRebootJobServiceCore.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "start activity after reboot");
        WoosmapCore woosmapCore = WoosmapCore.getInstance().initializeWoosmapInBackground(getBaseContext());
        woosmapCore.onReboot();

    }
}
