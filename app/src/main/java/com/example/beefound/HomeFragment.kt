package com.example.beefound

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val CAMERA_REQUEST_CODE = 4711
    private var someActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // set timestamp format
        val sdf = SimpleDateFormat("'Date                Time\n'dd-MM-yyyy    HH:mm:ss z")

        // set activity launcher for camera
        setActivityLauncher(view = view)

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
        val compass = view.findViewById<View>(R.id.view_compass)
        val btn_add = view.findViewById<Button>(R.id.btn_add_swarm)

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

        // setup map
        val ctx = activity?.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        val map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)                                   // enable 2 finger zoom
        map.setBuiltInZoomControls(false)                                 // disable zoom buttons

        val mapController = map.controller
        mapController.setZoom(14)                                           // set initial zoom level 14
        val startPoint = GeoPoint(48.8583, 2.2944)        // change to user's location
        mapController.setCenter(startPoint)

        // add markers (random for now)
        addmarker(view , longitude = 48.8583, latitude = 2.2944, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")
        addmarker(view , longitude = 2.2944, latitude = 48.8583, header = "title", snippet = "my text", time = sdf.format(Date()), user_email = "max.mustermann@gmail.com")

        // onclick navigation button
        btn_navigate.setOnClickListener {
            btn_maps.visibility = View.VISIBLE
            compass.visibility = View.VISIBLE
        }

        // onclick collected button
        btn_collected.setOnClickListener {
            TODO()
        }

        // onclick add swarm button
        btn_add.setOnClickListener {
            // Camera permissions and take photo
            if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.d(ContentValues.TAG, "Camera permission granted")
                takePhoto()
            } else {
                requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
                Log.d(ContentValues.TAG, "Camera permission requested")
                Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
                // check if permission was granted and take picture (does not work yet)
                if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(ContentValues.TAG, "Camera permission granted")
                    takePhoto()
                }
            }


            // set timestamp for marker
            val currentDateAndTime = sdf.format(Date())

            addmarker(view , longitude = 2.5, latitude = 50.0, header = "", snippet = "", time = currentDateAndTime, user_email = "max.mustermann_der_neue@gmail.com")

        }
        // onclick maps button (changes to other fragment for now)
        btn_maps.setOnClickListener { Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_popupFragment) }

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

    // add new marker to map
    fun addmarker(view: View, longitude: Double, latitude: Double, header: String, snippet: String, time: String, user_email: String) {
        val map = view.findViewById<MapView>(R.id.map)
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude) // Set the position for the marker
        //marker.isInfoWindowShown // Show the info window
        //marker.title = "Marker Title"
        //marker.snippet = "Marker Snippet"
        map.overlays?.add(marker)
        map.invalidate()

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

                // display popup and hide add button
                popup.visibility = View.VISIBLE
                img_bees.visibility = View.VISIBLE
                timestamp.visibility = View.VISIBLE
                status.visibility = View.VISIBLE
                email.visibility = View.VISIBLE
                btn_navigate.visibility = View.VISIBLE
                btn_collected.visibility = View.VISIBLE
                btn_close.visibility = View.VISIBLE
                btn_add.visibility = View.INVISIBLE

                // set picture according to clicked marker
                img_bees.setImageResource(R.drawable.bees)

                // set timestamp and initial status
                timestamp.text = time
                status.text = "Ready to be collected"

                // add email, break at @ if too long
                if (user_email.length > 30) {
                    val email1 = user_email.substring(0, user_email.indexOf("@"))
                    val email2 = user_email.substring(user_email.indexOf("@"))
                    val user_email_split = email1 + "\n" + email2
                    email.text = user_email_split
                } else
                email.text = user_email

                return true
            }
        })
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