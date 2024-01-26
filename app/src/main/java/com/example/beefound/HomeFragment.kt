package com.example.beefound

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
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
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment(), SensorEventListener  {
    lateinit var role: String
    // TODO: Rename and change types of parameters

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

    var currentlyNavigatingTo: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // get user data from MainActivity
        // reference to MainActivity
        val main = (activity as MainActivity)
        val userEmail = main.userEmail
        val userId = main.userId
        val userName = main.userName
        val userPhone = main.userPhone
        role = main.userRole

        Log.d("test", "user: $userName, $userEmail, $userPhone, $role")

        // get hive data from MainActivity
        val hivesFound = main.hives_Found
        val hivesNavigated = main.hives_Navigated
        val hivesSaved = main.hives_Saved
        val hivesSearched = main.hives_Searched
        Log.d("test", "hives: $hivesFound")

        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_home_regular_user, container, false)

       // if user is beekeeper, inflate different layout
        if (role == "beekeeper") {
            view = inflater.inflate(R.layout.fragment_home, container, false)
        }

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
                for (location in locationResult.locations) {
                    latitude_glob = location.latitude
                    longitude_glob = location.longitude
                    Log.d(TAG, "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                }
            }
        }
        val menu_view = view.findViewById<NavigationView>(R.id.nav_view)

        val transparent_overlay = view.findViewById<View>(R.id.transparent_overlay)
        if(role == "beekeeper"){

            menu_view.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_hives -> {
                        val intent = Intent(requireContext(), Hives::class.java)
                        startActivity(intent)
                    }
                    R.id.nav_profile -> {
                        val intent = Intent(requireContext(), ProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.nav_logout -> {
                        StartActivity.api.Logout()
                        val intent = Intent(requireContext(), StartActivity::class.java)
                        startActivity(intent)
                    }
                }
                menu_view.visibility = View.INVISIBLE
                true
            }
            transparent_overlay.setOnClickListener {
                menu_view.visibility = View.INVISIBLE
                transparent_overlay.visibility = View.INVISIBLE
            }
        }
        
        else{
            menu_view.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_profile -> {
                        val intent = Intent(requireContext(), ProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.nav_logout -> {
                        StartActivity.api.Logout()
                        val intent = Intent(requireContext(), StartActivity::class.java)
                        startActivity(intent)
                    }
                }
                menu_view.visibility = View.INVISIBLE
                true
            }



            transparent_overlay.setOnClickListener {
                menu_view.visibility = View.INVISIBLE
                transparent_overlay.visibility = View.INVISIBLE
            }
        }

        // set up menu


        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Location permission granted")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQ_CODE
            )
            Log.d(TAG, "Location permission requested")
            Toast.makeText(requireContext(), "Location permission needed", Toast.LENGTH_SHORT)
                .show()
            // check if permission was granted and take picture (does not work yet)
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Location permission granted")
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
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
        val startPoint =
            GeoPoint(latitude_glob, longitude_glob)            // show user location initially
        mapController.setCenter(startPoint)

        // add markers of found hives
        for (hive in hivesFound){
            Log.d("test", "found hive at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}")
            addmarker(view , longitude = hive.longitude.toDouble(), latitude = hive.latitude.toDouble(), header = "title", snippet = "Ready to be collected!", time = reformatDateTime(hive.created), user_email = hive.id.toString(), marker_id = hive.id)
        }

        // add markers of navigated hives
        for (hive in hivesNavigated){
            Log.d("test", "navigated hive at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}")
            addmarker(view , longitude = hive.longitude.toDouble(), latitude = hive.latitude.toDouble(), header = "title", snippet = "Other beekeeper on the way!", time = reformatDateTime(hive.created), user_email = hive.id.toString(), marker_id = hive.id)
        }

        // add polys of searched hives
        for (hive in hivesSearched){
            Log.d("test", "search hive at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}")
            addlostpoly(view, at = GeoPoint(hive.latitude.toDouble(), hive.longitude.toDouble()) , radius = 1000.0)
        }

        btn_menu.setOnClickListener {
            menu_view.visibility = View.VISIBLE
            transparent_overlay.visibility = View.VISIBLE
        }

        // onclick add swarm button
        btn_add.setOnClickListener {

            // Camera permissions and take photo
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Camera permission granted")
                takePhoto()
            } else {
                // request permission
                requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
                Log.d(TAG, "Camera permission requested")
                Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT)
                    .show()
                // check if permission was granted and take picture (does not work yet)
                if (checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "Camera permission granted")
                    takePhoto()
                }
            }

            // set timestamp for marker
            val currentDateAndTime = sdf.format(Date())

            // open confirmation to add marker
            markerConfirmation(
                view,
                longitude = longitude_glob,
                latitude = latitude_glob,
                header = "",
                snippet = "",
                time = sdf.format(Date()),
                user_email = userEmail,
                img = (activity as MainActivity?)?.getImageFile()
            )

        }
        // onclick maps button (changes to other fragment for now)
        //btn_maps.setOnClickListener { Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_popupFragment) }
        btn_maps.setOnClickListener {
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
                System.arraycopy(
                    event.values,
                    0,
                    accelerometerReading,
                    0,
                    accelerometerReading.size
                )
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

        rotation = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360

        var diff_lon = longitude_marker - longitude_glob

        var y = sin(Math.toRadians(diff_lon)) * cos(Math.toRadians(latitude_marker))
        var x = cos(Math.toRadians(latitude_glob)) * sin(Math.toRadians(latitude_marker)) - sin(
            Math.toRadians(latitude_glob)
        ) * cos(Math.toRadians(latitude_marker)) * cos(Math.toRadians(diff_lon))


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
    fun addmarker(
        view: View,
        longitude: Double,
        latitude: Double,
        header: String,
        snippet: String,
        time: String,
        user_email: String,
        marker_id: Int,
    ) {
        val map = view.findViewById<MapView>(R.id.map)
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude) // Set the position for the marker
        //marker.isInfoWindowShown // Show the info window
        //marker.title = "Marker Title"
        marker.snippet = snippet
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

                    map.overlays?.remove(marker)
                    map.invalidate()

                    // delete collected hive from data base
                    StartActivity.api.DeleteRequest("hive?id=$marker_id", "", fun(response: String) {
                        (activity as MainActivity).runOnUiThread {
                            kotlin.run {
                                Log.d("test", "deleted hive")
                            }
                        }
                    }, fun(i:Int, response: String) {
                        Log.d("test", "error deleting hive")
                    }).start()
                }


                btn_navigate.setOnClickListener {
                    if (currentlyNavigatingTo != -1) {
                        Toast.makeText(activity as MainActivity,"alredy navigating to a hive", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    currentlyNavigatingTo = marker_id
                    marker.snippet = "beekeeper on the way!"
                    status.text = marker.snippet
                    btn_navigate.visibility = View.INVISIBLE
                    marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
                    map.invalidate()

                    btn_maps.visibility = View.VISIBLE
                    compass.visibility = View.VISIBLE

                    StartActivity.api.PutRequest(
                        "hive/navigate?id=$marker_id",
                        "",
                        fun(response: String) {
                            (activity as MainActivity).runOnUiThread {
                                kotlin.run {
                                    Log.d("test", "hive set navigate")
                                }
                            }
                        },
                        fun(i: Int, response: String) {
                            Log.d("test", "error navigate hive")
                        }).start()

                    btn_maps.setOnClickListener {

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


                when (marker.snippet) {
                    "Ready to be collected!" -> {
                        if (role == "beekeeper") {
                            btn_navigate.visibility = View.VISIBLE
                            btn_collected.visibility = View.VISIBLE
                        } else {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.INVISIBLE
                        }
                        return true
                    }

                    "Beekeeper on the way!" -> {
                        if (role == "beekeeper") {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.VISIBLE
                        } else {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.INVISIBLE
                        }

                        marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
                        map.invalidate()

                        // set timestamp and initial status
                        timestamp.text = time
                        status.text = marker.snippet

                        return true
                    }

                    "Other beekeeper on the way!" -> {
                        if (role == "beekeeper") {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.VISIBLE
                        } else {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.INVISIBLE
                        }

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

        if (snippet == "Other beekeeper on the way!") {
            marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
            marker.snippet = "Other beekeeper on the way!"
            map.invalidate()
        }
    }

    fun markerConfirmation(
        view: View,
        longitude: Double,
        latitude: Double,
        header: String,
        snippet: String,
        time: String,
        user_email: String,
        img: File? = null
    ) {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm")

        Log.d(TAG, "conformation")

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder
            .setTitle("Confirm")
            .setMessage("Do you want to add a new swarm?")
            .setPositiveButton("Yes") { dialog, which ->
                // add marker
                Thread(Runnable {
                    StartActivity.api.sendMultipartRequest(
                        jsonData = "{\"Latitude\":\"$latitude_glob\",\"Longitude\":\"$longitude_glob\",\"type\":\"found\"}",
                        imageFile = img,
                        serverUrl = "hive/found"
                    )
                    requireActivity().runOnUiThread {
                        Log.d("test", "FileSend UI update")
                        addmarker(
                            view,
                            longitude = longitude_glob,
                            latitude = latitude_glob,
                            header = "",
                            snippet = "Ready to be collected!",
                            time = sdf.format(Date()),
                            user_email = user_email,
                            marker_id = 0, //todo change
                        )
                    }
                }).start()

            }
            .setNegativeButton("No") { dialog, which ->
                // Do not add marker
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    fun takePhoto(): File? {
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
                    imageFile
                )
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            someActivityResultLauncher?.launch(takePictureIntent)
            return imageFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
            val y: Double =
                at.longitude + radius / (111000.0 * cos(Math.toRadians(at.latitude))) * sin(angle)
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

    fun reformatDateTime(originalDateTime: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSS", Locale.US)
        val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)

        try {
            val parsedDate = inputFormat.parse(originalDateTime)
            return outputFormat.format(parsedDate)
        } catch (e: ParseException) {
            // Handle the exception, e.g., log it or return an error string
            e.printStackTrace()
            return "Invalid Date"
        }
    }

    //todo evtl. to onDestroy
    override fun onStop() {
        super.onStop()

        Log.d("test", "onDestroy")

        currentlyNavigatingTo = 0
        StartActivity.api.PutRequest("hive/navigate?id=$currentlyNavigatingTo", "", fun(response: String) {
            (activity as MainActivity).runOnUiThread {
                kotlin.run {
                    Log.d("test", "hive set navigate")
                }
            }
        }, fun(i:Int, response: String) {
            Log.d("test", "error navigate hive")
        }).start()
    }
}

