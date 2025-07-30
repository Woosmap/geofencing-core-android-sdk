package com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.room.util.StringUtil;

import com.google.gson.Gson;
import com.webgeoservices.woosmapgeofencingcore.WoosmapSettingsCore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringJoiner;


/***
 * Data container class which holds the data returned by <code>details</code> method
 */
public class SearchAPIResponseItemCore implements Parcelable {
    private static final String TAG = SearchAPIResponseItemCore.class.getSimpleName();
    public String idstore;
    public String city;
    public String zipCode;
    public Double distance = 0.0;
    public String formattedAddress;
    public String name;
    public String[] types;
    public String[] tags;
    public String countryCode;
    public String contact;
    public boolean openNow;
    public JSONObject item;
    public Geometry geometry;
    public HashMap<String, Object> userProperties;
    public HashMap<String, Object> openingHours;

    /***
     * protected constructor
     */
    protected SearchAPIResponseItemCore() {
    }

    /***
     * Construct object from a parcel
     * @param in
     */
    protected SearchAPIResponseItemCore(Parcel in) {
        try {
            idstore = in.readString();
            formattedAddress = in.readString();
            name = in.readString();
            types = in.createStringArray();
            item = new JSONObject(in.readString());
            geometry = in.readParcelable(Geometry.class.getClassLoader());
        } catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(idstore);
        dest.writeString(formattedAddress);
        dest.writeString(name);
        dest.writeStringArray(types);
        dest.writeString(item.toString());
        dest.writeParcelable(geometry, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /***
     * Creator to create <code>SearchAPIResponseItemCore</code> object from a parcel
     */
    public static final Creator<SearchAPIResponseItemCore> CREATOR = new Creator<SearchAPIResponseItemCore>() {
        @Override
        public SearchAPIResponseItemCore createFromParcel(Parcel in) {
            return new SearchAPIResponseItemCore(in);
        }

        @Override
        public SearchAPIResponseItemCore[] newArray(int size) {
            return new SearchAPIResponseItemCore[size];
        }
    };


    /***
     * Item identifier. For <code>address</code> API, this is an internal identifier of the library
     * @return String
     */
    public String getIdstore() {
        return idstore;
    }

    /***
     * String containing the human-readable address of this item
     * @return String
     */
    public String getFormattedAddress() {
        return formattedAddress;
    }

    /***
     * Item name
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Array of feature types describing the given item (like <code>locality</code> or <code>postal_town</code>)
     *
     * @return String Array
     */
    public String[] getTypes() {
        return types;
    }

    /***
     * Underlying raw JSON that was returned by the API
     * @return <code>JSONObject</code>
     */
    public JSONObject getItem() {
        return item;
    }

    /***
     * Item geometry returned by the underlying API as <code>Geometry</code> object
     * @return <code>Geometry</code> object
     */
    public Geometry getGeometry() {
        return geometry;
    }


    /***
     * Static mathod which constructs and returns a new <code>SearchAPIResponseItemCore</code> object from the raw JSON response
     * @param jsonObject - Raw json response returned from the API
     * @return <code>SearchAPIResponseItemCore</code> object
     */
    public static SearchAPIResponseItemCore fromJSON(JSONObject jsonObject) {
        return populateStoreDetail(jsonObject);
    }

    public static SearchAPIResponseItemCore fromFeature(Feature feature){
        return populateStoreDetail(feature);
    }

    /***
     * Static mathod which constructs and returns a new <code>SearchAPIResponseItemCore</code> object from `Feature` object
     * @param feature - `Feature` object returned from the API
     * @return <code>SearchAPIResponseItemCore</code> object
     */
    protected static SearchAPIResponseItemCore populateStoreDetail(Feature feature){
        SearchAPIResponseItemCore detailsResponseItem = null;
        try{
            Properties properties = feature.getProperties();
            Gson gson = new Gson();
            detailsResponseItem = new SearchAPIResponseItemCore();
            detailsResponseItem.contact = gson.toJson(properties.getContact());
            detailsResponseItem.idstore = properties.getStoreID();
            detailsResponseItem.distance = properties.getDistance();
            if (properties.getOpen()!=null && properties.getOpen().containsKey("open_now")){
                detailsResponseItem.openNow = (Boolean) properties.getOpen().get("open_now");
            }
            if (properties.getAddress().getLines()!=null){
                ArrayList<String> lines = (ArrayList<String>)properties.getAddress().getLines();
                detailsResponseItem.formattedAddress =  TextUtils.join(" ", lines);
            }
            else{
                detailsResponseItem.formattedAddress = properties.getName();
            }
            detailsResponseItem.city = properties.getAddress().getCity();
            detailsResponseItem.zipCode = properties.getAddress().getZipcode();
            detailsResponseItem.countryCode = properties.getAddress().getCountryCode();

            String[] stringArray = Arrays.copyOf(properties.getTypes(), properties.getTypes().length, String[].class);
            detailsResponseItem.types = stringArray;
            stringArray = Arrays.copyOf(properties.getTags(), properties.getTags().length, String[].class);
            detailsResponseItem.tags = stringArray;
            detailsResponseItem.item = new JSONObject(gson.toJson(feature));
            detailsResponseItem.name = properties.getName();
            HashMap<String, Object> userProperties = new HashMap<>();
            if (properties.getUserProperties()!=null){
                for (String key: properties.getUserProperties().keySet()){
                    Object object = properties.getUserProperties().get(key);
                    userProperties.put(key, object);
                }
            }
            detailsResponseItem.userProperties = userProperties;

            HashMap<String, Object> openingHours = new HashMap<>();
            if (properties.getOpeningHours()!=null){
                for (String key: properties.getOpeningHours().keySet()){
                    Object object = properties.getOpeningHours().get(key);
                    openingHours.put(key, object);
                }
            }
            detailsResponseItem.openingHours = openingHours;

            Geometry geometryDetail = new Geometry();
            geometryDetail.setLocation(new Location(
                    feature.getGeometry().getCoordinates()[1],
                    feature.getGeometry().getCoordinates()[0]
            ));
            geometryDetail.setCoordinates(feature.getGeometry().getCoordinates());
            geometryDetail.setType(feature.getGeometry().getType());
            detailsResponseItem.geometry = geometryDetail;
        }
        catch (Exception ex){
            Log.e(TAG, ex.getMessage(), ex);
            return null;
        }
        return detailsResponseItem;
    }


    /***
     * Creates and returns <code>SearchAPIResponseItemCore</code> object from the response returned by <code>store</code> API
     * @param jsonObject RAW json response returned by <code>store</code> API
     * @return <code>SearchAPIResponseItemCore</code> object
     */
    protected static SearchAPIResponseItemCore populateStoreDetail(JSONObject jsonObject) {
        SearchAPIResponseItemCore detailsResponseItem = new SearchAPIResponseItemCore();
        JSONObject properties,addressObject;
        JSONArray addressLineArray;
        try {
            properties = jsonObject.getJSONObject("properties");
            detailsResponseItem.idstore = properties.getString("store_id");
            detailsResponseItem.contact = properties.getString("contact");
            detailsResponseItem.distance = (Double) properties.get("distance");

            if (properties.has("open")) {
                detailsResponseItem.openNow = properties.getJSONObject("open").getBoolean("open_now");
            }

            if (properties.has("address")) {
                addressObject = properties.getJSONObject("address");
                if(addressObject.has("lines") && !addressObject.get("lines").toString().equals("null")){
                    addressLineArray = addressObject.getJSONArray("lines");
                    StringBuilder formattedAddress = new StringBuilder();
                    for (int i = 0; i < addressLineArray.length(); i++) {
                        if (addressLineArray.getString(i) != "null")
                            formattedAddress.append(addressLineArray.getString(i));
                    }
                    detailsResponseItem.formattedAddress = formattedAddress.toString().trim();
                }
                if(addressObject.has("city") && !addressObject.get("city").toString().equals("null")){
                    detailsResponseItem.city = addressObject.getString("city");
                }
                if(addressObject.has("zipcode") && !addressObject.get("zipcode").toString().equals("null")){
                    detailsResponseItem.zipCode = addressObject.getString("zipcode");
                }
                if(addressObject.has("country_code") && !addressObject.get("country_code").toString().equals("null")){
                    detailsResponseItem.countryCode = addressObject.getString("country_code");
                }
            } else {
                detailsResponseItem.formattedAddress = properties.getString("name");
            }
            JSONArray typesArray = properties.getJSONArray("types");
            if (typesArray.length() > 0) {
                String[] types = new String[typesArray.length()];
                for (int i = 0; i < typesArray.length(); i++) {
                    types[i] = typesArray.getString(i);
                }
                detailsResponseItem.types = types;
            } else {
                detailsResponseItem.types = new String[0];
            }

            JSONArray tagsArray = properties.getJSONArray("tags");
            if (tagsArray.length() > 0) {
                String[] tags = new String[tagsArray.length()];
                for (int i = 0; i < tagsArray.length(); i++) {
                    tags[i] = tagsArray.getString(i);
                }
                detailsResponseItem.tags = tags;
            } else {
                detailsResponseItem.tags = new String[0];
            }
            detailsResponseItem.item = jsonObject;
            detailsResponseItem.name = properties.getString("name");

            if (jsonObject.has("geometry")) {
                Geometry geometryDetail = new Geometry();

                geometryDetail.setLocation(new Location(
                        jsonObject.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1),
                        jsonObject.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0)
                ));

                detailsResponseItem.geometry = geometryDetail;
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            return null;
        }
        return detailsResponseItem;
    }


    /***
     * Creates and returns <code>userPropertiesFiltered</code> object from the user_properties in the response Search API
     * @param jsonObject RAW json response returned by <code>store</code> API
     * @return <code>userPropertiesFiltered</code> object
     */
    public static HashMap<String, Object> getUserProperties(JSONObject jsonObject, String storeId) {
        HashMap<String, Object> userPropertiesFiltered = new HashMap<String, Object>();
        SearchAPIResponseItemCore detailsResponseItem = new SearchAPIResponseItemCore();
        JSONObject properties;

        try {
            JSONArray features = jsonObject.getJSONArray("features");
            if (features.length() > 0) {
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    properties = feature.getJSONObject("properties");
                    if (properties.getString("store_id").equals(storeId)) {
                        if (properties.has("user_properties")) {
                            detailsResponseItem.userProperties = new Gson().fromJson(properties.get("user_properties").toString(), HashMap.class);
                            if (WoosmapSettingsCore.userPropertiesFilter.isEmpty()) {
                                userPropertiesFiltered = detailsResponseItem.userProperties;
                            } else {
                                for (String key : WoosmapSettingsCore.userPropertiesFilter) {
                                    if (detailsResponseItem.userProperties != null && detailsResponseItem.userProperties.get(key) != null) {
                                        userPropertiesFiltered.put(key, detailsResponseItem.userProperties.get(key));
                                    } else {
                                        userPropertiesFiltered.put(key, "null");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return userPropertiesFiltered;
    }

}
