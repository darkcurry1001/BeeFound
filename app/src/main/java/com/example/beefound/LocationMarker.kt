package com.example.beefound

import android.graphics.drawable.Drawable
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint


//Todo: google position marker
class LocationMarker: Marker {

    private val initialized = false
    private var map: MapView

    constructor(map: MapView, icon: Drawable) : super(map){
        this.map = map
        this.icon = icon
    }

    fun setLocation(lat: Double?, lon: Double?){
        if (lat == null || lon == null) return
        if (!initialized){
            map.overlays?.add(this)
            this.setOnMarkerClickListener { _, _ -> true }
        }
        this.position = GeoPoint(lat, lon)
    }


}