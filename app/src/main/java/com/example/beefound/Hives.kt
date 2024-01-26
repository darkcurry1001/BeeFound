package com.example.beefound

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat
import com.example.beefound.api.Api
import com.example.beefound.api.LocalStorageManager
import com.example.beefound.api.Middleware
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

    var hiveList_names_searched = ArrayList<String>()
    var hiveList_searched = ArrayList<Hive>()

    var locationRequest: LocationRequest? = null
    var latitude_glob: Double? = 48.4458708
    var longitude_glob: Double? = 14.5307493



    lateinit var hives_Found: List<com.example.beefound.api.Hive>
    lateinit var hives_Navigated: List<com.example.beefound.api.Hive>
    lateinit var hives_Saved: List<com.example.beefound.api.Hive>
    lateinit var hives_Searched: List<com.example.beefound.api.Hive>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Middleware.getHives(@SuppressLint("SuspiciousIndentation")
        fun(hivesFound: MutableList<com.example.beefound.api.Hive>, hivesNavigated: MutableList<com.example.beefound.api.Hive>, hivesSaved: MutableList<com.example.beefound.api.Hive>, hivesSearched: MutableList<com.example.beefound.api.Hive>){
            runOnUiThread {
                kotlin.run {
                    Log.d("test", "gethives: ")
                    hives_Found = hivesFound
                    hives_Navigated = hivesNavigated
                    hives_Saved = hivesSaved
                    hives_Searched = hivesSearched


                    Log.d("test", "hives_saved: $hives_Saved")
                    for (hive in hives_Saved){
                            hiveList_names.add("ID:${hive.id} "+hive.name)
                            hiveList.add(Hive("${hive.name}", hive.id, hive.latitude.toDouble(), hive.longitude.toDouble()))



                    }
                    for (hive in hives_Searched){
                        hiveList_names_searched.add("ID:${hive.id} "+hive.name)
                        hiveList_searched.add(Hive("${hive.name}", hive.id, hive.latitude.toDouble(), hive.longitude.toDouble()))
                    }


                    setContentView(R.layout.activity_hives)

                    lateinit var fusedLocationClient: FusedLocationProviderClient

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
                                Log.d(ContentValues.TAG, "Latitude: $latitude_glob, Longitude: $longitude_glob")
                            }
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

                        return@run
                    }
                    fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                    )

                    val addHive = findViewById<Button>(R.id.addHive)

                    val hivename = findViewById<TextView>(R.id.hivenametxt)
                    val hivelv = findViewById<ListView>(R.id.hivelist)


                    val adapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, hiveList_names)
                    hivelv.adapter = adapter
                    addHive.setOnClickListener {
                        if (hivename.text.toString() != "") {
                            // api call to add hive to saved list
                            var id = 0

                            StartActivity.api.PostRequest("hive/save/",
                                "{\"Latitude\":\"$latitude_glob\",\"Longitude\":\"$longitude_glob\",\"type\":\"saved\",\"name\":\"${hivename.text}\"}",
                                fun(response: String) {
                                    runOnUiThread {
                                        kotlin.run {
                                            val jsonresponse = JSONObject(response)
                                            val hive = jsonresponse.getJSONObject("hive")

                                            id = hive.getInt("ID")
                                            Toast.makeText(this, "Hive added to saved list", Toast.LENGTH_LONG).show()

                                            hiveList_names.add("ID:$id "+hivename.text.toString())
                                            hiveList.add(Hive(hivename.text.toString(), id, latitude_glob, longitude_glob))

                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                },
                                fun(i:Int, response: String) {
                                    runOnUiThread {
                                        kotlin.run {
                                            Toast.makeText(this, "Error adding hive to search list", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }).start()
                        }
                    }


                    hivelv.setOnItemClickListener { parent, view, position, id ->
                        Log.d(ContentValues.TAG, "Name: ${hiveList[position].name} Latitude: ${hiveList[position].latitude}, Longitude: ${hiveList[position].longitude}")

                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder
                            .setTitle("Confirm")
                            .setMessage("Do you want to mark this hive to be searched for or remove it?")
                            .setPositiveButton("Search Hive") { dialog, which ->
                                //  api call to add hive to be searched for
                                StartActivity.api.PutRequest("hive/save/search?id=${hiveList[position].id}",
                                    "",
                                    fun(response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Log.d(ContentValues.TAG, "Search for hive ${hiveList[position].name}, ${hiveList[position].id}")
                                                Toast.makeText(this, "Hive added to search list", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, Hives::class.java)
                                                startActivity(intent)
                                            }
                                        }
                                    },
                                    fun(i:Int, response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Toast.makeText(this, "Error adding hive to search list", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }).start()

                            }
                            .setNegativeButton("Remove") { dialog, which ->
                                // api call to stop searching for hive
                                StartActivity.api.DeleteRequest("hive?id=${hiveList[position].id}",
                                    "",
                                    fun(response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Log.d(ContentValues.TAG, "Deleted from saved ${hiveList[position].name}, ${hiveList[position].id}")
                                                Toast.makeText(this, "Hive added to search list", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, Hives::class.java)
                                                startActivity(intent)
                                            }
                                        }
                                    },
                                    fun(i:Int, response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Toast.makeText(this, "Error deleting hive from search list", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }).start()


                                val intent = Intent(this, Hives::class.java)
                                startActivity(intent)

                            }

                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }

                    val searched_hivelv = findViewById<ListView>(R.id.searchedhivelist)
                    val adapter_searched:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, hiveList_names_searched)
                    searched_hivelv.adapter = adapter_searched

                    searched_hivelv.setOnItemClickListener { parent, view, position, id ->
                        Log.d(ContentValues.TAG, "Name: ${hiveList_searched[position].name} Latitude: ${hiveList_searched[position].latitude}, Longitude: ${hiveList_searched[position].longitude}")

                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder
                            .setTitle("Confirm")
                            .setMessage("Do you want to stop searching for this hive?")
                            .setPositiveButton("Yes") { dialog, which ->

                                StartActivity.api.PutRequest("hive/save/found?id=${hiveList_searched[position].id}",
                                    "",
                                    fun(response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Log.d(ContentValues.TAG, "Stop Search for hive ${hiveList_searched[position].name}, ${hiveList_searched[position].id}")
                                                Toast.makeText(this, "Deleted hive from search list", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, Hives::class.java)
                                                startActivity(intent)
                                            }
                                        }
                                    },
                                    fun(i:Int, response: String) {
                                        runOnUiThread {
                                            kotlin.run {
                                                Toast.makeText(this, "Error deleting hive from search list", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }).start()

                            }
                            .setNegativeButton("No") { dialog, which ->
                                val intent = Intent(this, Hives::class.java)
                                startActivity(intent)

                            }

                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }

                }
            }
        }).start()

    }
    override fun onBackPressed() {

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("id", StartActivity.userGlob.id)
        intent.putExtra("username", StartActivity.userGlob.username)
        intent.putExtra("email", StartActivity.userGlob.email)
        intent.putExtra("phone", StartActivity.userGlob.phone)
        intent.putExtra("user_role", StartActivity.userGlob.user_role)
        startActivity(intent)

        // Finish the current activity to remove it from the back stack
        finish()
    }
}

class Hive(val name: String, val id:Int ,val latitude: Double?, val longitude: Double?)