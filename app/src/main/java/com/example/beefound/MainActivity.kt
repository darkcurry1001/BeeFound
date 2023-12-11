package com.example.beefound

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.FragmentActivity
import org.osmdroid.views.overlay.Marker
import java.io.File


class MainActivity : FragmentActivity(), SensorEventListener  {
    private val IMAGE_FILE_NAME: String = "test.jpg"

    private var photoFile: File = File("drawable/bees.jpg")

    var sensor: Sensor? = null
    var sensorManager: SensorManager? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    fun createPhotoFile(): File? {
        // check if external media is available
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
            Log.d(TAG, "Not mounted")
            return null
        }
        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.d(TAG, "Directory: $fileDir")
        if (!fileDir!!.exists()) {
            Log.d(TAG, "Path did not exist")
            if (!fileDir.mkdirs()) {
                Log.d(TAG, "Something wrong with directory: $fileDir")
            }
        }
        // Create file
        val imageFile = File("$fileDir/$IMAGE_FILE_NAME")
        Log.d(TAG, "Dir: $imageFile")
        photoFile = imageFile
        return imageFile
    }

    fun getImageFile(): File? {
        return photoFile
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            Log.d(
                TAG,
                "Rotation vector: ${event.values[0]}, ${event.values[1]}, ${event.values[2]}"
            )
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d(TAG, "Accuracy changed: $p0, $p1")
    }


}


/*
class MainActivity : FragmentActivity() {
    lateinit var map: MapView

    private val binding: ActivityHomeBinding? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var binding = ActivityHomeBinding.inflate(layoutInflater)
        //setContentView(binding.getRoot())
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //val navController = findNavController(this, R.id.nav_host_fragment_content_main)

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

        //val customInfoWindow = CustomMarkerInfoWindow(R.layout.information_window, map, this)

        addmarker(map , longitude = 40.001, latitude = 0.001, header = "title", snippet = "my text")
        addmarker(map , longitude = 0.001, latitude = 40.001, header = "title", snippet = "my text")

    }



    public override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(ContentValues.TAG, "Camera permission granted")
        } else {
            requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            Log.d(ContentValues.TAG, "Camera permission requested")
        }
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
        //marker.isInfoWindowShown // Show the info window
        //marker.title = "Marker Title"
        //marker.snippet = "Marker Snippet"
        map.overlays?.add(marker)
        map.invalidate()

        /*
        marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                marker.closeInfoWindow()
                //val fragment = findViewById<View>(R.id.container_fragment)
                //fragment.visibility = View.VISIBLE
                val navController = findNavController(this@MainActivity, R.id.nav_graph)
                navController.navigate(R.id.fragment_popup_content)
                return true
            }
        })*/
    }


}
*/