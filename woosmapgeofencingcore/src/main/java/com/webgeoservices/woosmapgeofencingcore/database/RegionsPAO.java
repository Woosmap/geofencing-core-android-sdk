package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RegionsPAO {
    @Insert
    void createRegion(Region region);

    @Update
    void updateRegion(Region region);

    @Delete
    void deleteRegion(Region region);

    @Query("DELETE FROM regions WHERE identifier LIKE '%poi_%'")
    void deleteAllPOIRegion();

    @Query("DELETE FROM regions WHERE identifier = :id")
    void deleteRegionFromId(String id);

    @Query("SELECT * FROM regions WHERE type='isochrone'")
    Region [] getRegionIsochrone();

    @Query("SELECT * FROM regions WHERE type='circle'")
    Region [] getRegionCircle();

    @Query("SELECT * FROM regions WHERE identifier LIKE '%poi_%'")
    Region [] getRegionPOI();

    @Query("SELECT * FROM regions ORDER BY dateTime DESC LIMIT 1")
    Region getLastRegion();

    @Query("SELECT * FROM regions ORDER BY dateTime DESC LIMIT 1,2")
    Region getPreviousLastRegion();

    @Query("SELECT * FROM regions WHERE identifier = :identifier")
    Region getRegionFromId(String identifier);

    @Query("DELETE FROM regions")
    void deleteAllRegions();

    @Query("SELECT * FROM regions ORDER BY dateTime")
    Region [] getAllRegions();

    @Query("SELECT * FROM regions ORDER BY dateTime")
    public abstract LiveData<Region []> getAllLiveRegions();

    @Query("DELETE FROM regions WHERE dateTime <= :dataDurationDelay")
    void deleteRegionsOlderThan(long dataDurationDelay);
}
