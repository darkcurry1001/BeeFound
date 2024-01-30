package com.example.beefound

import android.graphics.Color
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import android.util.Log
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


//Todo: google position marker
class LocationMarker: Marker {

    private var initialized = false
    private var map: MapView
    private var accuracy: Int = 90
    private var direction: Float? = null
    private var mapController: IMapController
    private var icon_center: Drawable

    constructor(map: MapView, icon: Drawable, accuracy: Int?, mapController: IMapController) : super(map){
        this.map = map
        this.icon_center = icon
        this.icon = icon_center
        this.accuracy = accuracy?:this.accuracy
        this.setAnchor(ANCHOR_CENTER, ANCHOR_CENTER)
        setAccuracy(this.accuracy)
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
            "flashLight","update accuracy: $accuracy"
        )
        //if (direction == null) return
        this.accuracy = accuracy
        this.icon = icon_center

        var acc: Float = (5-accuracy) * PI.toFloat() / 8
        if (acc > 1.5707963267f) acc = 1.5707963267f
        else if (acc < 1.5707963267f/4) acc = 1.5707963267f/4
        Log.d( "flashLight","update accuracy: ${cos(acc)}")
        var l = 2f
        val conePath = Path()
        conePath.moveTo(0.5f, 0.5f) // Starting point of the triangle
        conePath.lineTo(-sin(acc/2)*l+0.5f, -cos(acc/2)*l+0.5f)
        var grad = 180f/ PI.toFloat()*acc
        conePath.arcTo(-l+0.5f, l+0.5f, l+0.5f, -l+0.5f, 270f-grad/2, grad, false)
        conePath.lineTo(sin(acc/2)*l+0.5f, -cos(acc/2)*l+0.5f)
        conePath.lineTo(0f+0.5f, 0f+0.5f)

        // Create a new ShapeDrawable with the conePath to represent the cone
        val coneDrawable = ShapeDrawable(PathShape(conePath, 1f, 1f))


        // Set a RadialGradient as the Shader for the coneDrawable
        // The RadialGradient starts with a solid color at the center and fades out to transparent at the edges
        // The radius of the gradient corresponds to the accuracy of the sensor
        coneDrawable.paint.shader = RadialGradient(
            0.5f, 0.5f, l+0.5f,
            Color.argb(169,42,42,242), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )

        // Create a new LayerDrawable that layers the coneDrawable and the icon_center Drawable
        // The icon_center Drawable is in the middle of the cone

        // Set the LayerDrawable as the icon for the Marker
        Log.d("flashLight","set accuracy: $accuracy")
        this.icon = LayerDrawable(arrayOf<Drawable>(coneDrawable, icon_center))
        Log.d("flashLight","set accuracy: $accuracy")

    }

    fun setDirection(r: Float) {
        this.rotation = -r
        map.invalidate()
    }


}

