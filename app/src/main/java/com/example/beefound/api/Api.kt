package com.example.beefound.api

import android.content.Context
import android.util.Log
import androidx.lifecycle.asLiveData
import android.widget.Toast
import androidx.compose.material3.contentColorFor
import com.example.beefound.StartActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Api {
    var BaseUrl: String = "http://skeller.at:3000/api/"

    var SessionToken: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDQzOTMzNDQsInJvbGUiOiJ1c2VyIiwidHlwZSI6InNlc3Npb24iLCJ1c2VyX2lkIjoxfQ.9s_Kg3HYD8qpkknEwGtHjoX-z_06cJtZu6XdY0a-Ck8"
    var RefreshToken: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDQ5OTQ1NDQsInR5cGUiOiJyZWZyZXNoIiwidXNlcl9pZCI6MX0.TMauko0fWg6lVtoerDb6GRngvSOpQ7GaTcHa8ZE75kg"

    companion object{
        lateinit var LocalStorageManager: LocalStorageManager
    }

    constructor(context: Context){
        LocalStorageManager = LocalStorageManager(context)

        SessionToken = LocalStorageManager.readStringFromFile("sessionToken")
        RefreshToken = LocalStorageManager.readStringFromFile("refreshToken")
    }


    fun Login(sT: String, rT: String){
        SessionToken = sT
        RefreshToken = rT
        LocalStorageManager.saveStringToFile("sessionToken", sT)
        LocalStorageManager.saveStringToFile("refreshToken", rT)
    }

    fun Logout(){
        SessionToken = ""
        RefreshToken = ""
        LocalStorageManager.saveStringToFile("sessionToken", "")
        LocalStorageManager.saveStringToFile("refreshToken", "")
    }


//    ToDo: handle returns (threads, saving tokens)
    fun Request(url: String, body: String, method:String, token:String,
                okCallback: (String) -> Unit = fun(_){}, errCallback: (String) -> Unit = fun(_){},):Thread{
        return Thread {
            val url = URL(BaseUrl + url)
            Log.d("test", "url: " + url)
            Log.d("test", "body: " + body)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Token", SessionToken)
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                ) // The format of the content we're sending to the server
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                ) // The format of response we want to get from the server

                // Send the JSON we created
                if (body != ""){
                    connection.doOutput = true
                    connection.doInput = true
                    val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                    outputStreamWriter.write(body)
                    outputStreamWriter.flush()
                }

                Log.d("test", connection.responseCode.toString())

                if (connection.responseCode == 200) {
                    Log.d("test", "response ok")
                    val inputSystem = connection.inputStream
                    val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                    val response = inputStreamReader.readText()
                    Log.d("test", response)
                    okCallback(response)
                }else if(connection.responseCode == 401){
//                    Request("auth/refresh", "", "GET", RefreshToken, fun(s:String){}).start()
                    //ToDo: refresh token
                    Log.d("test", "response 401")
                } else {
                    Log.d("test", "response !!NOT!! ok")
                    val errorSystem = connection.errorStream
                    val errorStreamReader = InputStreamReader(errorSystem, "UTF-8")

                    Log.d("test", errorStreamReader.readText())
                }
            } catch (e: Exception) {
                Log.d("test", "Exception err:" + e.message)
            }

            Log.d("test", "end")
        }
    }

    fun GetRequest(url: String, callback: (String) -> Unit, errorCallback: ()->Unit = fun(){}): Thread {
        return Request(url, "", "GET", SessionToken, callback)
    }

    fun PostRequest(url: String, body: String, callback: (String) -> Unit, errorCallback: (String)->Unit = fun(_){}): Thread {
        return Request(url, body, "POST", SessionToken, callback, errorCallback)
    }

    fun PutRequest(url: String, body: String, callback: (String) -> Unit, errorCallback: ()->Unit = fun(){}): Thread {
        return Request(url, body, "PUT", SessionToken, callback)
    }

    fun DeleteRequest(url: String, body: String, callback: (String) -> Unit, errorCallback: ()->Unit = fun(){}): Thread {
        return Request(url, body, "DELETE", SessionToken, callback)
    }
}
