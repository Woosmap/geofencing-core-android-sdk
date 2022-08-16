package com.webgeoservices.woosmapgeofencingcore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public abstract class BaseLocationUpdateService extends Service {

    private static final String TAG=BaseLocationUpdateService.class.getSimpleName();


    private int message_icon;
    protected NotificationManager mNotificationManager;
    protected LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    protected FusedLocationProviderClient mFusedLocationClient;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    protected final long UPDATE_INTERVAL_IN_MILLISECONDS = 4000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    protected  final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    protected  final int NOTIFICATION_ID = 20200520;



    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */

    protected boolean mChangingConfiguration = false;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }


    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && WoosmapSettingsCore.foregroundLocationServiceEnable) {
            Log.i(TAG, "Starting foreground service");
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }



    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createLocationRequest();
        if (!shouldTrackUser()) {
            stopForeground(true);
            stopSelf();
        }

    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    protected boolean shouldTrackUser() {
        int finePermissionState = ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return finePermissionState == PackageManager.PERMISSION_GRANTED;
    }
    protected Notification getNotification(Class<?> cls, Location mLocation, String EXTRA_STARTED_FROM_NOTIFICATION) {
        Intent intent = new Intent(this, cls);

        //create channel
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        PackageManager pm = this.getPackageManager();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            String CHANNEL_ID = WoosmapSettingsCore.WoosmapNotificationChannelID;
            int active = NotificationManager.IMPORTANCE_NONE;
            if(WoosmapSettingsCore.WoosmapNotificationActive) {
                active = NotificationManager.IMPORTANCE_HIGH;
            }
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, WoosmapSettingsCore.WoosmapNotificationChannelName, active);
            channel.setDescription(WoosmapSettingsCore.WoosmapNotificationDescriptionChannel);
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        CharSequence text = getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        }

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, flags);

        Intent newIntent = pm.getLaunchIntentForPackage(this.getPackageName());
        PendingIntent mPendingIntent = PendingIntent.getActivity(this,0, newIntent,  flags);
        setIconFromManifestVariable();
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return builder
                .setContentText(text)
                .setContentTitle(WoosmapSettingsCore.updateServiceNotificationTitle)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon( message_icon )
                .setSound( defaultSoundUri )
                .setTicker(text)
                .setContentIntent(mPendingIntent)
                .setWhen(System.currentTimeMillis()).build();

    }
    private void setIconFromManifestVariable() {
        ApplicationInfo mApplicationInfo;
        try {
            mApplicationInfo = getApplication().getPackageManager().getApplicationInfo(getApplication().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = mApplicationInfo.metaData;
            if(bundle.containsKey("woosmap.messaging.default_notification_icon")){
                this.message_icon = bundle.getInt("woosmap.messaging.default_notification_icon", R.drawable.ic_local_grocery_store_black_24dp);
            }else {
                this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
        }
    }
    private String getLocationText(Location location) {
        if(WoosmapSettingsCore.updateServiceNotificationText.isEmpty()) {
            return location == null ? "Unknown location" :
                    "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
        } else {
            return WoosmapSettingsCore.updateServiceNotificationText;
        }
    }
    /**
     * Sets the location request parameters.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    protected void removeLocationUpdates(LocationCallback mLocationCallback) {
        Log.i(TAG, "Removing location updates");
        try {
            if(mLocationCallback!=null){
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
            stopForeground(true);
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    abstract void removeLocationUpdates();
    abstract void onNewLocation(Location location);
    abstract void enableLocationBackground(boolean enable);

}
