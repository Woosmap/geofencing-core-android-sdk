package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RegionDurationDAO {
    @Insert
    void createRegionDuration(RegionDuration regionDuration);

    @Update
    void updateRegionDuration(RegionDuration regionDuration);

    @Delete
    void deleteRegionDuration(RegionDuration regionDuration);

    @Query("SELECT * FROM RegionDuration WHERE regionIdentifier like :regionIdentifier ORDER BY entryTime DESC LIMIT 1")
    RegionDuration getRegionDuration(String regionIdentifier);

    @Query("DELETE FROM RegionDuration WHERE entryTime <= :dataDurationDelay")
    void deleteRegionDurationsOlderThan(long dataDurationDelay);
}
