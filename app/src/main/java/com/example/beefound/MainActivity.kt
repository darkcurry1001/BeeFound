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
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.beefound.api.Api
import com.example.beefound.api.Hive
import com.example.beefound.api.Middleware
import org.osmdroid.views.overlay.Marker
import java.io.File


class MainActivity : FragmentActivity()  {

    private val IMAGE_FILE_NAME: String = "test.jpg"

    private var photoFile: File = File("drawable/bees.jpg")

    lateinit var userName: String
    lateinit var userEmail: String
    lateinit var userPhone: String
    lateinit var userRole: String
    lateinit var hives_Found: MutableList<Hive>
    lateinit var hives_Navigated: MutableList<Hive>
    lateinit var hives_Saved: MutableList<Hive>
    lateinit var hives_Searched: MutableList<Hive>

    var userId: Int = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userName = intent.getStringExtra("username") ?: ""
        userEmail = intent.getStringExtra("email") ?: ""
        userPhone = intent.getStringExtra("phone") ?: ""
        userRole = intent.getStringExtra("user_role") ?: ""
        userId = intent.getIntExtra("id", 0)

        // get hives from data base
        Middleware.getHives(fun(hivesFound: MutableList<Hive>, hivesNavigated: MutableList<Hive>, hivesSaved: MutableList<Hive>, hivesSearched: MutableList<Hive>){
            runOnUiThread {
                kotlin.run {
                    Log.d("test", "gethives: ")
                    hives_Found = hivesFound
                    hives_Navigated = hivesNavigated
                    hives_Saved = hivesSaved
                    hives_Searched = hivesSearched

                    // only set after hives are loaded
                    setContentView(R.layout.activity_home)
                }
            }
        }).start()

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

}