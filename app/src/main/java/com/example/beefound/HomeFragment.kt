package com.example.beefound

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(), SensorEventListener  {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val CAMERA_REQUEST_CODE = 4711
    private var someActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    private val LOCATION_PERMISSION_REQ_CODE = 1000;
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var locationRequest: LocationRequest? = null


    private val permissionId = 2
    var swarms = mutableListOf<Marker>()

    var latitude_glob: Double = 48.30639
    var longitude_glob: Double = 14.28611

    var latitude_marker: Double = 48.30639
    var longitude_marker: Double = 14.28611

    // for sensor

    private lateinit var sensorManager: SensorManager
    var accelerometer: Sensor? = null
    var magnetometer: Sensor? = null


    val accelerometerReading = FloatArray(3)
    val magnetometerReading = FloatArray(3)

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)
    var rotation = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // set timestamp format
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm")

        // set activity launcher for camera
        setActivityLauncher(view = view)

        // get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.create()
        locationRequest?.interval = TimeUnit.SECONDS.toMillis(60)
        locationRequest?.fastestInterval = TimeUnit.SECONDS.toMillis(30)
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        var locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    latitude_glob = location.latitude
                    longitude_glob = location.longitude
                    Log.d(TAG, "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                }
            }
        }

        // set up menu
        val menu_view = view.findViewById<NavigationView>(R.id.nav_view)
        menu_view.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_hives -> {
                    val intent = Intent(requireContext(), SignUp::class.java)
                    startActivity(intent)
                }
                R.id.nav_profile -> {
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    val intent = Intent(requireContext(), StartActivity::class.java)
                    startActivity(intent)
                }
            }
            menu_view.visibility = View.INVISIBLE
            true
        }

        val transparent_overlay = view.findViewById<View>(R.id.transparent_overlay)

        transparent_overlay.setOnClickListener {
            menu_view.visibility = View.INVISIBLE
            transparent_overlay.visibility = View.INVISIBLE
        }



        if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission granted")
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())}
        else {
            requestPermissions(arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE)
            Log.d(TAG, "Location permission requested")
            Toast.makeText(requireContext(), "Location permission needed", Toast.LENGTH_SHORT).show()
            // check if permission was granted and take picture (does not work yet)
            if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted")
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }


        }


        // get map
        val map = view.findViewById<MapView>(R.id.map)

        // get vars for all overlay elements
        val popup = view.findViewById<View>(R.id.view_popup)
        val img_bees = view.findViewById<ImageView>(R.id.img_bees)
        val timestamp = view.findViewById<TextView>(R.id.txt_timestamp)
        val status = view.findViewById<TextView>(R.id.txt_status)
        val email = view.findViewById<TextView>(R.id.txt_email)
        val btn_navigate = view.findViewById<Button>(R.id.btn_navigate)
        val btn_collected = view.findViewById<Button>(R.id.btn_collected)
        val btn_close = view.findViewById<Button>(R.id.btn_close)

        val btn_maps = view.findViewById<Button>(R.id.btn_maps)
        val compass = view.findViewById<View>(R.id.image_compass)
        val btn_add = view.findViewById<Button>(R.id.btn_add_swarm)
        val btn_menu = view.findViewById<Button>(R.id.btn_menu)

        // initially hide some overlay elements
        popup.visibility = View.INVISIBLE
        img_bees.visibility = View.INVISIBLE
        timestamp.visibility = View.INVISIBLE
        status.visibility = View.INVISIBLE
        email.visibility = View.INVISIBLE
        btn_navigate.visibility = View.INVISIBLE
        btn_collected.visibility = View.INVISIBLE
        btn_close.visibility = View.INVISIBLE

        compass.visibility = View.INVISIBLE
        btn_maps.visibility = View.INVISIBLE

        // initialize sensors
        initializeSensors()


        // setup map
        val ctx = activity?.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)                                   // enable 2 finger zoom
        map.setBuiltInZoomControls(false)                                 // disable zoom buttons

        val mapController = map.controller
        mapController.setZoom(15)                                           // set initial zoom level 15
        val startPoint = GeoPoint(latitude_glob, longitude_glob)            // show user location initially
        mapController.setCenter(startPoint)

        // add markers (random for now)
        addmarker(view , longitude = 48.8583, latitude = 2.2944, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")
        addmarker(view , longitude = 2.28611, latitude = 48.30639, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")
        //addmarker(view , longitude = 2.2944, latitude = 48.8583, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")
        addmarker(view , longitude = 2.28611, latitude = 30.30639, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")
        addmarker(view , longitude = 22.28611, latitude = 48.30639, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")

        addlostpoly(view, at = GeoPoint(latitude_glob, longitude_glob) , radius = 1000.0) // add lost swarms (random for now)


        btn_menu.setOnClickListener {
            menu_view.visibility = View.VISIBLE
            transparent_overlay.visibility = View.VISIBLE
        }

        // onclick add swarm button
        btn_add.setOnClickListener {
            //getCurrentLocation()
            // Camera permissions and take photo
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted")
                takePhoto()
            } else {
                // request permission


                requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
                Log.d(TAG, "Camera permission requested")
                Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
                // check if permission was granted and take picture (does not work yet)
                if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted")
                    takePhoto()
                }
            }

            // set timestamp for marker
            val currentDateAndTime = sdf.format(Date())

            // open confirmation to add marker
            markerConfirmation(view , longitude = longitude_glob, latitude = latitude_glob, header = "", snippet = "", time = sdf.format(Date()), user_email = "max.mustermann_der_neue@gmail.com")

        }
        // onclick maps button (changes to other fragment for now)
        //btn_maps.setOnClickListener { Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_popupFragment) }
        btn_maps.setOnClickListener{
            val gmmIntentUri =
                Uri.parse("google.navigation:q=48.30639,14.28611")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
        // onclick close button
        btn_close.setOnClickListener {
            popup.visibility = View.INVISIBLE
            img_bees.visibility = View.INVISIBLE
            timestamp.visibility = View.INVISIBLE
            status.visibility = View.INVISIBLE
            email.visibility = View.INVISIBLE
            btn_navigate.visibility = View.INVISIBLE
            btn_collected.visibility = View.INVISIBLE
            btn_close.visibility = View.INVISIBLE
            btn_add.visibility = View.VISIBLE
        }

        return view

    }

    private fun initializeSensors() {
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magnetometer != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(requireContext(), "Sensors not available", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        rotation = (Math.toDegrees(orientationAngles[0].toDouble())+360)%360

        var diff_lon = longitude_marker - longitude_glob

        var y = sin(Math.toRadians(diff_lon)) * cos(Math.toRadians(latitude_marker))
        var x = cos(Math.toRadians(latitude_glob)) * sin(Math.toRadians(latitude_marker)) - sin(Math.toRadians(latitude_glob)) * cos(Math.toRadians(latitude_marker)) * cos(Math.toRadians(diff_lon))


        var angle = atan2(y, x)
        var angle_deg = Math.toDegrees(angle)
        angle_deg = (angle_deg + 360) % 360

        var compass_angle = (angle_deg - rotation + 360) % 360


        var compass = view?.findViewById<ImageView>(R.id.image_compass)
        compass?.rotation = compass_angle.toFloat()


    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this example
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        initializeSensors()
    }

    // add new marker to map
    fun addmarker(view: View, longitude: Double, latitude: Double, header: String, snippet: String, time: String, user_email: String) {
        val map = view.findViewById<MapView>(R.id.map)
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude) // Set the position for the marker
        //marker.isInfoWindowShown // Show the info window
        //marker.title = "Marker Title"
        marker.snippet = "Ready to be collected!"
        marker.icon = resources.getDrawable(R.drawable.bee_marker, null)
        map.overlays?.add(marker)
        map.invalidate()
        swarms.add(marker)

        // onclick for marker
        marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {

                marker.closeInfoWindow()    // do not show the standard info window

                // get vars for overlay elements
                val popup = view.findViewById<View>(R.id.view_popup)
                val img_bees = view.findViewById<ImageView>(R.id.img_bees)
                val timestamp = view.findViewById<TextView>(R.id.txt_timestamp)
                val status = view.findViewById<TextView>(R.id.txt_status)
                val email = view.findViewById<TextView>(R.id.txt_email)
                val btn_navigate = view.findViewById<Button>(R.id.btn_navigate)
                val btn_collected = view.findViewById<Button>(R.id.btn_collected)
                val btn_close = view.findViewById<Button>(R.id.btn_close)

                val btn_add = view.findViewById<Button>(R.id.btn_add_swarm)

                val btn_maps = view.findViewById<Button>(R.id.btn_maps)
                val compass = view.findViewById<View>(R.id.image_compass)

                // display popup and hide add button
                popup.visibility = View.VISIBLE
                img_bees.visibility = View.VISIBLE
                timestamp.visibility = View.VISIBLE
                status.visibility = View.VISIBLE
                email.visibility = View.VISIBLE
                btn_close.visibility = View.VISIBLE
                btn_add.visibility = View.INVISIBLE

                // set picture according to clicked marker
                img_bees.setImageResource(R.drawable.bees)

                // add email, break at @ if too long
                if (user_email.length > 200) {
                    val email1 = user_email.substring(0, user_email.indexOf("@"))
                    val email2 = user_email.substring(user_email.indexOf("@"))
                    val user_email_split = email1 + "\n" + email2
                    email.text = "Found by: \n $user_email_split"
                } else {
                    email.text = "Found by: \n ${user_email}"
                }
                when (marker.snippet) {
                    "Ready to be collected!" -> {
                        btn_navigate.visibility = View.VISIBLE
                        btn_collected.visibility = View.VISIBLE

                        // set timestamp and initial status
                        timestamp.text = time
                        status.text = marker.snippet

                        // onclick for collected button
                        btn_collected.setOnClickListener {
                            status.text = "Collected!"
                            btn_collected.visibility = View.INVISIBLE
                            btn_navigate.visibility = View.INVISIBLE

                            btn_maps.visibility = View.INVISIBLE
                            compass.visibility = View.INVISIBLE
                            popup.visibility = View.INVISIBLE
                            img_bees.visibility = View.INVISIBLE
                            timestamp.visibility = View.INVISIBLE
                            status.visibility = View.INVISIBLE
                            email.visibility = View.INVISIBLE
                            btn_close.visibility = View.INVISIBLE
                            btn_add.visibility = View.VISIBLE


                            //marker.snippet = "Collected!"
                            map.overlays?.remove(marker)
                            map.invalidate()
                        }


                        btn_navigate.setOnClickListener {
                            marker.snippet = "Beekeeper on the way!"
                            status.text = marker.snippet
                            btn_navigate.visibility = View.INVISIBLE
                            marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
                            map.invalidate()

                            btn_maps.visibility = View.VISIBLE
                            compass.visibility = View.VISIBLE

                            btn_maps.setOnClickListener{

                                //get longitude and latitude of marker
                                val latitude = marker.position.latitude
                                val longitude = marker.position.longitude


                                val gmmIntentUri =
                                    Uri.parse("google.navigation:q=$latitude,$longitude")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                startActivity(mapIntent)
                            }
                            latitude_marker = latitude
                            longitude_marker = longitude


                        }

                        return true
                    }
                    "Beekeeper on the way!" -> {
                        btn_navigate.visibility = View.INVISIBLE
                        btn_collected.visibility = View.VISIBLE

                        // set timestamp and initial status
                        timestamp.text = time
                        status.text = marker.snippet

                        return true
                    }
                    else -> {
                        // TODO: add for imker in the way
                        return true
                    }
                }
            }
        })
    }

    fun markerConfirmation(view: View, longitude: Double, latitude: Double, header: String, snippet: String, time: String, user_email: String) {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm")

        Log.d(TAG, "conformation")

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder
            .setTitle("Confirm")
            .setMessage("Do you want to add a new swarm?")
            .setPositiveButton("Yes") { dialog, which ->
                // add marker
                addmarker(view , longitude = longitude_glob, latitude = latitude_glob, header = "", snippet = "", time = sdf.format(Date()), user_email = "coroian.petruta.simina_even_longer@gmail.com")
            }
            .setNegativeButton("No") { dialog, which ->
                // Do not add marker
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    fun takePhoto() {
        Log.d(TAG, "Use system camera to take photo")
        // use Intent to access camera
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            // new for FileProvider
            val imageFile: File? = (activity as MainActivity?)?.createPhotoFile()
            var photoURI: Uri? = null
            if (imageFile != null) {
                photoURI = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.example.beefound.fileprovider",
                    imageFile)
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            someActivityResultLauncher?.launch(takePictureIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addlostpoly(view: View, at: GeoPoint, radius: Double) {
        /*
        view ... map view from fragment_home.xml to add the polygon to
        at ... GeoPoint of location of the center of the circle
        radius ... radius of the circle in meters
        */

        val map = view.findViewById<MapView>(R.id.map)
        val circle = Polygon()
        circle.setFillColor(0x3000FF00)             // Fill color (semi-transparent green)
        circle.setStrokeColor(0xFF00FF00.toInt())   // Stroke color (green)
        circle.setStrokeWidth(2F)                   // Stroke width

        val numberOfPoints = 100 // Number of points to create a smooth circle

        // calculate points for the circle
        for (i in 0 until numberOfPoints) {
            val angle = Math.PI * 2 * i / numberOfPoints
            val x: Double = at.latitude + radius / 111000.0 * cos(angle)
            val y: Double = at.longitude + radius / (111000.0 * cos(Math.toRadians(at.latitude))) * sin(angle)
            circle.addPoint(GeoPoint(x, y))
        }

        // Add the circle Polygon to the map
        map.overlays?.add(circle);

        // Refresh the map to display the circle
        map.invalidate()
    }

    private fun setActivityLauncher(view: View) {
        someActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),

                fun(result: ActivityResult) {
                    if (result.resultCode == Activity.RESULT_OK) {
                        Log.d(TAG, "Photo has been taken")
                        // new file provider code
                        val imgFile: File? = (activity as MainActivity?)?.getImageFile()
                        Log.d(TAG, "activityResult (path): $imgFile")
                        if (imgFile?.exists() == true) {
                            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                            view.findViewById<ImageView>(R.id.img_bees).setImageBitmap(myBitmap)
                        }
                    }
                })
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }



}