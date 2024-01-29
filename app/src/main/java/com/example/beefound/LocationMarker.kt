package com.example.beefound

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.Log
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

//Todo: google position marker
class LocationMarker: Marker {

    private var initialized = false
    private var map: MapView
    private var accuracy: Int? = 80
    private var direction: Float? = null
    private var mapController: IMapController
    private var icon_center: Drawable

    constructor(map: MapView, icon: Drawable, accuracy: Int?, mapController: IMapController) : super(map){
        this.map = map
        this.icon_center = icon
        this.icon = icon_center
        this.accuracy = accuracy
        this.setAnchor(ANCHOR_CENTER, ANCHOR_CENTER)
        this.mapController = mapController
    }

    fun setLocation(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        if (!initialized) {
            map.overlays?.add(this)
            this.setOnMarkerClickListener { _, _ -> true }
            this.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker?, mapView: MapView?): Boolean {
                    mapController.setZoom(17.0)
                    mapController.setCenter(GeoPoint(lat, lon))
                    return true
                }
            })
            initialized = true
        }
        this.position = GeoPoint(lat, lon)
    }

    fun setAccuracy(accuracy: Int) {
        Log.d(
            "Location","update accuracy: $accuracy"
        )
        if (direction == null) return
        this.accuracy = accuracy
        this.icon = icon_center
    }

    fun setDirection(r: Float) {
        Log.d(
            "Location","update direction: $r"
        )
        this.rotation = -r
        map.invalidate()
    }
}

