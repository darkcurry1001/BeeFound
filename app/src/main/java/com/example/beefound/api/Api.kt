package com.example.beefound.api

import android.util.Log
import android.widget.Toast
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Api {

    var BaseUrl: String = "http://192.168.0.42:3000/api/"

    fun Request(url: String, body: String, method:String):Thread{
        return Thread {
            val url = URL(BaseUrl + url)
            Log.d("test", "url: " + BaseUrl + url)
            Log.d("test", "body: " + body)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                ) // The format of the content we're sending to the server
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                ) // The format of response we want to get from the server
                connection.doInput = true
                connection.doOutput = true

                // Send the JSON we created
                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(body)
                outputStreamWriter.flush()

                Log.d("test", connection.responseCode.toString())

                if (connection.responseCode == 200) {
                    Log.d("test", "response ok")
                    val inputSystem = connection.inputStream
                    val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                    Log.d("test", inputStreamReader.readText())
                } else {
//                    Toast.makeText()
                    Log.d("test", "response !!NOT!! ok")
                    val inputSystem = connection.inputStream
                    val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                    Log.d("test", inputStreamReader.readText())
                }
            } catch (e: Exception) {
                Log.d("test", "err:" + e.message)
            }

            Log.d("test", "end")
        }
    }

    fun GetRequest(url: String, body: String): Thread {
        return Request(url, body, "GET")
    }

    fun PostRequest(url: String, body: String): Thread {
        return Request(url, body, "POST")
    }

    fun PutRequest(url: String, body: String): Thread {
        return Request(url, body, "PUT")
    }

    fun DeleteRequest(url: String, body: String): Thread {
        return Request(url, body, "DELETE")
    }




}