package com.webgeoservices.woosmapgeofencingcore;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel.Feature;
import com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel.SearchAPIResponseItemCore;
import com.webgeoservices.woosmapgeofencingcore.database.POI;

import java.util.Collections;
import java.util.List;

public class Util {

    private static Feature getFeatureObjectFromJSON(LinkedTreeMap<String, Object> featureObject){
        LinkedTreeMap<String, Object> properties = (LinkedTreeMap<String, Object>) featureObject.get("properties");
        if (properties.containsKey("types")){
            Object typesValue = properties.get("types");

            if (typesValue instanceof String) {
                // If "types" is a String, convert it to a list of strings
                String typesString = (String) typesValue;
                List<String> typesList = Collections.singletonList(typesString);
                properties.put("types", typesList);
                featureObject.put("properties", properties);
            }
        }

        properties = (LinkedTreeMap<String, Object>) featureObject.get("properties");
        if (properties.containsKey("tags")){
            Object tagsValue = properties.get("tags");

            if (tagsValue instanceof String) {
                // If "tags" is a String, convert it to a list of strings
                String tagsString = (String) tagsValue;
                List<String> tagsList = Collections.singletonList(tagsString);
                properties.put("tags", tagsList);
                featureObject.put("properties", properties);
            }
        }
        Gson gson = new Gson();
        Feature feature = gson.fromJson(gson.toJson(featureObject).toString(), Feature.class);
        return feature;
    }

    protected static POI getPOIFromFeature(LinkedTreeMap<String, Object> featureObject, int positionId, String response){
        Feature feature = getFeatureObjectFromJSON(featureObject);
        SearchAPIResponseItemCore searchAPIResponseItem = SearchAPIResponseItemCore.fromFeature(feature);
        POI POIaround = new POI();
        POIaround.city = searchAPIResponseItem.city;
        POIaround.zipCode = searchAPIResponseItem.zipCode;
        POIaround.dateTime = System.currentTimeMillis();
        POIaround.distance = searchAPIResponseItem.distance;
        POIaround.locationId = positionId;
        POIaround.idStore = searchAPIResponseItem.idstore;
        POIaround.name = searchAPIResponseItem.name;
        POIaround.lat = searchAPIResponseItem.geometry.getLocation().getLat();
        POIaround.lng = searchAPIResponseItem.geometry.getLocation().getLng();
        POIaround.address = searchAPIResponseItem.formattedAddress;
        POIaround.contact = searchAPIResponseItem.contact;
        POIaround.types = String.join(" - ", searchAPIResponseItem.types);
        POIaround.tags = String.join(" - ", searchAPIResponseItem.tags);
        POIaround.countryCode = searchAPIResponseItem.countryCode;
        POIaround.data = response;
        if (searchAPIResponseItem.userProperties != null){
            POIaround.userProperties = new Gson().toJson(searchAPIResponseItem.userProperties);
        }
        return POIaround;
    }
}
