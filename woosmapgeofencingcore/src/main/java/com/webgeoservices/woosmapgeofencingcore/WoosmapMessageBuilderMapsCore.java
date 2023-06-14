package com.webgeoservices.woosmapgeofencingcore;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel.Feature;
import com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel.SearchAPI;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;

public class WoosmapMessageBuilderMapsCore {

    protected final Context context;
    protected Class<?> cls = null;
    protected ApplicationInfo mApplicationInfo;
    protected int message_icon;
    protected NotificationCompat.Builder mBuilder;
    protected PendingIntent mPendingIntent;
    protected NotificationManager mNotificationManager;
    protected final NotificationCompat.Style[] mStyle = new NotificationCompat.Style[1];


    public WoosmapMessageBuilderMapsCore(Context context) {
        this.context = context;
        this.setIconFromManifestVariable();
    }

    public WoosmapMessageBuilderMapsCore(Context context, Class<?> cls) {
        this.context = context;
        this.cls = cls;
        this.setIconFromManifestVariable();


    }

    private void setIconFromManifestVariable() {
        try {
            mApplicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = mApplicationInfo.metaData;
            this.message_icon = bundle.getInt("woosmap.messaging.default_notification_icon", R.drawable.ic_local_grocery_store_black_24dp);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.message_icon = R.drawable.ic_local_grocery_store_black_24dp;
        }
    }

    /**
     * Set the notification's small icon
     *
     * @param icon
     */
    public void setSmallIcon(int icon) {
        this.message_icon = icon;
    }

