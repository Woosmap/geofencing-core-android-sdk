package com.webgeoservices.samplecore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.webgeoservices.samplecore.model.PlaceData;
import com.webgeoservices.woosmapgeofencingcore.FigmmForVisitsCreatorCore;
import com.webgeoservices.woosmapgeofencingcore.PositionsManagerCore;
import com.webgeoservices.woosmapgeofencingcore.WoosmapCore;
import com.webgeoservices.woosmapgeofencingcore.WoosmapSettingsCore;
import com.webgeoservices.woosmapgeofencingcore.database.Distance;
import com.webgeoservices.woosmapgeofencingcore.database.MovingPosition;
import com.webgeoservices.woosmapgeofencingcore.database.POI;
import com.webgeoservices.woosmapgeofencingcore.database.Region;
import com.webgeoservices.woosmapgeofencingcore.database.RegionLog;
import com.webgeoservices.woosmapgeofencingcore.database.Visit;
import com.webgeoservices.woosmapgeofencingcore.database.WoosmapDb;
import com.webgeoservices.woosmapgeofencingcore.database.ZOI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final boolean AIRSHIP = false;
    static SimpleDateFormat displayDateFormatAirship = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationFragment locationFragment;
    private MapFragment mapFragment;
    private VisitFragment visitFragment;

    private POI[] POIData = new POI[0];

    private boolean isMenuOpen = false;

    private BottomNavigationView bottomNav;

    private WoosmapCore woosmapCore;

    public class WoosLocationReadyListener implements WoosmapCore.LocationReadyListener {
        public void LocationReadyCallback(Location location) {
            onLocationCallback(location);
        }
    }

    private void onLocationCallback(Location currentLocation) {
    }


    public class WoosSearchAPIReadyListener implements WoosmapCore.SearchAPIReadyListener {
        public void SearchAPIReadyCallback(POI poi) {
            onPOICallback(poi);
        }
    }

    private void onPOICallback(POI poi) {
    }

    public class WoosDistanceReadyListener implements WoosmapCore.DistanceReadyListener {
        public void DistanceReadyCallback(Distance[] distances) {
            onDistanceCallback(distances);
        }
    }

    private void onDistanceCallback(Distance[] distances) {
    }

    public class WoosVisitReadyListener implements WoosmapCore.VisitReadyListener {
        public void VisitReadyCallback(Visit visit) {
            onVisitCallback(visit);
        }
    }

    private void onVisitCallback(Visit visit) {
    }

    public class WoosRegionReadyListener implements WoosmapCore.RegionReadyListener {
        public void RegionReadyCallback(Region region) {
            onRegionCallback(region);
        }
    }

    private void onRegionCallback(Region region) {
    }

    public class WoosRegionLogReadyListener implements WoosmapCore.RegionLogReadyListener {
        public void RegionLogReadyCallback(RegionLog regionLog) {
            onRegionLogCallback(regionLog);
        }
    }

    private void onRegionLogCallback(RegionLog regionLog) {
        createNotification("Region update from geofence detection","Region : " + regionLog.identifier + "\n" + "didenter : " + regionLog.didEnter +
                "\n" + "isCurrentPositionInside : " + regionLog.isCurrentPositionInside +
                "\n" + "Date : " + displayDateFormatAirship.format(regionLog.dateTime));
    }

    public class WoosProfileReadyListener implements WoosmapCore.ProfileReadyListener {
        @Override
        public void ProfileReadyCallback(Boolean status, ArrayList<String> errors) {
            System.out.println("Geofencing SDK - Custom profil Status = " + status);
            for (int i=0; i<errors.size(); i++) {
                System.out.println(errors.get(i));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            Log.d("WoosmapGeofencing", "Permission OK");
            this.woosmapCore.onResume();
        } else {
            Log.d("WoosmapGeofencing", "Permission NOK");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("WoosmapGeofencing", "BackGround");
        if (checkPermissions()) {
            this.woosmapCore.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        woosmapCore.onDestroy();
        super.onDestroy();
    };


    public void createNotification(String title, String body) {
        final int NOTIFY_ID = 1002;

        // There are hardcoding only for show it's just strings
        String name = "my_package_channel";
        String id = "my_package_channel_1"; // The user-visible name of the channel.
        String description = "my_package_first_channel"; // The user-visible description of the channel.

        Intent intent;
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setLightColor( Color.GREEN);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, id);

            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

            builder.setContentTitle(title)  // required
                    .setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setContentText(body)
                    .setDefaults( Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        } else {

            builder = new NotificationCompat.Builder(this);

            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            builder.setContentTitle(title)                           // required
                    .setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setContentText(body)  // required
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder.build();
        notifManager.notify(NOTIFY_ID, notification);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationFragment = new LocationFragment();
        mapFragment = new MapFragment();
        visitFragment = new VisitFragment();

        POIData = new POI[0];

        setFragment(mapFragment);
        setFragment(visitFragment);
        setFragment(locationFragment);

        loadData();
        loadPOI();
        loadVisit();
        loadZOI();
        loadRegion();
        loadRegionLogs();

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_map:
                        setFragment(mapFragment);
                        return true;
                    case R.id.navigation_location:
                        setFragment(locationFragment);
                        return true;
                    case R.id.navigation_visit:
                        setFragment(visitFragment);
                        return true;
                    default:
                        return false;
                }
            }

        });

        // TODO this could maybe be remove with new profiles methods
        final SharedPreferences mPrefs = getApplicationContext().getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        boolean trackingEnable = mPrefs.getBoolean("trackingEnable",true);
        boolean distanceAPIEnable = mPrefs.getBoolean("distanceAPIEnable",true);

        boolean modeHighFrequencyLocationEnable = mPrefs.getBoolean("modeHighFrequencyLocationEnable",false);
        WoosmapSettingsCore.trackingEnable = trackingEnable;
        WoosmapSettingsCore.distanceAPIEnable = distanceAPIEnable;
        WoosmapSettingsCore.modeHighFrequencyLocation = modeHighFrequencyLocationEnable;

        // Set Filter on user Location
        //WoosmapSettings.currentLocationTimeFilter = 30;

        // Set Filter on search API
        //WoosmapSettings.searchAPITimeFilter = 30;
        //WoosmapSettings.searchAPIDistanceFilter = 50;

        // Set Filter on Accuracy of the location
        //WoosmapSettings.accuracyFilter = 10;

        // Instanciate woosmap object
        this.woosmapCore = WoosmapCore.getInstance().initializeWoosmap(this);

        // Set the Delay of Duration data
        WoosmapSettingsCore.numberOfDayDataDuration = 30;

        // Set Keys
        //WoosmapSettingsCore.privateKeyWoosmapAPI = "406cb325-a5fd-417f-8781-0c38682894e1";
        WoosmapSettingsCore.privateKeyWoosmapAPI = "fa3cff08-d288-4ff6-99a6-d98fffb7e7d6";
        WoosmapSettingsCore.privateKeyGMPStatic = "";

        WoosmapSettingsCore.foregroundLocationServiceEnable = true;

        this.woosmapCore.setLocationReadyListener(new WoosLocationReadyListener());
        this.woosmapCore.setSearchAPIReadyListener(new WoosSearchAPIReadyListener());
        this.woosmapCore.setDistanceReadyListener(new WoosDistanceReadyListener());
        this.woosmapCore.setVisitReadyListener(new WoosVisitReadyListener());
        this.woosmapCore.setRegionReadyListener( new WoosRegionReadyListener() );
        this.woosmapCore.setRegionLogReadyListener( new WoosRegionLogReadyListener() );
        this.woosmapCore.setProfileReadyListener( new WoosProfileReadyListener() );



        // Search API parameters
        //WoosmapSettings.searchAPIParameters.put("radius","5000");
        //WoosmapSettings.searchAPIParameters.put("stores_by_page","100");

        // User properties filter
        //WoosmapSettings.userPropertiesFilter.add( "creation_year" );

        // Fix the radius of geofence POI
        //WoosmapSettings.poiRadiusNameFromResponse = "near_radius";



        // For android version >= 8 you have to create a channel or use the woosmap's channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.woosmapCore.createWoosmapNotifChannel();
        }

        this.InitializeOptionsPanel();
    }

    private void InitializeOptionsPanel() {
        final SharedPreferences mPrefs = getApplicationContext().getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPrefs.edit();
        final FloatingActionButton enableLocationUpdateBtn = findViewById(R.id.UpdateLocation);
        enableLocationUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettingsCore.modeHighFrequencyLocation = !WoosmapSettingsCore.modeHighFrequencyLocation;
                String msg = "";
                if(WoosmapSettingsCore.modeHighFrequencyLocation) {
                    msg = "Mode High Frequency Location Enable";
                    editor.putBoolean( "modeHighFrequencyLocationEnable",true);
                    editor.apply();
                    enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                } else {
                    msg = "Mode High Frequency Location disable";
                    editor.putBoolean( "modeHighFrequencyLocationEnable",false);
                    editor.apply();
                    enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                woosmapCore.enableModeHighFrequencyLocation(WoosmapSettingsCore.modeHighFrequencyLocation );
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();


            }
        });

        final FloatingActionButton clearDBBtn = findViewById(R.id.clearDB);
        clearDBBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clear Database", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                new clearDBTask(getApplicationContext(), MainActivity.this).execute();
            }
        });

        final FloatingActionButton testZOIBtn = findViewById(R.id.TestZOI);
        testZOIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Create ZOI", 8000)
                        .setAction("Action", null).show();
                new testZOITask(getApplicationContext(), MainActivity.this).execute();
                //new testDataImportTask(getApplicationContext(), MainActivity.this).execute();
            }
        });

        final FloatingActionButton enableLocationBtn = findViewById(R.id.EnableLocation);
        enableLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettingsCore.trackingEnable = !WoosmapSettingsCore.trackingEnable;
                String msg = "";
                if(WoosmapSettingsCore.trackingEnable) {
                    msg = "Tracking Enable";
                    editor.putBoolean( "trackingEnable",true);
                    editor.apply();
                    enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                } else {
                    msg = "Tracking Disable";
                    editor.putBoolean( "trackingEnable",false);
                    editor.apply();
                    enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                woosmapCore.enableTracking(WoosmapSettingsCore.trackingEnable);
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final FloatingActionButton enableSearchAPIBtn = findViewById(R.id.EnableSearchAPI);
        enableSearchAPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        final FloatingActionButton enableDistanceAPIBtn = findViewById(R.id.EnableDistanceAPI);
        enableDistanceAPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WoosmapSettingsCore.distanceAPIEnable = !WoosmapSettingsCore.distanceAPIEnable;
                String msg = "";
                if(WoosmapSettingsCore.distanceAPIEnable) {
                    msg = "DistanceAPI Enable";
                    editor.putBoolean( "distanceAPIEnable",true);
                    editor.apply();
                    enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                }else {
                    msg = "DistanceAPI Disable";
                    editor.putBoolean( "distanceAPIEnable",false);
                    editor.apply();
                    enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if(WoosmapSettingsCore.trackingEnable) {
            enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        } else {
            enableLocationBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        if(WoosmapSettingsCore.modeHighFrequencyLocation) {
            enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        } else {
            enableLocationUpdateBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        if(WoosmapSettingsCore.distanceAPIEnable) {
            enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        }else {
            enableDistanceAPIBtn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
        }

        final FloatingActionButton menuSettings = findViewById(R.id.Menu);
        menuSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isMenuOpen) {
                    isMenuOpen = true;
                    clearDBBtn.animate().translationY(-200);
                    testZOIBtn.animate().translationY(-400);
                    enableDistanceAPIBtn.animate().translationY(-600);
                    enableSearchAPIBtn.animate().translationY(-800);
                    enableLocationBtn.animate().translationY(-1000);
                    enableLocationUpdateBtn.animate().translationY(-1200);
                } else {
                    isMenuOpen = false;
                    clearDBBtn.animate().translationY(0);
                    testZOIBtn.animate().translationY(0);
                    enableDistanceAPIBtn.animate().translationY(0);
                    enableSearchAPIBtn.animate().translationY(0);
                    enableLocationBtn.animate().translationY(0);
                    enableLocationUpdateBtn.animate().translationY(0);
                }
            }
        });

    }

    private void loadPOI() {
        final LiveData<POI[]> POIList = WoosmapDb.getInstance(getApplicationContext()).getPOIsDAO().getAllLivePOIs();
        POIList.observe(this, new Observer<POI[]>() {
            @Override
            public void onChanged(POI[] pois) {
                POIData = pois;
            }
        });
    }

    private void loadVisit() {
        final LiveData<Visit[]> VisitList = WoosmapDb.getInstance(getApplicationContext()).getVisitsDao().getAllLiveStaticPositions();
        VisitList.observe(this, new Observer<Visit[]>() {
            @Override
            public void onChanged(Visit[] visits) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (Visit visitToShow : visits) {
                    PlaceData place = new PlaceData();
                    place.setType( PlaceData.dataType.visit );
                    place.setLatitude( visitToShow.lat );
                    place.setLongitude( visitToShow.lng );
                    place.setArrivalDate( visitToShow.startTime );
                    place.setDepartureDate( visitToShow.endTime );
                    place.setDuration( visitToShow.duration );
                    arrayOfPlaceData.add( place );

                    SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
                    if(visitToShow.duration >= WoosmapSettingsCore.durationVisitFilter) {
                        LatLng latLng = new LatLng( visitToShow.lat, visitToShow.lng );
                        String startFormatedDate = displayDateFormat.format( visitToShow.startTime );
                        String endFormatedDate = "";
                        if (visitToShow.endTime == 0) {
                            //Visit in progress
                            endFormatedDate = "ongoing";
                        } else {
                            endFormatedDate = displayDateFormat.format( visitToShow.endTime );
                        }
                        String infoVisites = " --> start: " + startFormatedDate + " / end: " + endFormatedDate + " NbPt : " + visitToShow.nbPoint;
                        MarkerOptions markerOptions = new MarkerOptions().position( latLng ).title( infoVisites );
                        boolean markerToUpdate = false;
                        for (MarkerOptions marker : MainActivity.this.mapFragment.markersVisit) {
                            if (marker.getPosition().equals( markerOptions.getPosition() )) {
                                //Update marker
                                markerToUpdate = true;
                                marker.title( markerOptions.getTitle() );
                            }
                        }
                        if (!markerToUpdate) {
                            MainActivity.this.mapFragment.markersVisit.add( markerOptions );
                            if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                                MainActivity.this.mapFragment.visitMarkerList.add( MainActivity.this.mapFragment.mGoolgeMap.addMarker( markerOptions.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_ORANGE ) ) ) );
                                if (!MainActivity.this.mapFragment.visitEnableCheckbox.isChecked()) {
                                    for (Marker marker : MainActivity.this.mapFragment.visitMarkerList) {
                                        marker.setVisible( false );
                                    }
                                }
                            }
                        }
                    }
                }
                MainActivity.this.visitFragment.loadDataFromVisit( arrayOfPlaceData );
            }
        });
    }

    private void loadRegion() {
        final LiveData<Region[]> regionList = WoosmapDb.getInstance( getApplicationContext() ).getRegionsDAO().getAllLiveRegions();
        regionList.observe( this, new Observer<Region[]>() {
            @Override
            public void onChanged(Region[] regions) {
                MainActivity.this.mapFragment.regions.clear();
                for (Region region : regions) {
                    MainActivity.this.mapFragment.regions.add(region);
                }

                if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                    MainActivity.this.mapFragment.clearCircleGeofence();
                    MainActivity.this.mapFragment.drawCircleGeofence();
                }
            }
        } );
    }

    private void loadZOI() {
        final LiveData<ZOI[]> zoiList = WoosmapDb.getInstance( getApplicationContext() ).getZOIsDAO().getAllLiveZois();
        zoiList.observe( this, new Observer<ZOI[]>() {
            @Override
            public void onChanged(ZOI[] zois) {
                MainActivity.this.mapFragment.zois.clear();
                MainActivity.this.mapFragment.clearPolygon();

                MainActivity.this.mapFragment.zois.addAll(Arrays.asList(zois));

                if (MainActivity.this.mapFragment.mGoolgeMap != null) {
                    MainActivity.this.mapFragment.drawPolygon();
                }
            }
        } );
    }

    private void loadRegionLogs() {
        final LiveData<RegionLog[]> regionLogList = WoosmapDb.getInstance( getApplicationContext() ).getRegionLogsDAO().getAllLiveRegionLogs();
        regionLogList.observe( this, new Observer<RegionLog[]>() {
            @Override
            public void onChanged(RegionLog[] regionLogs) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (RegionLog regionLogToShow : regionLogs) {
                    PlaceData place = new PlaceData( regionLogToShow );
                    arrayOfPlaceData.add( place );
                }
                MainActivity.this.visitFragment.loadDataFromRegionLog( arrayOfPlaceData );
            }
        } );
    }

    public void loadData() {
        final LiveData<MovingPosition[]> movingPositionList = WoosmapDb.getInstance(getApplicationContext()).getMovingPositionsDao().getLiveDataMovingPositions(-1);
        movingPositionList.observe(this, new Observer<MovingPosition[]>() {
            @Override
            public void onChanged(MovingPosition[] movingPositions) {
                final ArrayList<PlaceData> arrayOfPlaceData = new ArrayList<>();
                for (MovingPosition locationToShow : movingPositionList.getValue()) {
                    final PlaceData place = new PlaceData();
                    place.setType( PlaceData.dataType.location );
                    place.setLatitude( locationToShow.lat );
                    place.setLongitude( locationToShow.lng );
                    place.setDate(locationToShow.dateTime);
                    place.setLocationId( locationToShow.id );
                    for (POI poi : POIData) {
                        if (poi.locationId == locationToShow.id) {
                            place.setPOILatitude( poi.lat );
                            place.setPOILongitude( poi.lng );
                            place.setZipCode( poi.zipCode );
                            place.setCity( poi.city );
                            place.setDistance( poi.distance );
                            place.setTravelingDistance( poi.travelingDistance );
                            place.setType( PlaceData.dataType.POI );
                            place.setMovingDuration( poi.duration );
                            break;
                        }
                    }
                    arrayOfPlaceData.add( place );
                    if(place.getType() == PlaceData.dataType.location) {
                        LatLng latLng = new LatLng( place.getLatitude(), place.getLongitude() );
                        boolean markerToAdd = true;
                        MarkerOptions markerOptions = new MarkerOptions().position( latLng ).icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA ) );
                        if (!MainActivity.this.mapFragment.markersLocations.isEmpty()) {
                            for (MarkerOptions marker : MainActivity.this.mapFragment.markersLocations) {
                                if (marker.getPosition().equals( markerOptions.getPosition() )) {
                                    markerToAdd = false;
                                }
                            }
                        }
                        if (markerToAdd) {
                            MainActivity.this.mapFragment.markersLocations.add( markerOptions );
                            if (MainActivity.this.mapFragment.mGoolgeMap != null && MainActivity.this.mapFragment.isVisible()) {
                                MainActivity.this.mapFragment.locationMarkerList.add( MainActivity.this.mapFragment.mGoolgeMap.addMarker( markerOptions ) );
                                if (!MainActivity.this.mapFragment.locationEnableCheckbox.isChecked()) {
                                    for (Marker marker : MainActivity.this.mapFragment.locationMarkerList) {
                                        marker.setVisible( false );
                                    }
                                }
                            }
                        }
                    }

                    if (MainActivity.this.mapFragment.mGoolgeMap != null && MainActivity.this.mapFragment.isVisible()) {

                        if(place.getType() == PlaceData.dataType.POI) {
                            for (POI poi : POIData) {
                                if (poi.locationId == locationToShow.id) {
                                    place.setPOILatitude( poi.lat );
                                    place.setPOILongitude( poi.lng );
                                    place.setZipCode( poi.zipCode );
                                    place.setCity( poi.city );
                                    place.setDistance( poi.distance );
                                    place.setTravelingDistance( poi.travelingDistance );
                                    place.setType( PlaceData.dataType.POI );
                                    place.setMovingDuration( poi.duration );
                                    LatLng latLng = new LatLng( place.getPOILatitude(), place.getPOILongitude() );
                                    boolean markerToAdd = true;
                                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(place.getCity()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                    if (!MainActivity.this.mapFragment.markersPOI.isEmpty()) {
                                        for (MarkerOptions marker : MainActivity.this.mapFragment.markersPOI) {
                                            if (marker.getPosition().equals(markerOptions.getPosition())) {
                                                markerToAdd =false;
                                            }
                                        }
                                    }
                                    if(markerToAdd) {
                                        MainActivity.this.mapFragment.markersPOI.add(markerOptions);
                                        MainActivity.this.mapFragment.poiMarkerList.add(MainActivity.this.mapFragment.mGoolgeMap.addMarker(markerOptions));
                                        if(!MainActivity.this.mapFragment.POIEnableCheckbox.isChecked()){
                                            for (Marker marker : MainActivity.this.mapFragment.poiMarkerList) {
                                                marker.setVisible(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                MainActivity.this.locationFragment.loadData( arrayOfPlaceData );
            }
        });
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int finePermissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return finePermissionState == PackageManager.PERMISSION_GRANTED || coarsePermissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("WoosmapGeofencing", "Displaying permission rationale to provide additional context.");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i("WoosmapGeofencing", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        Log.i( "WoosmapGeofencing", "onRequestPermissionResult" );
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i( "WoosmapGeofencing", "User interaction was cancelled." );
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i( "WoosmapGeofencing", "Permission granted, updates requested, starting location updates" );
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar( R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
                                Uri uri = Uri.fromParts( "package", getPackageName(), null );
                                intent.setData( uri );
                                intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                                startActivity( intent );
                            }
                        } );
            }
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    public static class clearDBTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        clearDBTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            WoosmapDb.getInstance(mContext).clearAllTables();
            WoosmapCore.getInstance().removeGeofence();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mActivity.locationFragment.adapter != null)
                mActivity.locationFragment.clearData();
            if (mActivity.visitFragment.adapter != null)
                mActivity.visitFragment.clearData();
            mActivity.mapFragment.clearMarkers();
        }
    }

    public class testZOITask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        testZOITask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream in = getResources().openRawResource(R.raw.visit_qualif);
            // if you want less visits and ZOI you can load the file location.csv like that :
            //InputStream in = getResources().openRawResource(R.raw.location);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            FigmmForVisitsCreatorCore figmmForVisitsCreator = new FigmmForVisitsCreatorCore(WoosmapDb.getInstance(mContext));
            try {
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    String[] separated = line.split(",");
                    String id = separated[0];
                    double accuracy = Double.valueOf(separated[1]);
                    String[] valLatLng = separated[2].replace("POINT(","").replace(")","").split(" ");

                    double x = Double.valueOf(valLatLng[0]);
                    double y = Double.valueOf(valLatLng[1]);

                    long startime = formatter.parse(separated[3]).getTime();
                    long endtime = formatter.parse(separated[4]).getTime();

                    Visit visit = new Visit();
                    visit.lat = y;
                    visit.lng = x;
                    visit.startTime = startime;
                    visit.endTime = endtime;
                    visit.accuracy = (float) accuracy;
                    visit.uuid = id;
                    visit.duration = visit.endTime - visit.startTime;

                    WoosmapDb.getInstance(mContext).getVisitsDao().createStaticPosition(visit);

                    figmmForVisitsCreator.figmmForVisitTest(visit);
                    i++;
                }
                figmmForVisitsCreator.update_db();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    public class testDataImportTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        public MainActivity mActivity;

        testDataImportTask(Context context, MainActivity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream in = getResources().openRawResource(R.raw.dataimport);
            // if you want less visits and ZOI you can load the file location.csv like that :
            //InputStream in = getResources().openRawResource(R.raw.location);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            PositionsManagerCore mPositionsManager = new PositionsManagerCore(mContext,WoosmapDb.getInstance( mContext),WoosmapCore.getInstance());
            try {
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    String[] separated = line.split(",");
                    String id = separated[0];
                    double accuracy = Double.valueOf(separated[3]);

                    Location loc = new Location("test");
                    loc.setTime( Long.parseLong( separated[5] ) );
                    loc.setAccuracy( (float) accuracy );
                    loc.setLatitude( Double.valueOf(separated[1]));
                    loc.setLongitude(  Double.valueOf(separated[2]));

                    List<Location> listLocations = new ArrayList<Location>();
                    listLocations.add(loc);
                    Thread.sleep(200);
                    mPositionsManager.asyncManageLocation(listLocations);

                    i++;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}