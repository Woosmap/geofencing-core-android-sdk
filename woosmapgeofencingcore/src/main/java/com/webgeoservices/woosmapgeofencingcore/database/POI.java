package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "POI")

public class POI {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int locationId;
    public double lat;
    public double lng;
    public String name;
    public String idStore;
    public String city;
    public String zipCode;
    public double distance;
    public String travelingDistance;
    public String duration;
    public long dateTime;
    public String data;
    public int radius;
    public String contact;
    public String tags;
    public String types;
    public String countryCode;
    public String address;
    public boolean openNow;
    public String userProperties;

}

