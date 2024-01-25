package com.example.beefound.api

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class LocalStorageManager(private val context: Context) {

    fun saveStringToFile(fileName: String, data: String) {
        // Get the path to the app's internal files directory
        val filesDir: File = context.filesDir

        // Create a new file in the specified directory
        val file = File(filesDir, fileName)

        try {
            // Open a FileOutputStream to write into the file
            val outputStream = FileOutputStream(file)

            // Write the data to the file
            outputStream.write(data.toByteArray())

            // Close the FileOutputStream
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readStringFromFile(fileName: String): String {
        val result = StringBuilder()
        try {
            // Open a FileInputStream to read from the file
            val file = File(context.filesDir, fileName)
            val inputStream = FileInputStream(file)

            // Read the data from the file
            var content: Int
            while (inputStream.read().also { content = it } != -1) {
                result.append(content.toChar())
            }

            // Close the FileInputStream
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result.toString()
    }
}