    /**
     * Create and show a notification
     *
     * @param datas FCM message body received.
     */
    public void sendWoosmapNotification(final WoosmapMessageDatasCore datas) {
        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Search API");

        /**
         * Compare Timestamp between Server and Mobile to know if the notification is outdated
         */
        if (datas.timestamp != null) {
            Long tsMobile = System.currentTimeMillis() / 1000;

            try {
                Long tsServer = Long.parseLong(datas.timestamp);
                if (tsServer + WoosmapSettingsCore.outOfTimeDelay < tsMobile) {
                    Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Timestamp is outdated");
                    return;
                }
            } catch (NumberFormatException ex) { // handle your exception
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "invalid timestamp ");
                return;
            }

        } else {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "No timestamp is define in the payload");
            return;
        }

        mNotificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = WoosmapSettingsCore.WoosmapNotificationChannelID;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        mBuilder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                //.setSmallIcon(this.message_icon)
                .setSmallIcon(R.drawable.ic_local_grocery_store_black_24dp)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        if (datas.icon_url != null) {
            mBuilder.setLargeIcon(getBitmapFromURL(datas.icon_url));
        }

        /**
         * Get Position on received FCM notification
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Intent resultIntent;
        if (this.cls != null) {
            resultIntent = new Intent(this.context, this.cls);
        } else {
            resultIntent = new Intent(Intent.ACTION_VIEW);
            if (datas.open_uri != null) {
                resultIntent.setData(Uri.parse(datas.open_uri));
            } else {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Try to open empty URI");
                resultIntent.setData(Uri.parse(WoosmapSettingsCore.getNotificationDefaultUri(this.context)));
            }
        }
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.putExtra(WoosmapSettingsCore.WoosmapNotification, datas.notificationId);
        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "notif: " + datas.notificationId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        }
        mPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, flags);


        if (!WoosmapSettingsCore.privateKeyGMPStatic.isEmpty() && !WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            sendNotificationWithSearchAPIAndStaticMap();
        } else if (WoosmapSettingsCore.privateKeyGMPStatic.isEmpty() && !WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            sendNotificationWithSearchAPI();
        } else if (!WoosmapSettingsCore.privateKeyGMPStatic.isEmpty() && WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            sendNotificationWithGMPStatic();
        } else {
            sendNotificationWithLocation();
        }
    }

    /**
     * Create and show a notification with the result of Search API and a show a google Map Static with the user location
     * and the nearest POI
     */
    private void sendNotificationWithSearchAPIAndStaticMap() {
        getLatestLocation(this.context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                //Request Search API with Google map Static
                searchAPIRequest(location, true);
            }
        });
    }

    /**
     * Create and show a notification with the result of Search API
     */
    private void sendNotificationWithSearchAPI() {
        getLatestLocation(this.context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                //Request Search API
                searchAPIRequest(location, false);
            }
        });
    }

    /**
     * Create and show a notification with a google map Static
     */
    private void sendNotificationWithGMPStatic() {
        getLatestLocation(this.context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                // Fill body message with informations from API
                final String messageBody = "User Location Longitude = " + location.getLongitude() + "\n" + "User Location Latitude = " + location.getLatitude();
                mBuilder.setContentText(messageBody);
                mBuilder.setContentTitle("Location Notification");
                // call Google API static map
                googleMapStaticAPIRequest(location, null, null, messageBody);
            }
        });
    }

    /**
     * Create and show a notification with the result of the user location
     */
    private void sendNotificationWithLocation() {
        getLatestLocation(this.context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location == null) {
                    Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Can't get user Location");
                    return;
                }
                // Fill body message with informations from API
                final String messageBody = "User Location Longitude = " + location.getLongitude() + "\n" + "User Location Latitude = " + location.getLatitude();
                mBuilder.setContentTitle("Location Notification");
                mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(messageBody));
                mBuilder.setContentIntent(mPendingIntent);

                Notification notification = mBuilder.build();
                mNotificationManager.notify(new Random().nextInt(20), notification);
            }
        });
    }

    /**
     * Request a Google Map Static
     */
    private void googleMapStaticAPIRequest(Location location, Double longitudePOI, Double latitudePOI, final String messageBody) {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        // Request Google Maps Static
        String urlGMPStatic = "";
        if (longitudePOI == null) {
            urlGMPStatic = String.format(WoosmapSettingsCore.GoogleMapStaticUrl1POI, location.getLatitude(), location.getLongitude(), WoosmapSettingsCore.privateKeyGMPStatic);
        } else {
            urlGMPStatic = String.format(WoosmapSettingsCore.GoogleMapStaticUrl, location.getLatitude(), location.getLongitude(), latitudePOI, longitudePOI, WoosmapSettingsCore.privateKeyGMPStatic);
        }
        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(urlGMPStatic,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        mStyle[0] = new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(messageBody);
                        mBuilder.setStyle(mStyle[0]);
                        mBuilder.setContentIntent(mPendingIntent);

                        Notification notification = mBuilder.build();
                        mNotificationManager.notify(new Random().nextInt(20), notification);

                    }
                }, 0, 0, null, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " maps.google.com");
                        sendErrorNotification(context, "Google API : " + error.toString());
                    }
                });
        // Add ImageRequest to the RequestQueue
        requestQueue.add(request);
    }

    /**
     * Request SearchAPI nearest the user location
     */
    private void searchAPIRequest(final Location location, final boolean withGoogleMapStatic) {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        String urlAPI = getStoreAPIUrl(location.getLatitude(), location.getLongitude());
        StringRequest stringRequest = APIHelperCore.getInstance(context).createGetReuqest(
            urlAPI,
            response -> {
                Gson gson = new Gson();
                SearchAPI data = gson.fromJson(response, SearchAPI.class);
                Feature featureSearch = data.getFeatures()[0];
                String city = featureSearch.getProperties().getAddress().getCity();
                String zipcode = featureSearch.getProperties().getAddress().getZipcode();
                String distance = String.valueOf(featureSearch.getProperties().getDistance());
                double longitudePOI = featureSearch.getGeometry().getLocation().getLng();
                double latitudePOI = featureSearch.getGeometry().getLocation().getLat();
                // Fill body message with informations from API
                String messageBody = "city = " + city + "\nzipcode =" + zipcode + "\ndistance = " + distance;

                // With Google Map Static in the notification
                if (withGoogleMapStatic) {
                    googleMapStaticAPIRequest(location, longitudePOI, latitudePOI, messageBody);
                } else {

                    mBuilder.setContentTitle("Location Notification");
                    mBuilder.setContentIntent(mPendingIntent);
                    mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(messageBody));
                    mBuilder.setContentIntent(mPendingIntent);
                    Notification notification = mBuilder.build();
                    mNotificationManager.notify(new Random().nextInt(20), notification);
                }
            },
            error -> {
                // Error request
                Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " search API");
                sendErrorNotification(context, "Search API : " + error.toString());
            }
        );
        requestQueue.add(stringRequest);
    }

    public String getStoreAPIUrl(Double lat, Double lng) {
        String url = String.format(WoosmapSettingsCore.SearchAPIUrl, WoosmapSettingsCore.WoosmapURL, WoosmapSettingsCore.privateKeyWoosmapAPI, lat, lng);
        if (!WoosmapSettingsCore.searchAPIParameters.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder(url);

            for (Map.Entry<String, String> entry : WoosmapSettingsCore.searchAPIParameters.entrySet()) {
                stringBuilder.append("&" + entry.getKey() + "=" + entry.getValue());
            }

            url = stringBuilder.toString();
        }
        return url;
    }

    private static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void getLatestLocation(Context context, OnSuccessListener<Location> successListener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(WoosmapSettingsCore.WoosmapSdkTag, "No permission");
        } else {
            FusedLocationProviderClient locationProvider = LocationServices.getFusedLocationProviderClient(context);
            locationProvider.getLastLocation().addOnSuccessListener(successListener);
        }
    }

    private void sendErrorNotification(Context context, String errorMsg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WoosmapSettingsCore.WoosmapNotificationChannelID)
                .setSmallIcon(R.drawable.ic_shopping_cart_black_24dp)
                .setContentTitle(WoosmapSettingsCore.NotificationError)
                .setContentText(errorMsg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationManager.notify(new Random().nextInt(20), builder.build());
    }
}
