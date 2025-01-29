package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "regionLogs")

public class RegionLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int locationId;
    public String identifier;
    public double lat;
    public double lng;
    public double radius;
    public boolean didEnter = false;
    public String idStore = "";
    public long dateTime;
    public boolean isCurrentPositionInside = false;
    public int distance = 0;
    public String distanceText = "";
    public int duration = 0;
    public String durationText = "";
    public String type = "circle";
    public float expectedAverageSpeed=-1f;
    public String eventName="";
    public long spentTime = 0;
    public long addedOn;
}
