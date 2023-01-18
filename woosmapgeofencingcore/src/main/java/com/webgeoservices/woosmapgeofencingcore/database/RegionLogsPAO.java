package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;


@Dao
public interface RegionLogsPAO {
    @Insert
    void createRegionLog(RegionLog regionLog);

    @Delete
    void deleteRegion(RegionLog regionLog);

    @Query("SELECT * FROM regionLogs ORDER BY dateTime DESC LIMIT 1")
    RegionLog getLastRegionLog();

    @Query("SELECT * FROM regionLogs ORDER BY dateTime DESC LIMIT 1,2")
    RegionLog getPreviousLastRegionLog();

    @Query("SELECT * FROM regionLogs WHERE identifier = :identifier")
    RegionLog getRegionLogFromId(String identifier);

    @Query("SELECT * FROM regionLogs WHERE identifier = :identifier ORDER BY dateTime DESC LIMIT 1")
    RegionLog getLatestRegionLogFromId(String identifier);

    @Query("DELETE FROM regionLogs")
    void deleteAllRegionLogs();

    @Query("SELECT * FROM regionLogs ORDER BY dateTime")
    RegionLog [] getAllRegionLogs();

    @Query("SELECT * FROM regionLogs ORDER BY dateTime")
    public abstract LiveData<RegionLog []>  getAllLiveRegionLogs();

    @Query("DELETE FROM regionLogs WHERE dateTime <= :dataDurationDelay")
    void deleteRegionLogsOlderThan(long dataDurationDelay);
}
