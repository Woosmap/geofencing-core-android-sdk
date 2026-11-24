package com.webgeoservices.woosmapgeofencingcore

import com.google.android.gms.location.Geofence
import com.webgeoservices.woosmapgeofencingcore.database.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #85: the region log of a geofence transition reported by Android carries the time the transition
 * was handled, and a transition that changes nothing writes no log.
 */
class NativeRegionTransitionTest {

    private val enteredAt = 1_000L
    private val now = 365_000L

    private fun region(inside: Boolean) = Region().apply {
        identifier = "home"
        idStore = "store-1"
        lat = 48.8566
        lng = 2.3522
        radius = 100.0
        isCurrentPositionInside = inside
        didEnter = inside
        dateTime = enteredAt
    }

    @Test fun anExitIsStampedWithTheTimeItWasHandledNotTheTimeOfTheEnter() {
        val log = NativeRegionTransition.regionLogFor(region(inside = true), Geofence.GEOFENCE_TRANSITION_EXIT, now)

        assertEquals(now, log!!.dateTime)
    }

    @Test fun anEnterIsStampedWithTheTimeItWasHandledNotTheTimeTheRegionWasCreated() {
        val log = NativeRegionTransition.regionLogFor(region(inside = false), Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertEquals(now, log!!.dateTime)
    }

    @Test fun anExitFromARegionTheDeviceIsOutsideWritesNoLog() {
        val log = NativeRegionTransition.regionLogFor(region(inside = false), Geofence.GEOFENCE_TRANSITION_EXIT, now)

        assertNull(log)
    }

    @Test fun anEnterIntoARegionTheDeviceIsInsideWritesNoLog() {
        val log = NativeRegionTransition.regionLogFor(region(inside = true), Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertNull(log)
    }

    @Test fun anEnterIsLoggedAsAnEnteredEvent() {
        val log = NativeRegionTransition.regionLogFor(region(inside = false), Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertEquals("woos_geofence_entered_event", log!!.eventName)
    }

    @Test fun anExitIsLoggedAsAnExitedEvent() {
        val log = NativeRegionTransition.regionLogFor(region(inside = true), Geofence.GEOFENCE_TRANSITION_EXIT, now)

        assertEquals("woos_geofence_exited_event", log!!.eventName)
    }

    @Test fun anEnterMarksTheRegionAsInside() {
        val region = region(inside = false)

        NativeRegionTransition.regionLogFor(region, Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertTrue(region.isCurrentPositionInside)
    }

    @Test fun anExitMarksTheRegionAsOutside() {
        val region = region(inside = true)

        NativeRegionTransition.regionLogFor(region, Geofence.GEOFENCE_TRANSITION_EXIT, now)

        assertFalse(region.isCurrentPositionInside)
    }

    // The SDK's own position check may have seen the crossing first; Android's report still counts.
    @Test fun anEnterTheSdkAlreadySawIsStillRecordedAsAnnouncedByAndroid() {
        val region = region(inside = true).apply { didEnter = false }

        NativeRegionTransition.regionLogFor(region, Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertTrue(region.didEnter)
    }

    @Test fun theLogDescribesTheRegion() {
        val log = NativeRegionTransition.regionLogFor(region(inside = false), Geofence.GEOFENCE_TRANSITION_ENTER, now)

        assertEquals("home", log!!.identifier)
    }
}
