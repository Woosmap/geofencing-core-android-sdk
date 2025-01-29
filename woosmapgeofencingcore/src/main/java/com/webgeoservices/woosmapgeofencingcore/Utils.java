package com.webgeoservices.woosmapgeofencingcore;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.webgeoservices.woosmapgeofencingcore.database.RegionLog;

import org.json.JSONObject;

public class Utils {
    public static JSONObject regionLogToJSON(RegionLog regionLog){
        JSONObject regionData = new JSONObject();
        try{
            regionData.put("lng", regionLog.lng);
            regionData.put("lat", regionLog.lat);
            regionData.put("id", regionLog.id);
            regionData.put("duration", regionLog.duration);
            regionData.put("spentTime", regionLog.spentTime);
            regionData.put("dateTime", regionLog.dateTime);
            regionData.put("didEnter", regionLog.didEnter);
            regionData.put("distance", regionLog.distance);
            regionData.put("expectedAverageSpeed", regionLog.expectedAverageSpeed);
            regionData.put("isCurrentPositionInside", regionLog.isCurrentPositionInside);
            regionData.put("locationId", regionLog.locationId);
            regionData.put("radius", regionLog.radius);
            regionData.put("distanceText", regionLog.distanceText);
            regionData.put("durationText", regionLog.durationText);
            regionData.put("eventName", regionLog.eventName);
            regionData.put("identifier", regionLog.identifier);
            regionData.put("idStore", regionLog.idStore);
            regionData.put("type", regionLog.type);
            regionData.put("addedOn", regionLog.addedOn);
        }
        catch (Exception ex){
            Log.e("Utils", ex.toString());
        }
        return regionData;
    }

    public static RegionLog JSONObjectToRegionLog(JSONObject regionData){
        RegionLog regionLog = new RegionLog();
        try{
            regionLog.identifier = regionData.getString("identifier");
            regionLog.dateTime = regionData.getLong("dateTime");
            regionLog.didEnter = regionData.getBoolean("didEnter");
            regionLog.lat = regionData.getDouble("lat");
            regionLog.lng = regionData.getDouble("lng");
            regionLog.idStore = regionData.getString("idStore");
            regionLog.radius = regionData.getDouble("radius");
            regionLog.isCurrentPositionInside = regionData.getBoolean("isCurrentPositionInside");
            regionLog.eventName =  regionData.getString("eventName");
            regionLog.type = regionData.getString("type");;
            regionLog.distance = regionData.getInt("distance");
            regionLog.duration = regionData.getInt("duration");
            regionLog.expectedAverageSpeed = (float) regionData.getDouble("expectedAverageSpeed");
            regionLog.locationId = regionData.getInt("locationId");
            regionLog.spentTime = regionData.getLong("spentTime");
            regionLog.distanceText = regionData.getString("distanceText");
            regionLog.durationText = regionData.getString("durationText");
            regionLog.locationId = regionData.getInt("locationId");
            regionLog.addedOn = regionData.getLong("addedOn");
        }
        catch (Exception ex){
            Log.e("Utils", ex.toString());
        }
        return regionLog;
    }

    protected static void sendRegionRecordEnteredBroadcast(RegionLog regionLog, Context context){
        try{
            Intent customIntent = new Intent("com.woosmap.REGION_LOG_ADDED");
            JSONObject jsonObject = regionLogToJSON(regionLog);
            customIntent.putExtra("regionLog", jsonObject.toString());
            context.sendBroadcast(customIntent);
        }
        catch (Exception ex){
            Log.e("Utils", ex.toString());
        }
    }
}
