package com.webgeoservices.woosmapgeofencingcore;

import android.content.Context;
import android.util.ArrayMap;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;

import javax.annotation.Nullable;

public class APIHelperCore {
    private Context context;
    private static APIHelperCore _instance;

    protected APIHelperCore(Context context){
        this.context = context;
    }

    protected static APIHelperCore getInstance(Context context){
        if (_instance == null){
            _instance = new APIHelperCore(context);
        }
        return _instance;
    }

    protected StringRequest createGetReuqest(String url,
                                             Response.Listener<String> listener,
                                             @Nullable Response.ErrorListener errorListener){
        StringRequest stringRequest;
        stringRequest = new StringRequest(Request.Method.GET, url,listener, errorListener){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new ArrayMap<>();
                headers.put("X-Api-Key", WoosmapSettingsCore.privateKeyWoosmapAPI);
                headers.put("X-Android-Identifier", context.getPackageName());
                headers.put("X-SDK-Source", "geofence-sdk");
                headers.put("X-AK-SDK-Platform", "Android");
                headers.put("X-AK-SDK-Version", WoosmapSettingsCore.getGeofencingSDKVersion());
                return headers;
            }
        };
        return stringRequest;
    }
}
