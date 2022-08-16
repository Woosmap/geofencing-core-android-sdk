package com.webgeoservices.woosmapgeofencingcore

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.util.Log
import android.util.Pair
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.gson.Gson
import com.webgeoservices.woosmapgeofencingcore.DistanceAPIDataModel.DistanceAPI
import com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel.SearchAPIResponseItemCore
import com.webgeoservices.woosmapgeofencingcore.database.*
import org.json.JSONObject
import java.util.*

open class PositionsManagerCore(context: Context, db: WoosmapDb, woosmapProvider: WoosmapProvider) {
    private val context = context
    private val db: WoosmapDb = db
    private val woosmapProvider: WoosmapProvider = woosmapProvider
    protected var temporaryCurrentVisits: MutableList<Visit> = mutableListOf<Visit>()
    protected var temporaryFinishedVisits: MutableList<Visit> = mutableListOf<Visit>()
    protected var requestQueue: RequestQueue? = null

    protected open fun visitsDetectionAlgo(lastVisit: Visit, location: Location) {
        Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "get New Location")
        val lastVisitLocation = Location("Woosmap")
        lastVisitLocation.latitude = lastVisit.lat
        lastVisitLocation.longitude = lastVisit.lng

        if (location.time < lastVisit.startTime) {
            return
        }

