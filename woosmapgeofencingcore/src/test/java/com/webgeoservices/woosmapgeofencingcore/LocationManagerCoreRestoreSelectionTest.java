package com.webgeoservices.woosmapgeofencingcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.webgeoservices.woosmapgeofencingcore.database.MovingPosition;
import com.webgeoservices.woosmapgeofencingcore.database.Region;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pins how the geofence-restore path picks the regions it hands to Android (issue #77,
 * problem 3): at most 100, the nearest to the last known position, and never one the device is inside.
 */
public class LocationManagerCoreRestoreSelectionTest {

    /** Last known position: a customer's home in Salinelles. */
    private final MovingPosition home = position(43.81206, 4.0571);

    private static MovingPosition position(double lat, double lng) {
        MovingPosition position = new MovingPosition();
        position.lat = lat;
        position.lng = lng;
        return position;
    }

    private static Region region(String id, double lat, double lng, boolean inside, long touched) {
        Region region = new Region();
        region.identifier = id;
        region.lat = lat;
        region.lng = lng;
        region.radius = 100.0;
        region.isCurrentPositionInside = inside;
        region.dateTime = touched;
        return region;
    }

    /** {@code count} regions north of home, r0 the nearest, each about 111 m further than the previous. */
    private Region[] ring(int count) {
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            regions.add(region("r" + i, home.lat + 0.001 * (i + 1), home.lng, false, 0));
        }
        Collections.shuffle(regions);
        return regions.toArray(new Region[0]);
    }

    private static Set<String> identifiers(List<Region> regions) {
        Set<String> ids = new HashSet<>();
        for (Region region : regions) {
            ids.add(region.identifier);
        }
        return ids;
    }

    @Test
    public void keepsAtMostTheLimit() {
        assertEquals(100, LocationManagerCore.selectRegionsToRegister(ring(120), home, 100).size());
    }

    @Test
    public void keepsTheNearestToTheLastKnownPosition() {
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            expected.add("r" + i);
        }
        assertEquals(expected, identifiers(LocationManagerCore.selectRegionsToRegister(ring(120), home, 100)));
    }

    @Test
    public void noRegionsSelectsNothing() {
        assertTrue(LocationManagerCore.selectRegionsToRegister(new Region[0], home, 100).isEmpty());
    }

    @Test
    public void belowTheLimitEverythingIsKept() {
        assertEquals(7, LocationManagerCore.selectRegionsToRegister(ring(7), home, 100).size());
    }

    @Test
    public void aRegionTheDeviceIsInsideIsKeptEvenWhenFarAway() {
        Region office = region("office", 43.6100609, 3.8919651, true, 0); // 26 km from home
        List<Region> regions = new ArrayList<>(Arrays.asList(ring(120)));
        regions.add(office);
        List<Region> kept = LocationManagerCore.selectRegionsToRegister(regions.toArray(new Region[0]), home, 100);
        assertTrue(identifiers(kept).contains("office"));
    }

    @Test
    public void withoutAKnownPositionTheMostRecentlyTouchedAreKept() {
        Region[] regions = {
                region("old", 0, 0, false, 1_000),
                region("new", 0, 0, false, 3_000),
                region("mid", 0, 0, false, 2_000)
        };
        List<Region> kept = LocationManagerCore.selectRegionsToRegister(regions, null, 2);
        assertEquals(Arrays.asList("new", "mid"), Arrays.asList(kept.get(0).identifier, kept.get(1).identifier));
    }

    @Test
    public void distanceMatchesTheCustomerFixFiveMetresFromHome() {
        // moving_positions row 460 of the enterprise #179 export, 5.3 m from the home centre.
        assertEquals(5.3, LocationManagerCore.distanceMeters(43.8120278, 4.0570513, home.lat, home.lng), 0.1);
    }
}
