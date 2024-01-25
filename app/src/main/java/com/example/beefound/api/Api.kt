package com.example.beefound.api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.asLiveData
import android.widget.Toast
import androidx.compose.material3.contentColorFor
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
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
    var BaseUrl: String = "http://192.168.0.42:3000/api/"

    var SessionToken: String =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDYxODMwMDUsInJvbGUiOiJiZWVrZWVwZXIiLCJ0eXBlIjoic2Vzc2lvbiIsInVzZXJfaWQiOjF9.SNVlRDIz2hVZyepSD5b1zlz1CCa5q9Syz9xVp8ZYWno"
    var RefreshToken: String =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDY3ODg3NDEsInR5cGUiOiJyZWZyZXNoIiwidXNlcl9pZCI6MX0.nxVd1f0cqmvDiZbxN_d38d2ZtV6yplA1uKJP3z5L-Uc"

    lateinit var context: Context
    var refreshOkCallback: () -> Unit = fun() {}
    var refreshErrCallback: () -> Unit = fun() {}

    companion object {
        lateinit var LocalStorageManager: LocalStorageManager
    }

    constructor(context: Context, refreshOkCallback: () -> Unit, refreshErrCallback: () -> Unit) {
        this.context = context
        LocalStorageManager = LocalStorageManager(context)
        this.refreshOkCallback = refreshOkCallback
        this.refreshErrCallback = refreshErrCallback

//        SessionToken = LocalStorageManager.readStringFromFile("sessionToken")
//        RefreshToken = LocalStorageManager.readStringFromFile("refreshToken")
    }


    fun Login(sT: String, rT: String) {
        SessionToken = sT
        RefreshToken = rT
        LocalStorageManager.saveStringToFile("sessionToken", sT)
        LocalStorageManager.saveStringToFile("refreshToken", rT)
    }

    fun Logout() {
        SessionToken = ""
        RefreshToken = ""
        LocalStorageManager.saveStringToFile("sessionToken", "")
        LocalStorageManager.saveStringToFile("refreshToken", "")
    }


    //    ToDo: handle returns (threads, saving tokens)
    fun Request(
        urlString: String, body: String, method: String, token: String = SessionToken,
        okCallback: (String) -> Unit = fun(_) {},
        errCallback: (Int, String) -> Unit = fun(_, _) {}
    ): Thread {
        return Thread {
            val url = URL(BaseUrl + urlString)
            Log.d("test", "url: " + url)
            Log.d("test", "body: " + body)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Token", token)
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                ) // The format of the content we're sending to the server
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                ) // The format of response we want to get from the server

                // Send the JSON we created
                if (body != "") {
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
                } else {
                    if (connection.responseCode == 401) {
                        if (RefreshToken != token) {
                            RefreshRequest(urlString, body, method, okCallback, errCallback)
                        }
                        Log.d("test", "response 401")
                    } else {
                        Log.d("test", "response !!NOT!! ok")
                    }
                    errCallback(connection.responseCode, connection.responseMessage)
                }
            } catch (e: Exception) {
                Log.d("test", "Exception err:" + e.message)
            }

            Log.d("test", "end")
        }
    }

    fun RefreshRequest(url: String, body: String, method: String,
                       okCallback: (String) -> Unit = fun(_) {},
                       errCallback: (Int, String) -> Unit = fun(_, _) {}) {
        Log.d("test", "refresh request")
        Request("auth/refresh", "", "GET", RefreshToken,
            fun(s: String) {
                val jsonObject = JSONObject(s)
                Login(
                    jsonObject.getString("session_token"),
                    jsonObject.getString("refresh_token")
                )
                Request(url, body, method,
                    okCallback=okCallback, errCallback=errCallback).start()
            },
            fun(code: Int, s: String) {
                Logout()
                refreshErrCallback()
            }).start()
    }

    fun GetRequest(
        url: String,
        callback: (String) -> Unit,
        errorCallback: (Int, String) -> Unit = fun(_, _) {}
    ): Thread {
        return Request(url, "", "GET", okCallback = callback, errCallback = errorCallback)
    }

    fun PostRequest(
        url: String,
        body: String,
        callback: (String) -> Unit,
        errorCallback: (Int, String) -> Unit = fun(_, _) {}
    ): Thread {
        return Request(url, body, "POST", okCallback = callback, errCallback = errorCallback)
    }

    fun PutRequest(
        url: String,
        body: String,
        callback: (String) -> Unit,
        errorCallback: (Int, String) -> Unit = fun(_, _) {}
    ): Thread {
        return Request(url, body, "PUT", okCallback = callback, errCallback = errorCallback)
    }

    fun DeleteRequest(
        url: String,
        body: String,
        callback: (String) -> Unit,
        errorCallback: (Int, String) -> Unit = fun(_, _) {}
    ): Thread {
        return Request(url, body, "DELETE", okCallback = callback, errCallback = errorCallback)
    }
}
