package com.webgeoservices.woosmapgeofencingcore

import com.google.android.gms.location.Geofence
import com.webgeoservices.woosmapgeofencingcore.database.Region
import com.webgeoservices.woosmapgeofencingcore.database.RegionLog

/** Applies a geofence transition reported by Android to a region, without touching the database. */
internal object NativeRegionTransition {

    /**
     * Records the transition on [region] and builds its region log, stamped with [now]: the
     * region's own `dateTime` is the time of its previous state change (#85).
     *
     * @return the log to store, or `null` when the region was already in the reported state, in
     * which case only `didEnter` may have changed
     */
    fun regionLogFor(region: Region, transition: Int, now: Long): RegionLog? {
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            region.didEnter = true
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            region.didEnter = false
        }
        if (region.didEnter == region.isCurrentPositionInside) {
            return null
        }
        region.isCurrentPositionInside = region.didEnter

        val regionLog = RegionLog()
        regionLog.identifier = region.identifier
        regionLog.dateTime = now
        regionLog.didEnter = region.didEnter
        regionLog.lat = region.lat
        regionLog.lng = region.lng
        regionLog.idStore = region.idStore
        regionLog.radius = region.radius
        regionLog.isCurrentPositionInside = region.didEnter
        regionLog.eventName =
            if (region.didEnter) "woos_geofence_entered_event" else "woos_geofence_exited_event"
        return regionLog
    }
}
