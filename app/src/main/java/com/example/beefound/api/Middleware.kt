package com.example.beefound.api

import com.example.beefound.StartActivity
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

}