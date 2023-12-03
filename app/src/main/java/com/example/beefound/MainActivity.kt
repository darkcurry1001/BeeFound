package com.example.beefound

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.beefound.databinding.ActivityHomeBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay





class MainActivity : Activity() {
    lateinit var map: MapView
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_home)
        map = findViewById<View>(R.id.map) as MapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)                                   // enable 2 finger zoom
        map.setBuiltInZoomControls(false)                                 // disable zoom buttons

        val mapController = map.controller
        mapController.setZoom(14)                                           // set initial zoom level
        val startPoint = GeoPoint(48.8583, 2.2944)        // change to user's location
        mapController.setCenter(startPoint)

        addmarker(map , longitude = 0.001, latitude = 0.001, header = "title", snippet = "my text")
        addmarker(map , longitude = 80.001, latitude = 80.001, header = "title", snippet = "my text")

    }

    public override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    public override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }

    fun addmarker(map: MapView, longitude: Double, latitude: Double, header: String, snippet: String) {
        val marker = Marker(map)
        marker.position = GeoPoint(longitude, latitude) // Set the position for the marker
        marker.title = "Marker Title"
        marker.snippet = "Marker Snippet"
        map.overlays?.add(marker)
        map.invalidate()
    }

}
