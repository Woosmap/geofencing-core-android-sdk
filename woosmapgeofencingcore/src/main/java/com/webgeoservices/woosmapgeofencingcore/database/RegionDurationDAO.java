package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RegionDurationDAO {
    @Insert
    void createRegionDuration(RegionDuration regionDuration);

    @Update
    void updateRegionDuration(RegionDuration regionDuration);

    @Query("SELECT * FROM RegionDuration WHERE regionID = :regionID")
    RegionDuration getRegionDuration(int regionID);
}
