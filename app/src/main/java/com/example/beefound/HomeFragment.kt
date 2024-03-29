package com.example.beefound

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
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
import com.example.beefound.api.Hive
import com.example.beefound.api.Middleware
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
import kotlin.math.abs

class HomeFragment : Fragment(), SensorEventListener {
    private var accuracy: Int? = null
    lateinit var role: String

    private val CAMERA_REQUEST_CODE = 4711
    private var someActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    private val LOCATION_PERMISSION_REQ_CODE = 1000;
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var locationRequest: LocationRequest? = null

    lateinit var view1: View
    lateinit var map: MapView

    var swarms = mutableListOf<Marker>()

    var latitude_glob: Double? = null
    var longitude_glob: Double? = null
    var loc_updated: Boolean = false

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

    //hive info variables
    var currentlyNavigatingTo: Int = 0
    var markerList = mutableListOf<Marker>()

    lateinit var hivesFound: MutableList<Hive>
    lateinit var hivesNavigated: MutableList<Hive>
    lateinit var hivesSearched: MutableList<Hive>
    lateinit var hivesSaved: MutableList<Hive>

    var dipslayedIdsFound = mutableListOf<Int>()
    var dipslayedIdsNavigated = mutableListOf<Int>()
    var dipslayedIdsSearched = mutableListOf<Int>()

    var positionMarker: LocationMarker? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // reference to MainActivity
        val main = (activity as MainActivity)

        // get user data from MainActivity
        val userEmail = main.userEmail
        val userId = main.userId
        val userName = main.userName
        val userPhone = main.userPhone
        role = main.userRole

        Log.d("test", "user: $userName, $userEmail, $userPhone, $role")

        // get hive data from MainActivity
        hivesFound = main.hives_Found
        hivesNavigated = main.hives_Navigated
        hivesSaved = main.hives_Saved
        hivesSearched = main.hives_Searched
        Log.d("test", "hives: $hivesFound")

        // Inflate the layout for this fragment
        view1 = inflater.inflate(R.layout.fragment_home_regular_user, container, false)

        // if user is beekeeper, inflate different layout
        if (role == "beekeeper") {
            view1 = inflater.inflate(R.layout.fragment_home, container, false)
        }

        // set timestamp format
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm")

        // set activity launcher for camera
        setActivityLauncher(view = view1)

        // get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.create()
        locationRequest?.interval = TimeUnit.SECONDS.toMillis(60)
        locationRequest?.fastestInterval = TimeUnit.SECONDS.toMillis(30)
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // get map
        map = view1.findViewById<MapView>(R.id.map)
        val mapController = map.controller
        var austriaCenter = GeoPoint(47.516231, 14.550072)
        map.maxZoomLevel = 19.5
        map.minZoomLevel = 5.0
        mapController.setCenter(austriaCenter)
        mapController.animateTo(austriaCenter)
        mapController.setZoom(6.9)

        // set up user location marker
        val icon = resources.getDrawable(R.drawable.bee, null)
        positionMarker = LocationMarker(map, icon, accuracy, mapController)

        // center user location when location is found
        var locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    latitude_glob =
                        if (location.latitude != 0.0) location.latitude else latitude_glob
                    longitude_glob =
                        if (location.longitude != 0.0) location.longitude else longitude_glob

