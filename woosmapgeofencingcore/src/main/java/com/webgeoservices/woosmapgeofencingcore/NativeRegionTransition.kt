package com.webgeoservices.woosmapgeofencingcore

import com.google.android.gms.location.Geofence
import com.webgeoservices.woosmapgeofencingcore.database.Region
import com.webgeoservices.woosmapgeofencingcore.database.RegionLog

/** Applies a geofence transition reported by Android to a region, without touching the database. */
internal object NativeRegionTransition {

    /**
     * Records the transition on [region] and builds its region log. Both are stamped with [now]:
     * until then the region's `dateTime` is the time of its previous state change (#85).
     *
     * @return the log to store, or `null` when the region was already in the reported state, in
     * which case only `didEnter` may have changed. A transition other than an enter or an exit
     * changes nothing.
     */
    fun applyTransition(region: Region, transition: Int, now: Long): RegionLog? {
        region.didEnter = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return null
        }
        if (region.didEnter == region.isCurrentPositionInside) {
            return null
        }
        region.isCurrentPositionInside = region.didEnter
        region.dateTime = now

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
