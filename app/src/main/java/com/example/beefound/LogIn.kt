package com.example.beefound

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.beefound.api.Middleware
import com.example.beefound.api.User
import org.json.JSONObject

class LogIn : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        val signUp: TextView = findViewById<TextView>(R.id.signUp)
        signUp.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        val logIn = findViewById<Button>(R.id.logInButton)
        logIn.setOnClickListener {
            // Start the SignUpActivity

            StartActivity.api.PostRequest("auth/login/",
                "{\"username\": \"${findViewById<TextView>(R.id.username).text}\"," +
                        "\"password\": \"${findViewById<TextView>(R.id.password).text}\"}",
                fun(response: String) {
                    runOnUiThread {
                        kotlin.run {
                            var json = JSONObject(response)
                            Log.d("test", "sessionToken: " + json.get("session_token"))
                            StartActivity.api.Login(
                                json.getString("session_token"),
                                json.getString("refresh_token")
                            )
                            Middleware.getUser(fun(user: User) {
                                runOnUiThread {
                                    kotlin.run {
                                        StartActivity.userGlob = user
                                        Log.d("test", "user: " + StartActivity.userGlob.username)
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("id", StartActivity.userGlob.id)
                                        intent.putExtra("username", StartActivity.userGlob.username)
                                        intent.putExtra("email", StartActivity.userGlob.email)
                                        intent.putExtra("phone", StartActivity.userGlob.phone)
                                        intent.putExtra("user_role", StartActivity.userGlob.user_role)
                                        Toast.makeText(this, "Logged in", Toast.LENGTH_LONG).show()
                                        startActivity(intent)
                                    }
                                }
                            }).start()
                        }
                    }
                },
                fun(i:Int, response: String) {
                    runOnUiThread {
                        kotlin.run {
                            Toast.makeText(this, "Wrong username or password", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }).start()
        }
    }
}