        // Visit is active
        if (lastVisit.endTime.compareTo(0) == 0) {
            val distance = lastVisitLocation.distanceTo(location)
            val accuracy = location.accuracy
            // Test if we are still in visit
            if (distance <= accuracy * 2) {
                //if new position accuracy is better than the visit, we do an Update
                if (lastVisit.accuracy >= location.accuracy) {
                    lastVisit.lat = location.latitude
                    lastVisit.lng = location.longitude
                    lastVisit.accuracy = location.accuracy
                }
                lastVisit.nbPoint += 1
                this.db.visitsDao.updateStaticPosition(lastVisit)
                Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "Always Static")
            }
            //Visit out
            else {
                //Close the current visit
                lastVisit.endTime = location.time
                this.finishVisit(lastVisit)
                Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "Not static Anyway")
            }
        }
        //not visit in progress
        else {
            val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
            var distance = 0.0F;
            if (previousMovingPosition != null) {
                distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
            }
            Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "distance : $distance")
            if (distance >= WoosmapSettingsCore.distanceDetectionThresholdVisits) {
                Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "We're Moving")
            } else { //Create a new visit
                val olderPosition = this.db.movingPositionsDao.previousLastMovingPosition
                if (olderPosition != null) {
                    val distanceVisit =
                        this.distanceBetweenLocationAndPosition(olderPosition, location)
                    if (distanceVisit <= WoosmapSettingsCore.distanceDetectionThresholdVisits) {
                        // less than distance of dectection visit of before last position, they are a visit
                        val visit = Visit()
                        visit.uuid = UUID.randomUUID().toString()
                        visit.lat = previousMovingPosition.lat
                        visit.lng = previousMovingPosition.lng
                        visit.accuracy = previousMovingPosition.accuracy
                        visit.startTime = previousMovingPosition.dateTime
                        visit.endTime = 0
                        visit.nbPoint = 1
                        this.createVisit(visit)
                        Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "Create new Visit")
                    }
                }
            }
        }
    }

    open fun addPositionFromLocation(location: Location) {
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition
            ?: this.createMovingPositionFromLocation(location)
        val distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
        Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "distance : " + distance.toString())
        if (distance >= WoosmapSettingsCore.currentLocationDistanceFilter) {
            this.createMovingPositionFromLocation(location)
        }
    }

    open fun createMovingPositionFromLocation(location: Location): MovingPosition {
        val distance = getDistanceBetweenLastMovingPositionAndLocation(location)
        val movingPosition = saveAndCreateMovingPositionForLocation(location)
        if (distance > 0.0) {
            if (WoosmapSettingsCore.checkIfPositionIsInsideGeofencingRegionsEnable) {
                checkIfPositionIsInsideGeofencingRegions(movingPosition)
            }

        }

        return movingPosition
    }

    protected fun saveAndCreateMovingPositionForLocation(location: Location): MovingPosition {
        val movingPosition = MovingPosition()
        movingPosition.lat = location.latitude
        movingPosition.lng = location.longitude
        movingPosition.accuracy = location.accuracy
        movingPosition.dateTime = location.time
        movingPosition.isUpload = 0

        val id = this.db.movingPositionsDao.createMovingPosition(movingPosition)
        movingPosition.id = id.toInt()
        return movingPosition
    }

    protected fun getDistanceBetweenLastMovingPositionAndLocation(location: Location): Float {
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition ?: null
        var distance = 1.0F
        if (previousMovingPosition != null) {
            distance = this.distanceBetweenLocationAndPosition(previousMovingPosition, location)
        }
        return distance
    }

    protected open fun checkIfPositionIsInsideGeofencingRegions(movingPosition: MovingPosition) {

        val regions = this.db.regionsDAO.getRegionCircle()

        regions.forEach {
            val regionCenter = Location("woosmap")
            regionCenter.latitude = it.lat
            regionCenter.longitude = it.lng

            val locationPosition = Location("woosmap")
            locationPosition.latitude = movingPosition.lat
            locationPosition.longitude = movingPosition.lng

            val isInside = locationPosition.distanceTo(regionCenter) < it.radius

            if (isInside != it.isCurrentPositionInside) {
                it.isCurrentPositionInside = isInside
                it.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(it)

                val regionLog = saveReionlogInDataBase(it, isInside)
                if (WoosmapSettingsCore.modeHighFrequencyLocation || it.didEnter != isInside) {
                    if (woosmapProvider.regionLogReadyListener != null) {
                        woosmapProvider.regionLogReadyListener.RegionLogReadyCallback(regionLog)
                    }
                }
            }
        }

    }

    protected fun saveReionlogInDataBase(region: Region, isInside: Boolean): RegionLog {
        val regionLog = RegionLog()
        regionLog.identifier = region.identifier
        regionLog.dateTime = region.dateTime
        regionLog.didEnter = region.didEnter
        regionLog.lat = region.lat
        regionLog.lng = region.lng
        regionLog.idStore = region.idStore
        regionLog.radius = region.radius
        regionLog.isCurrentPositionInside = isInside
        this.db.regionLogsDAO.createRegionLog(regionLog)
        return regionLog;
    }

    fun distanceBetweenLocationAndPosition(position: MovingPosition, location: Location): Float {
        val locationFromPosition = Location("woosmap")
        locationFromPosition.latitude = position.lat
        locationFromPosition.longitude = position.lng
        return location.distanceTo(locationFromPosition)
    }

    fun distanceBetweenTwoPositions(positionA: MovingPosition, positionB: MovingPosition): Float {
        val locationFromPositionA = Location("woosmap")
        locationFromPositionA.latitude = positionA.lat
        locationFromPositionA.longitude = positionA.lng
        val locationFromPositionB = Location("woosmap")
        locationFromPositionB.latitude = positionB.lat
        locationFromPositionB.longitude = positionB.lng
        return locationFromPositionA.distanceTo(locationFromPositionB)
    }

    private fun timeBetweenLocationAndPosition(position: MovingPosition, location: Location): Long {
        return (location.time - position.dateTime) / 1000
    }

    open fun manageLocation(location: Location) {
        Log.d(WoosmapSettingsCore.WoosmapVisitsTag, location.toString())
        storeVisitData(location)
        addPositionFromLocation(location)
    }

    open fun storeVisitData(location: Location) {
        //Filter on the accuracy of the location
        if (filterAccurary(location))
            return
        //Filter Time between the last Location and the current Location
        if (filterTimeLocation(location))
            return

        if (WoosmapSettingsCore.visitEnable) {
            val lastVisit = this.db.visitsDao.lastStaticPosition
            if (lastVisit != null) {
                this.visitsDetectionAlgo(lastVisit, location)
            } else {
                Log.d(WoosmapSettingsCore.WoosmapVisitsTag, "Empty")
                val staticLocation = Visit()
                staticLocation.uuid = UUID.randomUUID().toString()
                staticLocation.lat = location.latitude
                staticLocation.lng = location.longitude
                staticLocation.accuracy = location.accuracy
                staticLocation.startTime = location.time
                staticLocation.endTime = 0
                staticLocation.nbPoint = 0
                this.createVisit(staticLocation)
            }
        }
    }

    protected fun filterAccurary(location: Location): Boolean {
        // No parameter, No filter
        if (WoosmapSettingsCore.accuracyFilter == 0)
            return false
        if (location.accuracy > WoosmapSettingsCore.accuracyFilter)
            return true
        return false
    }

    open fun asyncManageLocation(locations: List<Location>) {
        Thread {
            try {
                temporaryCurrentVisits = mutableListOf<Visit>()
                temporaryFinishedVisits = mutableListOf<Visit>()
                for (location in locations.sortedBy { l -> l.time }) {
                    manageLocation(location)
                }

                detectVisitInZOIClassified()

            } catch (e: Exception) {
                Log.e(WoosmapSettingsCore.WoosmapVisitsTag, e.toString())
            }
        }.start()
    }

    protected open fun detectVisitInZOIClassified() {
        if (temporaryCurrentVisits.isEmpty() && temporaryFinishedVisits.isEmpty()) {
            return
        }

        val ZOIsClassified = this.db.zoIsDAO.getWorkHomeZOI()

        val lastVisitLocation = Location("lastVisit")
        var didEnter = false

        if (!temporaryCurrentVisits.isEmpty()) {
            lastVisitLocation.latitude = temporaryCurrentVisits.first().lat
            lastVisitLocation.longitude = temporaryCurrentVisits.first().lng
            didEnter = true
        }

        if (!temporaryFinishedVisits.isEmpty()) {
            lastVisitLocation.latitude = temporaryFinishedVisits.first().lat
            lastVisitLocation.longitude = temporaryFinishedVisits.first().lng
            didEnter = false
        }

        for (zoi in ZOIsClassified) {
            val zoiCenterLocation = Location("zoiCenter")
            zoiCenterLocation.latitude = SphericalMercator.y2lat(zoi.lngMean)
            zoiCenterLocation.longitude = SphericalMercator.x2lon(zoi.latMean)
            val distance = zoiCenterLocation.distanceTo(lastVisitLocation)
            if (distance < WoosmapSettingsCore.radiusDetectionClassifiedZOI) {
                var regionLog = RegionLog()
                regionLog.identifier = zoi.period
                regionLog.dateTime = System.currentTimeMillis()
                regionLog.didEnter = didEnter
                regionLog.lat = lastVisitLocation.latitude
                regionLog.lng = lastVisitLocation.longitude
                regionLog.radius = WoosmapSettingsCore.radiusDetectionClassifiedZOI.toDouble()
                this.db.regionLogsDAO.createRegionLog(regionLog)

                if (woosmapProvider.regionLogReadyListener != null) {
                    woosmapProvider.regionLogReadyListener.RegionLogReadyCallback(regionLog)
                }

            }
        }

    }

    fun getStoreAPIUrl(lat: Double, lng: Double): String? {
        var url = String.format(
            WoosmapSettingsCore.SearchAPIUrl,
            WoosmapSettingsCore.WoosmapURL,
            WoosmapSettingsCore.privateKeyWoosmapAPI,
            lat,
            lng
        )
        if (!WoosmapSettingsCore.searchAPIParameters.isEmpty()) {
            val stringBuilder: StringBuilder = StringBuilder(url)

            for ((key, value) in WoosmapSettingsCore.searchAPIParameters) {
                if (key == "stores_by_page") {
                    if (Integer.parseInt(value) >= 20) {
                        stringBuilder.append("&" + key + "=" + "20")
                    } else {
                        stringBuilder.append("&" + key + "=" + value)
                    }
                } else {
                    stringBuilder.append("&" + key + "=" + value)
                }
            }
            url = stringBuilder.toString()
        }
        return url
    }

    open fun requestDistanceAPI(POIaround: POI, positon: MovingPosition) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if (WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            return
        }

        val destination = POIaround.lat.toString() + "," + POIaround.lng.toString()

        val url = String.format(
            WoosmapSettingsCore.DistanceAPIUrl,
            WoosmapSettingsCore.WoosmapURL,
            WoosmapSettingsCore.distanceMode,
            WoosmapSettingsCore.getDistanceUnits(),
            WoosmapSettingsCore.getDistanceLanguage(),
            positon.lat,
            positon.lng,
            destination,
            WoosmapSettingsCore.privateKeyWoosmapAPI
        )
        val req = StringRequest(
            Request.Method.GET, url,
            { response ->
                Thread {
                    val gson = Gson()
                    val data = gson.fromJson(response, DistanceAPI::class.java)
                    val status = data.status

                    if (status == "OK") {
                        if (data.rows.get(0).elements.get(0).status == "OK") {
                            POIaround.travelingDistance =
                                data.rows.get(0).elements.get(0).distance.text
                            POIaround.duration = data.rows[0].elements[0].duration.text
                        }
                    }

                    this.db.poIsDAO.createPOI(POIaround)
                    if (woosmapProvider.searchAPIReadyListener != null) {
                        woosmapProvider.searchAPIReadyListener.SearchAPIReadyCallback(POIaround)
                    }
                    requestDistanceApiResponseListener?.requestDistanceApiData(data, POIaround)
                }.start()
            },
            { error ->
                Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " Distance API")
            })
        requestQueue?.add(req)
    }

    interface RequestDistanceApiResponseListener {
        fun requestDistanceApiData(data: DistanceAPI, POIaround: POI)
    }

    private var requestDistanceApiResponseListener: RequestDistanceApiResponseListener? = null
    public fun setRequestDistanceApiResponseListener(requestDistanceApiResponseListener: RequestDistanceApiResponseListener) {
        this.requestDistanceApiResponseListener = requestDistanceApiResponseListener
    }

    open fun searchAPI(lat: Double, lng: Double, positionId: Int = 0) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if (WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            return
        }

        val url = getStoreAPIUrl(lat, lng)
        val req = StringRequest(
            Request.Method.GET, url,
            { response ->
                Thread {
                    assert(response != null)
                    val jsonObject = JSONObject(response.toString())
                    if (!jsonObject.has("error_message")) {
                        val features = jsonObject.getJSONArray("features")
                        if (features.length() > 0) {
                            for (i in 0 until features.length()) {
                                val searchAPIResponseItemCore = SearchAPIResponseItemCore.fromJSON(
                                    jsonObject.getJSONArray("features").getJSONObject(i)
                                )
                                if(searchAPIResponseItemCore != null) {
                                    val POIaround = POI()
                                    POIaround.city = searchAPIResponseItemCore.city
                                    POIaround.zipCode = searchAPIResponseItemCore.zipCode
                                    POIaround.dateTime = System.currentTimeMillis()
                                    POIaround.distance = searchAPIResponseItemCore.distance
                                    POIaround.locationId = positionId
                                    POIaround.idStore = searchAPIResponseItemCore.idstore
                                    POIaround.name = searchAPIResponseItemCore.name
                                    POIaround.lat = searchAPIResponseItemCore.geometry.location.lat
                                    POIaround.lng = searchAPIResponseItemCore.geometry.location.lng
                                    POIaround.address = searchAPIResponseItemCore.formattedAddress
                                    POIaround.contact = searchAPIResponseItemCore.contact
                                    POIaround.types =
                                        searchAPIResponseItemCore.types.joinToString(" - ")
                                    POIaround.tags = searchAPIResponseItemCore.tags.joinToString(" - ")
                                    POIaround.countryCode = searchAPIResponseItemCore.countryCode
                                    POIaround.data = response

                                    this.db.poIsDAO.createPOI(POIaround)
                                    if (woosmapProvider.searchAPIReadyListener != null) {
                                        woosmapProvider.searchAPIReadyListener.SearchAPIReadyCallback(
                                            POIaround
                                        )
                                    }

                                }
                            }
                        }
                    }
                }.start()
            },
            { error ->
                Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " search API")
            })
        requestQueue?.add(req)
    }

    /*interface SearchApiResponseListener {
        fun searchApiData(searchApiData: SearchAPIResponseItemCore, POIaround: POI)
    }

    private var searchApiResponseListener: SearchApiResponseListener? = null
    public fun setSearchApiResponseListener(searchApiResponseListener: SearchApiResponseListener) {
        this.searchApiResponseListener = searchApiResponseListener
    }*/

    fun calculateDistance(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>
    ) {
        calculateDistance(latOrigin, lngOrigin, listPosition, 0)
    }

    fun calculateDistance(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        locationId: Int = 0
    ) {
        if (WoosmapSettingsCore.distanceProvider == WoosmapSettingsCore.woosmapDistance) {
            distanceAPI(latOrigin, lngOrigin, listPosition, locationId)
        } else {
            trafficDistanceAPI(
                latOrigin,
                lngOrigin,
                listPosition,
                locationId,
            )
        }
    }

    fun calculateDistance(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        parameters: Map<String, String>,
        locationId: Int = 0
    ) {
        var provider = WoosmapSettingsCore.distanceProvider
        if (parameters.containsKey("distanceProvider")) {
            provider = parameters["distanceProvider"]
        }

        if (provider == WoosmapSettingsCore.woosmapDistance) {
            distanceAPI(latOrigin, lngOrigin, listPosition, locationId, parameters)
        } else {
            trafficDistanceAPI(
                latOrigin,
                lngOrigin,
                listPosition,
                locationId,
                parameters
            )
        }
    }


    fun distanceAPI(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        locationId: Int = 0,
        parameters: Map<String, String> = emptyMap(),
    ) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if (WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            return
        }

        var destination = ""
        listPosition.forEach {
            destination += it.first.toString() + "," + it.second.toString() + "|"
        }

        var url: String
        var mode = WoosmapSettingsCore.distanceMode
        var units = WoosmapSettingsCore.distanceUnits
        var language = WoosmapSettingsCore.distanceLanguage
        if (parameters.isEmpty()) {
            url = String.format(
                WoosmapSettingsCore.DistanceAPIUrl,
                WoosmapSettingsCore.WoosmapURL,
                WoosmapSettingsCore.distanceMode,
                WoosmapSettingsCore.getDistanceUnits(),
                WoosmapSettingsCore.getDistanceLanguage(),
                latOrigin,
                lngOrigin,
                destination,
                WoosmapSettingsCore.privateKeyWoosmapAPI
            )
        } else {
            if (parameters.containsKey("distanceMode")) {
                mode = parameters["distanceMode"]
            }
            if (parameters.containsKey("distanceUnits")) {
                units = parameters["distanceUnits"]
            }
            if (parameters.containsKey("distanceLanguage")) {
                language = parameters["distanceLanguage"]
            }
            url = String.format(
                WoosmapSettingsCore.DistanceAPIUrl,
                WoosmapSettingsCore.WoosmapURL,
                mode,
                units,
                language,
                latOrigin,
                lngOrigin,
                destination,
                WoosmapSettingsCore.privateKeyWoosmapAPI
            )
        }
        val req = StringRequest(
            Request.Method.GET, url,
            { response ->
                Thread {
                    val gson = Gson()
                    val data = gson.fromJson(response, DistanceAPI::class.java)
                    val status = data.status
                    if (status.contains("OK")) {
                        var distancesList: MutableList<Distance> = mutableListOf<Distance>()
                        for (row in data.rows) {
                            for (element in row.elements) {
                                if (element.status.contains("OK")) {
                                    val distance = Distance()
                                    distance.locationId = locationId
                                    distance.dateTime = System.currentTimeMillis()
                                    distance.originLatitude = latOrigin
                                    distance.originLongitude = lngOrigin
                                    distance.distance = element.distance.value
                                    distance.distanceText = element.distance.text
                                    distance.duration = element.duration.value
                                    distance.durationText = element.duration.text
                                    distance.routing = WoosmapSettingsCore.trafficDistanceRouting
                                    distance.mode = mode
                                    distance.units = units
                                    distance.language = language
                                    val dest = listPosition.get(row.elements.indexOf(element))
                                    distance.destinationLatitude = dest.first
                                    distance.destinationLongitude = dest.second
                                    this.db.distanceDAO.createDistance(distance)
                                    distancesList.add(distance)
                                }
                            }
                        }
                        if (woosmapProvider.distanceReadyListener != null) {
                            woosmapProvider.distanceReadyListener.DistanceReadyCallback(
                                distancesList.toTypedArray()
                            )
                        }
                        distanceApiResponseListener?.distanceApiData(data, distancesList)

                    } else {
                        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Distance API " + status)
                    }
                    if (locationId != 0 && status.contains("OK") && data.rows.get(0).elements.get(0).status.contains(
                            "OK"
                        )
                    ) {
                        var poiToUpdate = this.db.poIsDAO.getPOIbyLocationID(locationId)
                        if (poiToUpdate != null) {
                            poiToUpdate.travelingDistance =
                                data.rows.get(0).elements.get(0).distance.text
                            poiToUpdate.duration = data.rows.get(0).elements.get(0).duration.text
                            this.db.poIsDAO.updatePOI(poiToUpdate)
                        }
                    }

                }.start()
            },
            { error ->
                Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " Distance API")
            })
        requestQueue?.add(req)
    }

    interface DistanceApiResponseListener {
        fun distanceApiData(data: DistanceAPI, distanceList: MutableList<Distance>)
    }

    private var distanceApiResponseListener: DistanceApiResponseListener? = null
    public fun setDistanceApiResponseListener(distanceApiResponseListener: DistanceApiResponseListener) {
        this.distanceApiResponseListener = distanceApiResponseListener
    }

    protected fun generateTrafficDistanceAPIURL(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        locationId: Int = 0,
        parameters: Map<String, String> = emptyMap()
    ): String {
        var destination = ""
        listPosition.forEach {
            destination += it.first.toString() + "," + it.second.toString()
            if (listPosition.last() != it) {
                destination += "|"
            }
        }
        val url: String
        var mode = WoosmapSettingsCore.distanceMode
        var units = WoosmapSettingsCore.distanceUnits
        var language = WoosmapSettingsCore.distanceLanguage
        var routing = WoosmapSettingsCore.trafficDistanceRouting
        if (parameters.isEmpty()) {
            url = String.format(
                WoosmapSettingsCore.TrafficDistanceAPIUrl,
                WoosmapSettingsCore.WoosmapURL,
                WoosmapSettingsCore.distanceMode,
                WoosmapSettingsCore.getDistanceUnits(),
                WoosmapSettingsCore.getDistanceLanguage(),
                WoosmapSettingsCore.trafficDistanceRouting,
                latOrigin,
                lngOrigin,
                destination,
                WoosmapSettingsCore.privateKeyWoosmapAPI
            )
        } else {
            if (parameters.containsKey("distanceMode")) {
                mode = parameters["distanceMode"]
            }
            if (parameters.containsKey("distanceUnits")) {
                units = parameters["distanceUnits"]
            }
            if (parameters.containsKey("distanceLanguage")) {
                language = parameters["distanceLanguage"]
            }
            if (parameters.containsKey("trafficDistanceRouting")) {
                routing = parameters["trafficDistanceRouting"]
            }
            url = String.format(
                WoosmapSettingsCore.TrafficDistanceAPIUrl,
                WoosmapSettingsCore.WoosmapURL,
                mode,
                units,
                language,
                routing,
                latOrigin,
                lngOrigin,
                destination,
                WoosmapSettingsCore.privateKeyWoosmapAPI
            )
        }
        return url

    }

    protected fun getDistanceListFromDistanceApiData(
        data: DistanceAPI,
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        locationId: Int = 0
    ): MutableList<Distance> {
        val distancesList: MutableList<Distance> = mutableListOf<Distance>()
        for (row in data.rows) {
            for (element in row.elements) {
                if (element.status.contains("OK")) {
                    val distance = Distance()
                    distance.locationId = locationId
                    distance.dateTime = System.currentTimeMillis()
                    distance.originLatitude = latOrigin
                    distance.originLongitude = lngOrigin
                    distance.distance = element.distance.value
                    distance.distanceText = element.distance.text
                    distance.duration = element.duration_with_traffic.value
                    distance.durationText = element.duration_with_traffic.text
                    distance.routing = WoosmapSettingsCore.trafficDistanceRouting
                    distance.mode = WoosmapSettingsCore.distanceMode
                    distance.units = WoosmapSettingsCore.distanceUnits
                    distance.language = WoosmapSettingsCore.distanceLanguage
                    val dest = listPosition.get(row.elements.indexOf(element))
                    distance.destinationLatitude = dest.first
                    distance.destinationLongitude = dest.second
                    this.db.distanceDAO.createDistance(distance)
                    distancesList.add(distance)
                }
            }
        }
        return distancesList
    }


    fun trafficDistanceAPI(
        latOrigin: Double,
        lngOrigin: Double,
        listPosition: MutableList<Pair<Double, Double>>,
        locationId: Int = 0,
        parameters: Map<String, String> = emptyMap(),
    ) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if (WoosmapSettingsCore.privateKeyWoosmapAPI.isEmpty()) {
            return
        }
        val url = generateTrafficDistanceAPIURL(
            latOrigin,
            lngOrigin,
            listPosition,
            locationId,
            parameters
        )
        val req = StringRequest(
            Request.Method.GET, url,
            { response ->
                Thread {
                    val gson = Gson()
                    val data = gson.fromJson(response, DistanceAPI::class.java)
                    val status = data.status
                    if (status.contains("OK")) {
                        val distancesList = getDistanceListFromDistanceApiData(
                            data,
                            latOrigin,
                            lngOrigin,
                            listPosition,
                            locationId
                        )
                        if (woosmapProvider.distanceReadyListener != null) {
                            woosmapProvider.distanceReadyListener.DistanceReadyCallback(
                                distancesList.toTypedArray()
                            )
                        }
                        trafficApiResponseListener?.trafficApiData(data, distancesList)

                    } else {
                        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Distance API " + status)
                    }
                    updatePoiFromTrafficAPI(locationId, status, data)

                }.start()
            },
            { error ->
                Log.e(WoosmapSettingsCore.WoosmapSdkTag, error.toString() + " Distance API")
            })
        requestQueue?.add(req)
    }


    interface TrafficApiResponseListener {
        fun trafficApiData(data: DistanceAPI, distanceList: MutableList<Distance>)
    }

    private var trafficApiResponseListener: TrafficApiResponseListener? = null
    public fun setTrafficApiResponseListener(trafficApiResponseListener: TrafficApiResponseListener) {
        this.trafficApiResponseListener = trafficApiResponseListener;
    }

    protected fun updatePoiFromTrafficAPI(locationId: Int, status: String, data: DistanceAPI) {
        if (locationId != 0 && status.contains("OK") && data.rows.get(0).elements.get(0).status.contains(
                "OK"
            )
        ) {
            var poiToUpdate = this.db.poIsDAO.getPOIbyLocationID(locationId)
            if (poiToUpdate != null) {
                poiToUpdate.travelingDistance =
                    data.rows.get(0).elements.get(0).distance.text
                poiToUpdate.duration =
                    data.rows.get(0).elements.get(0).duration_with_traffic.text
                this.db.poIsDAO.updatePOI(poiToUpdate)
            }
        }
    }

    protected fun createVisit(visit: Visit) {
        this.db.visitsDao.createStaticPosition(visit)
        temporaryCurrentVisits.add(visit)
    }

    protected open fun finishVisit(visit: Visit) {
        visit.duration = visit.endTime - visit.startTime


        if (visit.duration >= WoosmapSettingsCore.durationVisitFilter) {
            // Refresh zoi on Visit
            val figmmForVisitsCreator = FigmmForVisitsCreator(db)
            figmmForVisitsCreator.figmmForVisit(visit)

            this.db.visitsDao.updateStaticPosition(visit)
            temporaryFinishedVisits.add(visit)
            if (woosmapProvider.visitReadyListener != null) {
                woosmapProvider.visitReadyListener.VisitReadyCallback(visit)
            }

        }


    }

    fun cleanOldPositions() {
        Thread {
            val lastStaticPosition = this.db.visitsDao.lastUploadedStaticPosition
            if (lastStaticPosition != null) {
                this.db.movingPositionsDao.deleteOldPositions(lastStaticPosition.startTime)
            }
            this.db.visitsDao.deleteUploadedStaticPositions()
        }.start()
    }

    fun filterTimeLocation(location: Location): Boolean {
        // No parameter, No filter
        if (WoosmapSettingsCore.currentLocationTimeFilter == 0)
            return false

        // No data in db, No filter
        val previousMovingPosition = this.db.movingPositionsDao.lastMovingPosition ?: return false

        // Check time between last position in db and the current position
        if (timeBetweenLocationAndPosition(
                previousMovingPosition,
                location
            ) > WoosmapSettingsCore.currentLocationTimeFilter
        )
            return false
        return true
    }


    open fun createRegion(
        identifier: String,
        radius: Double,
        lat: Double,
        lng: Double,
        idStore: String,
        type: String = "circle"
    ) {
        var region = Region()
        region.lat = lat
        region.lng = lng
        region.identifier = identifier
        region.idStore = idStore
        region.radius = radius
        region.dateTime = System.currentTimeMillis()
        region.type = type
        Thread {
            this.db.regionsDAO.createRegion(region)
            if (woosmapProvider.regionReadyListener != null) {
                woosmapProvider.regionReadyListener.RegionReadyCallback(region)
            }

        }.start()

    }


    open fun didEventRegion(geofenceIdentifier: String, transition: Int) {
        Thread {
            val regionDetected =
                this.db.regionsDAO.getRegionFromId(geofenceIdentifier) ?: return@Thread

            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                regionDetected.didEnter = true
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                regionDetected.didEnter = false
            }

            var regionLog = RegionLog()
            regionLog.identifier = regionDetected.identifier
            regionLog.dateTime = regionDetected.dateTime
            regionLog.didEnter = regionDetected.didEnter
            regionLog.lat = regionDetected.lat
            regionLog.lng = regionDetected.lng
            regionLog.idStore = regionDetected.idStore
            regionLog.radius = regionDetected.radius
            regionLog.isCurrentPositionInside = regionDetected.isCurrentPositionInside


            if (regionDetected.didEnter != regionDetected.isCurrentPositionInside) {

                regionLog.isCurrentPositionInside = regionDetected.didEnter
                regionDetected.isCurrentPositionInside = regionDetected.didEnter

                this.db.regionLogsDAO.createRegionLog(regionLog)

                regionDetected.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(regionDetected)

                if (woosmapProvider.regionLogReadyListener != null) {
                    woosmapProvider.regionLogReadyListener.RegionLogReadyCallback(regionLog)
                }

            } else {
                this.db.regionLogsDAO.createRegionLog(regionLog)
                regionDetected.dateTime = System.currentTimeMillis()
                this.db.regionsDAO.updateRegion(regionDetected)
            }

        }.start()
    }

    fun removeGeofence(id: String) {
        Thread {
            this.db.regionsDAO.deleteRegionFromId(id)
        }.start()

    }


    @SuppressLint("MissingPermission")
    fun addGeofence(
        geofenceHelper: GeofenceHelper,
        geofencingRequest: GeofencingRequest,
        geofencePendingIntent: PendingIntent,
        geofencingClient: GeofencingClient,
        id: String,
        radius: Float,
        latitude: Double,
        longitude: Double,
        idStore: String
    ) {
        Thread {
            val region = this.db.regionsDAO.getRegionFromId(id)
            if (region != null) {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Region already exist")
            } else {
                createRegion(id, radius.toDouble(), latitude, longitude, idStore)
            }
        }.start()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onSuccess: Geofence Added...")
            }
            addOnFailureListener {
                val errorMessage = geofenceHelper.getErrorString(exception)
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onFailure " + errorMessage)
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun replaceGeofenceCircle(
        oldId: String,
        geofenceHelper: GeofenceHelper,
        geofencingRequest: GeofencingRequest,
        GeofencePendingIntent: PendingIntent,
        mGeofencingClient: GeofencingClient,
        newId: String,
        radius: Float,
        latitude: Double,
        longitude: Double,
    ) {
        var region = Region()
        region.lat = latitude
        region.lng = longitude
        region.identifier = newId
        region.idStore = ""
        region.radius = radius.toDouble()
        region.dateTime = System.currentTimeMillis()
        region.type = "circle"

        Thread {
            val regionOld = this.db.regionsDAO.getRegionFromId(oldId)
            if (regionOld == null) {
                Log.d(
                    WoosmapSettingsCore.WoosmapSdkTag,
                    "Region to replace not exist id = " + oldId
                )
            } else {
                this.db.regionsDAO.deleteRegionFromId(oldId)
            }
            val regionNew = this.db.regionsDAO.getRegionFromId(newId)
            if (regionNew != null) {
                Log.d(WoosmapSettingsCore.WoosmapSdkTag, "Region already exist id = " + newId)
            } else {
                this.db.regionsDAO.createRegion(region)
                mGeofencingClient.addGeofences(geofencingRequest, GeofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onSuccess: Geofence Added...")
                    }
                    addOnFailureListener {
                        val errorMessage = geofenceHelper.getErrorString(exception)
                        Log.d(WoosmapSettingsCore.WoosmapSdkTag, "onFailure " + errorMessage)
                    }
                }
            }
        }.start()
    }

}