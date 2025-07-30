package com.webgeoservices.woosmapgeofencingcore.database;

import android.util.Log;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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
    @Deprecated
    public boolean openNow;
    public String userProperties;
    public String openingHours;

    public Map<String, Object> getUserPropertyMap(){
        HashMap map = new HashMap<String, Object>();
        if (userProperties!=null && !userProperties.isEmpty()){
            map = new Gson().fromJson(userProperties, map.getClass());
        }
        return map;
    }

    public boolean openNow(){
        if (openingHours == null || openingHours.isEmpty()) {
            return false;
        }
        try{
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(openingHours, JsonObject.class);

            // POI timezone
            String timezoneStr = json.has("timezone") ? json.get("timezone").getAsString() : "UTC";
            ZoneId poiZoneId = ZoneId.of(timezoneStr);

            // POI-local time
            ZonedDateTime poiNow = ZonedDateTime.now(poiZoneId);
            LocalDate today = poiNow.toLocalDate();
            LocalTime currentTime = poiNow.toLocalTime();
            DayOfWeek dayOfWeek = poiNow.getDayOfWeek(); // 1 = Monday, 7 = Sunday
            String dayKey = String.valueOf(dayOfWeek.getValue());

            // Device-local time
            ZonedDateTime deviceNow = ZonedDateTime.now();
            Log.d("POI-DEBUG", "Device time: " + deviceNow.toString());
            Log.d("POI-DEBUG", "POI time (" + timezoneStr + "): " + poiNow.toString());

            // Check temporary closure
            if (json.has("temporary_closure")) {
                JsonArray closures = json.getAsJsonArray("temporary_closure");
                for (JsonElement elem : closures) {
                    JsonObject range = elem.getAsJsonObject();
                    LocalDate start = LocalDate.parse(range.get("start").getAsString());
                    LocalDate end = LocalDate.parse(range.get("end").getAsString());
                    if (!today.isBefore(start) && !today.isAfter(end)) {
                        Log.d("POI-DEBUG", "Closed due to temporary closure");
                        return false;
                    }
                }
            }

            // Check special hours
            if (json.has("special")) {
                JsonObject special = json.getAsJsonObject("special");
                String todayKey = today.toString(); // e.g. 2025-08-15
                if (special.has(todayKey)) {
                    JsonArray specialHours = special.getAsJsonArray(todayKey);
                    boolean open = isOpenDuring(currentTime, specialHours);
                    Log.d("POI-DEBUG", "Using special hours for " + todayKey + ": " + open);
                    return open;
                }
            }

            // Check usual hours
            if (json.has("usual")) {
                JsonObject usual = json.getAsJsonObject("usual");
                JsonArray todayHours = usual.has(dayKey) ? usual.getAsJsonArray(dayKey) :
                        usual.has("default") ? usual.getAsJsonArray("default") : new JsonArray();
                boolean open = isOpenDuring(currentTime, todayHours);
                Log.d("POI-DEBUG", "Using usual/default hours for day " + dayKey + ": " + open);
                return open;
            }
        }
        catch (Exception ex){
            Log.e("POI-DEBUG", "Error while parsing opening hours", ex);
        }
        return false;
    }

    private boolean isOpenDuring(LocalTime now, JsonArray timeRanges) {
        for (JsonElement elem : timeRanges) {
            JsonObject range = elem.getAsJsonObject();

            if (range.has("all-day") && range.get("all-day").getAsBoolean()) {
                return true;
            }

            if (range.has("start") && range.has("end")) {
                LocalTime start = LocalTime.parse(range.get("start").getAsString(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime end = LocalTime.parse(range.get("end").getAsString(), DateTimeFormatter.ofPattern("HH:mm"));

                boolean isOvernight = start.isAfter(end);
                boolean isOpen;

                if (isOvernight) {
                    isOpen = !now.isBefore(start) || !now.isAfter(end);
                } else {
                    isOpen = !now.isBefore(start) && !now.isAfter(end);
                }

                if (isOpen) return true;
            }
        }
        return false;
    }
}

