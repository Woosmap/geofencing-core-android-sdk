package com.webgeoservices.woosmapgeofencingcore.database;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.webgeoservices.woosmapgeofencingcore.FigmmForVisitsCreatorCore;
import com.webgeoservices.woosmapgeofencingcore.WoosmapSettingsCore;

@Database(entities = {Visit.class, MovingPosition.class, POI.class, ZOI.class, Region.class, RegionLog.class, Distance.class,RegionDuration.class}, version = 38, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WoosmapDb extends RoomDatabase {

    public abstract VisitsDao getVisitsDao();

    public abstract MovingPositionsDao getMovingPositionsDao();

    public abstract POIsDAO getPOIsDAO();

    public abstract ZOIsDAO getZOIsDAO();

    public abstract RegionsPAO getRegionsDAO();

    public abstract RegionLogsPAO getRegionLogsDAO();

    public abstract DistancesDAO getDistanceDAO();

    public abstract RegionDurationDAO getRegionDurationDAO();

    private static volatile WoosmapDb instance;

    public static synchronized WoosmapDb getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static WoosmapDb create(final Context context) {
        return Room.databaseBuilder(
                context,
                WoosmapDb.class,
                "database-woosmap")
                .fallbackToDestructiveMigration()
                .build();
    }

    public void cleanOldGeographicData(final Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences("WGSGeofencingPref",MODE_PRIVATE);

        long lastUpdate = mPrefs.getLong("lastUpdate", 0);
        if (lastUpdate != 0) {
            long dateNow = System.currentTimeMillis();
            long timeDiffFromNow = dateNow - lastUpdate;
            //update date if no updating since 1 day
            FigmmForVisitsCreatorCore figmmForVisitsCreator = new FigmmForVisitsCreatorCore(WoosmapDb.getInstance(context));
            if (timeDiffFromNow > 86400000) {
                figmmForVisitsCreator.deleteVisitOnZoi(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getVisitsDao().deleteVisitOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getMovingPositionsDao().deleteMovingOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getPOIsDAO().deletePOIOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getDistanceDAO().deleteDistanceOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getRegionLogsDAO().deleteRegionLogsOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getRegionsDAO().deleteRegionsOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                getRegionDurationDAO().deleteRegionDurationsOlderThan(dateNow - WoosmapSettingsCore.dataDurationDelay);
                //Update date
                mPrefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply();
            }
        } else {
            //Update date
            mPrefs.edit().putLong("lastUpdate", System.currentTimeMillis()).apply();
        }
    }
}