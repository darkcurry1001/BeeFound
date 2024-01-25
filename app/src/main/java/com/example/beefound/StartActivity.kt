package com.example.beefound

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.beefound.api.Api
import com.example.beefound.api.Middleware
import com.example.beefound.api.User

class StartActivity : Activity() {
    companion object {
        lateinit var api: Api
        lateinit var userGlob : User
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        api = Api(this,
            refreshOkCallback = fun (){
                runOnUiThread {
                    kotlin.run {
                        Toast.makeText(this, "Refreshed", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            },
            refreshErrCallback = fun (){
            runOnUiThread {
                kotlin.run {
                    Toast.makeText(this, "Session expired", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, StartActivity::class.java)
                    startActivity(intent)
                }
            }
        })

        api.GetRequest("auth/test/",
            fun(response: String) {
                Middleware.getUser(fun(user: User) {
                    runOnUiThread {
                        kotlin.run {
                            userGlob = user
                            Log.d("test", "user: " + userGlob.username)
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("id", userGlob.id)
                            intent.putExtra("username", userGlob.username)
                            intent.putExtra("email", userGlob.email)
                            intent.putExtra("phone", userGlob.phone)
                            intent.putExtra("user_role", userGlob.user_role)
                            Toast.makeText(this, "Logged in", Toast.LENGTH_LONG).show()
                            startActivity(intent)
                        }
                    }
                }).start()
            }).start()

        val signUpButton: Button = findViewById<Button>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        val logInButton: Button = findViewById<Button>(R.id.logInButton)
        logInButton.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }
}