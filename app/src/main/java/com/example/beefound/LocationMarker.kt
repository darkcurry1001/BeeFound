package com.example.beefound

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class GlowingConeDrawable(context: Context, private val accuracy: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowRadius = 20f
    private val coneHeight = 50f // Adjust cone height as needed

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds

        // Draw the glowing effect
        paint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), bounds.width() / 2f, paint)

        // Draw the cone
        paint.maskFilter = null
        val path = Path()
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        path.lineTo(bounds.exactCenterX(), bounds.top.toFloat() - coneHeight)
        path.lineTo(bounds.right.toFloat(), bounds.bottom.toFloat())
        path.lineTo(bounds.left.toFloat(), bounds.bottom.toFloat())
        path.close()

        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}


//Todo: google position marker
class LocationMarker: Marker {

    private var initialized = false
    private var map: MapView
    private var accuracy: Int? = 80
    private var direction: Float? = null
    private var glowingConeDrawable: GlowingConeDrawable? = null

    constructor(map: MapView, icon: Drawable, accuracy: Int?) : super(map){
        this.map = map
        this.icon = icon
        this.accuracy = accuracy
    }

    fun setLocation(lat: Double?, lon: Double?) {
        if (lat == null || lon == null) return
        if (!initialized) {
            map.overlays?.add(this)
            this.setOnMarkerClickListener { _, _ -> true }
            initialized = true
        }
        this.position = GeoPoint(lat, lon)
    }

    fun setAccuracy(accuracy: Int) {
        Log.d(
            "Location","update accuracy: $accuracy"
        )
//        if (glowingConeDrawable == null && direction === null) {
//            glowingConeDrawable = GlowingConeDrawable(map.context, accuracy)
//            icon = glowingConeDrawable
//        } else {
//            glowingConeDrawable?.let {
//                it.alpha = (accuracy / 100f * 255).toInt()
//                it.invalidateSelf()
//            }
//        }
    }

    fun setDirection(rotation: Float) {
        Log.d(
            "Location","update direction: $rotation"
        )
//        val matrix = Matrix()
//        matrix.postRotate(rotation)
//
//        val bitmap = (this.icon as BitmapDrawable).bitmap
//        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//
//        this.icon = BitmapDrawable(rotatedBitmap)
    }
}