                    if (!loc_updated) {
                        mapController.setZoom(15.0)
                        val startPoint =
                            GeoPoint(
                                location.latitude,
                                location.longitude
                            )
                        Log.d("startCenter", "startPoint: $startPoint")
                        requireActivity().runOnUiThread {
                            kotlin.run {
                                Log.d("startCenter", "startPoint: $startPoint")
                                mapController.setCenter(startPoint)
                                mapController.animateTo(startPoint)
                            }
                        }
                        loc_updated = true
                    }
                    Log.d(
                        "startCenter",
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    )
                    if (latitude_glob != null && longitude_glob != null) {
                        positionMarker?.setLocation(latitude_glob!!, longitude_glob!!)
                        map.invalidate()
                    }
                }
            }
        }
        val menu_view = view1.findViewById<NavigationView>(R.id.nav_view)

        // add transparent overlay to enable map clicks
        val transparent_overlay = view1.findViewById<View>(R.id.transparent_overlay)

        // set up menu
        if (role == "beekeeper") {
            menu_view.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_hives -> {
                        val intent = Intent(requireContext(), Hives::class.java)
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
        } else {
            menu_view.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
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

        // check/get location permission
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

        // get vars for all overlay elements
        val popup = view1.findViewById<View>(R.id.view_popup)
        val img_bees = view1.findViewById<ImageView>(R.id.img_bees)
        val timestamp = view1.findViewById<TextView>(R.id.txt_timestamp)
        val status = view1.findViewById<TextView>(R.id.txt_status)
        val email = view1.findViewById<TextView>(R.id.txt_email)
        val btn_navigate = view1.findViewById<Button>(R.id.btn_navigate)
        val btn_collected = view1.findViewById<Button>(R.id.btn_collected)
        val btn_close = view1.findViewById<Button>(R.id.btn_close)

        val btn_maps = view1.findViewById<Button>(R.id.btn_maps)
        val compass = view1.findViewById<View>(R.id.image_compass)
        val btn_add = view1.findViewById<Button>(R.id.btn_add_swarm)
        val btn_menu = view1.findViewById<Button>(R.id.btn_menu)

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

        map.setBuiltInZoomControls(false)                                 // disable zoom buttons                                   // set initial zoom level 15

        // add markers of found hives
        fillmarkers(
            view1,
            hivesFound,
            hivesNavigated,
            hivesSearched,
            dipslayedIdsFound,
            dipslayedIdsNavigated,
            dipslayedIdsSearched
        )

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
                return@setOnClickListener
            }

            // open confirmation to add marker
            if (latitude_glob != null && longitude_glob != null) {
                markerConfirmation(
                    img = (activity as MainActivity?)?.getImageFile()
                )
            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            }

        }
        // onclick maps button
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

        startRepeatingTask()
        return view1
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

    // get compass direction from sensor
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

        var r = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360
        if (longitude_glob != null && latitude_glob != null && abs(rotation - r) > 5) {
            rotation = r
            var long_temp = longitude_glob!!
            var lat_temp = latitude_glob!!
            var diff_lon = longitude_marker - long_temp

            var y = sin(Math.toRadians(diff_lon)) * cos(Math.toRadians(latitude_marker))
            var x = cos(Math.toRadians(lat_temp)) * sin(Math.toRadians(latitude_marker)) - sin(
                Math.toRadians(lat_temp)
            ) * cos(Math.toRadians(latitude_marker)) * cos(Math.toRadians(diff_lon))


            var angle = atan2(y, x)
            var angle_deg = Math.toDegrees(angle)
            angle_deg = (angle_deg + 360) % 360

            var compass_angle = (angle_deg - rotation + 360) % 360


            var compass = view1?.findViewById<ImageView>(R.id.image_compass)
            compass?.rotation = compass_angle.toFloat()
            positionMarker?.setDirection(rotation.toFloat())
        }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(
            "LocationACC", "update direction: ${accuracy}"
        )
        if (abs(
                this.accuracy?.minus(accuracy) ?: 0
            ) > 0 && sensor?.type == Sensor.TYPE_MAGNETIC_FIELD
        ) {
            this.accuracy = accuracy
            positionMarker?.setAccuracy(accuracy)
        }
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
    ): Marker {
        val map = view.findViewById<MapView>(R.id.map)
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.snippet = snippet
        marker.icon = resources.getDrawable(R.drawable.bee_marker, null)
        marker.id = marker_id.toString()
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

                Log.d(
                    "test",
                    "marker id: $marker_id, marker snippet: ${marker.snippet}, marker status: ${status.text}"
                )

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
                    StartActivity.api.DeleteRequest(
                        "hive?id=$marker_id",
                        "",
                        fun(response: String) {
                            (activity as MainActivity).runOnUiThread {
                                kotlin.run {
                                    Log.d("test", "deleted hive")
                                }
                            }
                        },
                        fun(i: Int, response: String) {
                            Log.d("test", "error deleting hive")
                        }).start()
                }

                // onclick for navigate button
                btn_navigate.setOnClickListener {

                    currentlyNavigatingTo = marker_id
                    marker.snippet = "Beekeeper on the way!"
                    status.text = marker.snippet
                    btn_navigate.visibility = View.INVISIBLE
                    marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
                    map.invalidate()

                    btn_maps.visibility = View.VISIBLE
                    compass.visibility = View.VISIBLE

                    // reset status of other hives
                    StartActivity.api.PutRequest(
                        "hive/navigate?id=0",
                        "",
                        fun(response: String) {
                            (activity as MainActivity).runOnUiThread {
                                kotlin.run {
                                    Log.d("test", "hive set navigate")
                                    StartActivity.api.PutRequest(
                                        "hive/navigate?id=$marker_id", "", fun(response: String) {
                                            Log.d("test", "hive set navigate")
                                        }).start()
                                }
                            }
                        },
                        fun(i: Int, response: String) {
                            Log.d("test", "error navigate hive")
                        }).start()

                    // onclick for navigate button
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

                // change marker status
                when (marker.snippet) {
                    "Ready to be collected!" -> {
                        if (role == "beekeeper") {
                            btn_navigate.visibility = View.VISIBLE
                            btn_collected.visibility = View.VISIBLE
                        } else {
                            btn_navigate.visibility = View.INVISIBLE
                            btn_collected.visibility = View.INVISIBLE
                        }
                    }

                    "Beekeeper on the way!" -> {
                        if (role == "beekeeper") {
                            Log.d("test", "btn should show")
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
                    }

                    else -> {
                    }
                }

                // load image from data base
                StartActivity.api.GetRequest("hive/img?id=$marker_id",
                    imgCallback = fun(response: Bitmap?) {
                        Log.d("test", "get img")
                        (activity as MainActivity).runOnUiThread {
                            kotlin.run {
                                img_bees.setImageBitmap(response)
                            }
                        }
                    },
                    errorCallback = fun(i: Int, response: String) {
                        Log.d("test", "error getting img")
                    }).start()

                return true
            }
        })

        if (snippet == "Other beekeeper on the way!") {
            marker.icon = resources.getDrawable(R.drawable.bee_marker_gray, null)
            marker.snippet = "Other beekeeper on the way!"
            map.invalidate()
        }

        return marker
    }

    // user confirmation to add marker
    fun markerConfirmation(
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
                    Log.d("testFound", "Found hive added successfully")
                    StartActivity.api.sendMultipartRequest(
                        jsonData = "{\"Latitude\":\"$latitude_glob\",\"Longitude\":\"$longitude_glob\",\"type\":\"found\"}",
                        imageFile = img,
                        serverUrl = "hive/found",
                        callback = fun(response: String) {
                            requireActivity().runOnUiThread {
                                Log.d("testFound", "Found hive added successfully")
                                Log.d("testFound", "FileSend UI update")
                                Log.d("test", "restart loop")
                                stopRepeatingTask()
                                startRepeatingTask()
                                fillmarkers(
                                    view1,
                                    hivesFound,
                                    hivesNavigated,
                                    hivesSearched,
                                    dipslayedIdsFound,
                                    dipslayedIdsNavigated,
                                    dipslayedIdsSearched
                                )
                            }
                        },
                        errorCallback = fun(i: Int, response: String) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Error uploading found hive\n $response",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d("testFound", "FileSend UI error update")
                                Log.d("testFound", "FileSend UI error: $response")
                            }
                        }
                    )

                }).start()
            }
            .setNegativeButton("No") { dialog, which ->
                // Do not add marker
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    // take picture
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

    // add polygon to map
    fun addlostpoly(view: View, at: GeoPoint, radius: Double) {

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

    // reformat date time from data base for displaying
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

    // define update loop for hives
    val handler = Handler()
    val delayMillis: Long = 1000 // 1 second
    val runnable: Runnable = object : Runnable {
        override fun run() {
            // repeatedly executed code
            Middleware.getHives(fun(
                hFound: MutableList<Hive>,
                hNavigated: MutableList<Hive>,
                hSaved: MutableList<Hive>,
                hSearched: MutableList<Hive>
            ) {
                (activity as MainActivity).runOnUiThread {
                    kotlin.run {
                        Log.d("test", "gethives: ")
                        hivesFound = hFound
                        hivesNavigated = hNavigated
                        hivesSaved = hSaved
                        hivesSearched = hSearched

                        fillmarkers(
                            view1,
                            hivesFound,
                            hivesNavigated,
                            hivesSearched,
                            dipslayedIdsFound,
                            dipslayedIdsNavigated,
                            dipslayedIdsSearched
                        )
                    }
                }
            }).start()
            // Schedule the next execution after the specified delay
            handler.postDelayed(this, delayMillis)
        }
    }

    private fun startRepeatingTask() {
        // Initial delay of 0 means it will start immediately
        handler.postDelayed(runnable, 0)
    }

    private fun stopRepeatingTask() {
        handler.removeCallbacks(runnable)
    }

    // update displayed markers
    fun fillmarkers(
        view: View,
        hivesFound: MutableList<Hive>,
        hivesNavigated: MutableList<Hive>,
        hivesSearched: MutableList<Hive>,
        dipslayedIdsFound: MutableList<Int>,
        displayedIdsNavigated: MutableList<Int>,
        dipslayedIdsSearched: MutableList<Int>
    ) {
        val hiveIdsFound = mutableListOf<Int>()
        val hiveIdsNavigated = mutableListOf<Int>()
        val hiveIdsSearched = mutableListOf<Int>()

        val removeIdsFound = mutableListOf<Int>()
        val removeIdsNavigated = mutableListOf<Int>()
        val removeIdsSearched = mutableListOf<Int>()

        // add polys of searched hives
        for (hive in hivesSearched) {
            hiveIdsSearched.add(hive.id)
            if (dipslayedIdsSearched.contains(hive.id)) {
                continue
            }
            dipslayedIdsSearched.add(hive.id)
            Log.d(
                "test",
                "search hive at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}"
            )
            addlostpoly(
                view,
                at = GeoPoint(hive.latitude.toDouble(), hive.longitude.toDouble()),
                radius = 1000.0
            )
        }
        for (id in dipslayedIdsSearched) {
            if (!hiveIdsSearched.contains(id)) {
                removeIdsSearched.add(id)
            }
        }
        dipslayedIdsSearched.removeAll(removeIdsSearched)

        // add markers of found hives
        for (hive in hivesFound) {
            hiveIdsFound.add(hive.id)
            if (dipslayedIdsFound.contains(hive.id)) {
                continue
            }
            Log.d(
                "test",
                "found hive ${hive.id} at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}"
            )
            dipslayedIdsFound.add(hive.id)
            markerList.add(
                addmarker(
                    view,
                    longitude = hive.longitude.toDouble(),
                    latitude = hive.latitude.toDouble(),
                    header = "title",
                    snippet = "Ready to be collected!",
                    time = reformatDateTime(hive.created),
                    user_email = hive.email,
                    marker_id = hive.id
                )
            )
        }
        for (id in dipslayedIdsFound) {
            if (!hiveIdsFound.contains(id)) {
                removeIdsFound.add(id)
            }
        }
        dipslayedIdsFound.removeAll(removeIdsFound)

        // add markers of navigated hives
        for (hive in hivesNavigated) {
            hiveIdsNavigated.add(hive.id)
            if (displayedIdsNavigated.contains(hive.id)) {
                continue
            }
            Log.d(
                "test",
                "navigated hive at: ${hive.longitude.toDouble()},  ${hive.latitude.toDouble()}"
            )
            displayedIdsNavigated.add(hive.id)
            markerList.add(
                addmarker(
                    view,
                    longitude = hive.longitude.toDouble(),
                    latitude = hive.latitude.toDouble(),
                    header = "title",
                    snippet = "Other beekeeper on the way!",
                    time = reformatDateTime(hive.created),
                    user_email = hive.email,
                    marker_id = hive.id
                )
            )
        }
        for (id in displayedIdsNavigated) {
            if (!hiveIdsNavigated.contains(id)) {
                removeIdsNavigated.add(id)
                for (marker in markerList) {
                    Log.d("Time", "${marker.id}, ${id.toString()}")
                    if (marker.id == id.toString()) {
                        marker.remove(map)
                        map.invalidate()
                    }
                }
            }
        }
        displayedIdsNavigated.removeAll(removeIdsNavigated)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        initializeSensors()
        startRepeatingTask()
    }

    // stop update loop on stop
    override fun onStop() {
        super.onStop()
        Log.d("test", "onStop")
        //stop update loop
        stopRepeatingTask()
    }

    // stop navigation on destroy
    override fun onDestroy() {
        Log.d("test", "onStop")
        try {
            Log.d("test", "onStop")

            //stop update loop
            stopRepeatingTask()

            currentlyNavigatingTo = 0
            StartActivity.api.PutRequest(
                "hive/navigate?id=$currentlyNavigatingTo",
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
        } finally {
            super.onDestroy()
        }
    }
}

