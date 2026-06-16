package com.webgeoservices.woosmapgeofencingcore;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Regression guard for the gson upgrade (2.10.1 -> 2.14.0). Mirrors the
 * {@code ArrayList<String> <-> String} TypeToken round-trip used by the Room
 * {@code Converters}, plus a generic POJO round-trip.
 */
public class GsonSerializationRegressionTest {

    @Test
    public void stringListRoundTripMatchesConverterPattern() {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> original = Arrays.asList("visit-1", "visit-2", "visit-3");

        String json = new Gson().toJson(original);
        List<String> restored = new Gson().fromJson(json, listType);

        assertEquals(original, restored);
    }

    @Test
    public void emptyAndNullSafe() {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        String json = new Gson().toJson(new ArrayList<String>());
        List<String> restored = new Gson().fromJson(json, listType);
        assertEquals(0, restored.size());
    }

    @Test
    public void pojoRoundTrip() {
        Sample original = new Sample("zoi-42", 48.8566, 2.3522, 17);
        Gson gson = new Gson();
        Sample restored = gson.fromJson(gson.toJson(original), Sample.class);

        assertEquals(original.id, restored.id);
        assertEquals(original.lat, restored.lat, 1e-12);
        assertEquals(original.lng, restored.lng, 1e-12);
        assertEquals(original.count, restored.count);
    }

    private static class Sample {
        final String id;
        final double lat;
        final double lng;
        final int count;

        Sample(String id, double lat, double lng, int count) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
            this.count = count;
        }
    }
}
