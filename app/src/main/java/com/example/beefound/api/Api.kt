package com.example.beefound.api

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.contentColorFor
import com.example.beefound.MainActivity
import com.example.beefound.StartActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Api {
  //var BaseUrl: String = "http://192.168.0.42:3000/api/"

    var BaseUrl: String = "http://skeller.at:3000/api/"
  
    var SessionToken: String = ""
    var RefreshToken: String = ""

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

        SessionToken = LocalStorageManager.readStringFromFile("sessionToken")
        RefreshToken = LocalStorageManager.readStringFromFile("refreshToken")
    }

    fun Login(sT: String, rT: String){

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
        errCallback: (Int, String) -> Unit = fun(_, _) {},
        sendType: String = "application/json", recieveType: String = "application/json"
    ): Thread {
        return Thread {
            val url = URL(BaseUrl + urlString)
            Log.d("test", "url: " + url)
            Log.d("test", "body: " + body)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Token", token)
                connection.setRequestProperty(
                    "Content-Type",
                    sendType
                ) // The format of the content we're sending to the server
                connection.setRequestProperty(
                    "Accept",
                    recieveType
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
                    Log.d("test", "response !!NOT!! ok")
                    Log.d("test", InputStreamReader(connection.errorStream).readText())
                    if (connection.responseCode == 401) {
                        if (RefreshToken != token) {
                            RefreshRequest(urlString, body, method, okCallback, errCallback)
                        }
                        Log.d("test", "response 401")
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
        return Request(url, body, "DELETE", okCallback = callback, errCallback = errorCallback
        )
    }

    fun sendMultipartRequest(jsonData: String, imageFile: File?, serverUrl: String) {
        Log.d("test", "FileSend")
        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW" // Replace with your desired boundary

        val url = URL(BaseUrl + serverUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Token", SessionToken)
        connection.doOutput = true
        connection.doInput = true
        connection.useCaches = false
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        Log.d("test", "FileSend2")
        try {
            val outputStream = DataOutputStream(connection.outputStream)

            // Add JSON part
            Log.d("test", jsonData)
            addFormField("json", jsonData, boundary, outputStream)

            // Add image file part
            Log.d("test", (imageFile != null).toString())
            if (imageFile != null) {
                addFilePart("img", imageFile, boundary, outputStream)
            }

            // End of multipart/form-data
            outputStream.writeBytes("--$boundary--\r\n")

            outputStream.flush()
            outputStream.close()

            // Get the response code
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Successfully sent the request
                // Read the response if needed
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String?
                val response = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                // Handle the response here
            } else {
                // Handle error
                if (connection.responseCode == 401) {
                    RefreshRequest(serverUrl, jsonData, "POST", fun(s: String) {
                        sendMultipartRequest(jsonData, imageFile, serverUrl)
                    })
                }
                Log.d("test", "File Send Error")
                Log.d("test", connection.responseMessage)
                Log.d("test", InputStreamReader(connection.errorStream).readText())
            }
        } finally {
            Log.d("test", "FileSend End")
            connection.disconnect()
        }
    }

    private fun addFormField(fieldName: String, value: String, boundary: String, outputStream: DataOutputStream) {
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"\r\n\r\n")
        outputStream.writeBytes(value + "\r\n")
    }

    private fun addFilePart(fieldName: String, uploadFile: File, boundary: String, outputStream: DataOutputStream) {
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${uploadFile.name}\"\r\n")
        outputStream.writeBytes("Content-Type: application/octet-stream\r\n\r\n")

        val fileInputStream = FileInputStream(uploadFile)
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.writeBytes("\r\n")
        fileInputStream.close()
    }

}


