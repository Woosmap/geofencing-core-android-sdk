package com.webgeoservices.woosmapgeofencingcore.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public abstract class POIsDAO {

    public boolean createPOI(POI poi){
        POI existingPOI = getPOIbyStoreId(poi.idStore);
        if (existingPOI == null){
            createNewPOI(poi);
            return true;
        }
        else{
            existingPOI.address = poi.address;
            existingPOI.city = poi.city;
            existingPOI.idStore = poi.idStore;
            existingPOI.data = poi.data;
            existingPOI.contact = poi.contact;
            existingPOI.countryCode = poi.countryCode;
            existingPOI.zipCode = poi.zipCode;
            existingPOI.duration = poi.duration;
            existingPOI.name = poi.name;
            existingPOI.tags = poi.tags;
            existingPOI.travelingDistance = poi.travelingDistance;
            existingPOI.types = poi.types;
            existingPOI.userProperties = poi.userProperties;
            existingPOI.dateTime = System.currentTimeMillis();
            existingPOI.distance = poi.distance;
            existingPOI.locationId = poi.locationId;
            existingPOI.radius = poi.radius;
            existingPOI.lat = poi.lat;
            existingPOI.lng = poi.lng;
            existingPOI.openNow = poi.openNow;
            updatePOI(existingPOI);
            return false;
        }
    }

    @Insert
    abstract void createNewPOI(POI poi);

    @Update
    public abstract void updatePOI(POI poi);

    @Delete
    public abstract void deletePOI(POI poi);

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 1")
    public abstract POI getLastPOI();

    @Query("SELECT * FROM POI WHERE distance = (SELECT MAX(distance) FROM POI WHERE distance = (SELECT MAX(dateTime) FROM POI)) ")
    public abstract POI getLastfurthestPOI();

    @Query("SELECT * FROM POI WHERE locationId = :locId")
    public abstract POI getPOIbyLocationID(int locId);

    @Query("SELECT * FROM POI WHERE idStore = :idStore")
    public abstract POI getPOIbyStoreId(String idStore);

    @Query("SELECT * FROM POI WHERE locationId = :locId")
    public abstract LiveData<POI> getPOIbyLocationID2(int locId);

    @Query("SELECT * FROM POI ORDER BY dateTime DESC LIMIT 1,2")
    public abstract POI getPreviousLastPOI();

    @Query("DELETE FROM POI")
    public abstract void deleteAllPOIs();

    @Query("SELECT * FROM POI ORDER BY dateTime")
    public abstract POI [] getAllPOIs();

    @Query("DELETE FROM POI WHERE dateTime <= :dataDurationDelay")
    public abstract void deletePOIOlderThan(long dataDurationDelay);

    @Query("SELECT * FROM POI ORDER BY dateTime")
    public abstract LiveData<POI[]> getAllLivePOIs();
}


