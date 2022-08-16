package com.webgeoservices.woosmapgeofencingcore;


import android.content.Context;
import android.util.Log;

public class WoosmapCore extends WoosmapProvider{
    private String fcmToken = "";
    private static volatile WoosmapCore instance;


    private WoosmapCore() {
        // Prevent form the reflection api
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    /**
     * Initialize Woosmap singleton (use automatically FCM to notify)
     *
     * @param context Your application context
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    public WoosmapCore initializeWoosmap(final Context context) {
        WoosmapCore instance = initializeWoosmap(context, null);
        return instance;
    }

    /**
     * Initialize Woosmap singleton (notify manual could use GCM or FCM). Use this method only to initialized
     *
     * @param context       Your application context
     * @param messageToken, token give by notification service (GCM or FCM)
     * @return the Woosmap instance which has been initialized
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/client#sample-register">Firebase documentation</a>
     */
    private WoosmapCore initializeWoosmap(Context context, String messageToken) {
        this.setupWoosmap(context);
        if (messageToken != null) {
            this.fcmToken = messageToken;
        }
        this.initWoosmap();

        return instance;
    }

    /**
     * Initialize Woosmap singleton in Background. Use this method only to initialized
     *
     * @param context Your application context
     */
    WoosmapCore initializeWoosmapInBackground(Context context) {
        this.setupWoosmap(context);
        return instance;
    }
    public static WoosmapCore getInstance() {
        if  (instance == null) {
            synchronized (WoosmapCore.class) {
                if (instance == null) {
                    instance = new WoosmapCore();
                }
            }
        }
        return instance;
    }
    public static void setMessageToken(String messageToken) {
        getInstance().fcmToken = messageToken;
    }
    private void initWoosmap() {
        if (fcmToken == null) {
            Log.i(WoosmapSettingsCore.WoosmapSdkTag, "Message Token is null");
        }
        super.woosmapInitFunctionality();
    }
}
