package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "RegionDuration")
public class RegionDuration {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int regionID;
    public long entryTime;
    public long exitTime;
}
