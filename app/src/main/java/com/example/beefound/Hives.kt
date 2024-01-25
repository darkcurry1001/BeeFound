package com.example.beefound

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class Hives : Activity() {
    var hiveList_names = ArrayList<String>()
    var hiveList = ArrayList<Hive>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hives)
        lateinit var fusedLocationClient: FusedLocationProviderClient
        var locationRequest: LocationRequest? = null
        var latitude_glob: Double? = 48.3458708
        var longitude_glob: Double? = 14.5307493
        // get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
                    Log.d(ContentValues.TAG, "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                }
            }
        }
        val addHive = findViewById<Button>(R.id.addHive)
        val hivelv = findViewById<ListView>(R.id.hivelist)
        val hivename = findViewById<TextView>(R.id.hivenametxt)


        val adapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, hiveList_names)
        hivelv.adapter = adapter
        addHive.setOnClickListener {
            if (hivename.text.toString() != "") {

                hiveList_names.add(hivename.text.toString())
                hiveList.add(Hive(hivename.text.toString(), latitude_glob, longitude_glob))
                adapter.notifyDataSetChanged()
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        hivelv.setOnItemClickListener { parent, view, position, id ->
            //val intent = Intent(this, MainActivity::class.java)
            //intent.putExtra("hive_name", hiveList[position].name)
            //intent.putExtra("hive_latitude", hiveList[position].latitude)
            //intent.putExtra("hive_longitude", hiveList[position].longitude)
            Log.d(ContentValues.TAG, "Name: ${hiveList[position].name} Latitude: ${hiveList[position].latitude}, Longitude: ${hiveList[position].longitude}")
            //startActivity(intent)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Confirm")
                .setMessage("Do you want to mark this hive to be searched for?")
                .setPositiveButton("Yes") { dialog, which ->
                    // api call to add hive to be searched for
                    Log.d(ContentValues.TAG, "Search for hive ${hiveList[position].name}")
                    }
                .setNegativeButton("Stop searching") { dialog, which ->
                    // api call to stop searching for hive
                    Log.d(ContentValues.TAG, "Stop searching for hive ${hiveList[position].name}")
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

    }
}

class Hive(val name: String, val latitude: Double?, val longitude: Double?)