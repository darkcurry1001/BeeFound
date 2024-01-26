package com.example.beefound.api

import android.util.Log
import com.example.beefound.StartActivity
import org.json.JSONArray
import org.json.JSONObject
import javax.security.auth.callback.Callback

class Middleware {

    fun getUser(callback: (user: User)->Unit): Thread{
        return StartActivity.api.GetRequest("user/", fun (response: String){
                val jsonObject = JSONObject(response)
                val user = User(
                    jsonObject.getInt("id"),
                    jsonObject.getString("username"),
                    jsonObject.getString("email"),
                    jsonObject.getString("phone"),
                    jsonObject.getString("user_role")
                )
                callback(user)
        })
    }


    companion object {
        fun getUser(callback: (user: User)->Unit): Thread{
            return StartActivity.api.GetRequest("user/", fun (response: String){
                val jsonObject = JSONObject(response)
                val userJson = jsonObject.getJSONObject("user")
                val user = User(
                    userJson.getInt("id"),
                    userJson.getString("username"),
                    userJson.getString("email"),
                    try {
                        userJson.getString("phone")
                    } catch (e: Exception) {
                        ""
                    },
                    userJson.getString("role")
                )
                callback(user)
            })
        }

        fun getHives(callback: (hivesFound: MutableList<Hive>, hivesNavigated: MutableList<Hive>, hivesSaved: MutableList<Hive>, hivesSearched: MutableList<Hive>)->Unit): Thread{
            return StartActivity.api.GetRequest("hive/", fun (response: String){
                val jsonObject = JSONObject(response)
                val found = jsonObject.get("found")
                val navigated = jsonObject.get("navigated")
                val saved = jsonObject.get("saved")
                val searched = jsonObject.get("searched")

                var hivesFound: MutableList<Hive> = mutableListOf()
                var hivesNavigated: MutableList<Hive> = mutableListOf()
                var hivesSaved: MutableList<Hive> = mutableListOf()
                var hivesSearched: MutableList<Hive> = mutableListOf()

                if (found is JSONArray){
                    for (idx in 0 until found.length()) {
                        val hiveJson = found.getJSONObject(idx)
                        if (hiveJson is JSONObject) {
                            val hiveFound = Hive(
                                hiveJson.getInt("ID"),
                                hiveJson.getString("CreatedAt"),
                                hiveJson.getString("UpdatedAt"),
                                hiveJson.getString("Longitude"),
                                hiveJson.getString("Latitude"),
                                hiveJson.getInt("UserID"),
                                hiveJson.getString("type"),
                                hiveJson.getJSONObject("User").getString("email")
                            )
                            hivesFound.add(hiveFound)
                            //callback(hiveFound)
                        }
                    }
                }

                if (navigated is JSONArray){
                    for (idx in 0 until navigated.length()) {
                        val hiveJson = navigated.getJSONObject(idx)
                        if (hiveJson is JSONObject) {
                            val hiveNavigated = Hive(
                                hiveJson.getInt("ID"),
                                hiveJson.getString("CreatedAt"),
                                hiveJson.getString("UpdatedAt"),
                                hiveJson.getString("Longitude"),
                                hiveJson.getString("Latitude"),
                                hiveJson.getInt("UserID"),
                                hiveJson.getString("type"),
                                hiveJson.getJSONObject("User").getString("email")
                            )
                            hivesNavigated.add(hiveNavigated)
                            //callback(hiveNavigated)
                        }
                    }
                }

                if (saved is JSONArray){
                    for (idx in 0 until saved.length()) {
                        val hiveJson = saved.getJSONObject(idx)
                        if (hiveJson is JSONObject) {
                            val hiveSaved = Hive(
                                hiveJson.getInt("ID"),
                                hiveJson.getString("CreatedAt"),
                                hiveJson.getString("UpdatedAt"),
                                hiveJson.getString("Longitude"),
                                hiveJson.getString("Latitude"),
                                hiveJson.getInt("UserID"),
                                hiveJson.getString("type"),
                                hiveJson.getString("name")
                                hiveJson.getJSONObject("User").getString("email")
                            )
                            hivesSaved.add(hiveSaved)
                            //callback(hiveSaved)
                        }
                    }
                }

                if (searched is JSONArray){
                    for (idx in 0 until searched.length()) {
                        val hiveJson = searched.getJSONObject(idx)
                        if (hiveJson is JSONObject) {
                            val hiveSearched = Hive(
                                hiveJson.getInt("ID"),
                                hiveJson.getString("CreatedAt"),
                                hiveJson.getString("UpdatedAt"),
                                hiveJson.getString("Longitude"),
                                hiveJson.getString("Latitude"),
                                hiveJson.getInt("UserID"),
                                hiveJson.getString("type"),
                                hiveJson.getString("name")
                                hiveJson.getJSONObject("User").getString("email")
                            )
                            hivesSearched.add(hiveSearched)
                            //callback(hiveSearched)
                        }
                    }
                }
                callback(hivesFound, hivesNavigated, hivesSaved, hivesSearched)
            })
        }
    }
}