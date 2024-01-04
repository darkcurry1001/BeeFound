package com.example.beefound

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import com.example.beefound.api.Api
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class SignUp : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val logIn = findViewById<TextView>(R.id.logIn)
        logIn.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }

        val signUp = findViewById<Button>(R.id.signUpButton)
        val username = findViewById<TextView>(R.id.username)
        val email = findViewById<TextView>(R.id.email)
        val phone = findViewById<TextView>(R.id.phone)
        val user_role = findViewById<Switch>(R.id.userType)
        val psw = findViewById<TextView>(R.id.signUpButton)


        signUp.setOnClickListener {
            var body = "{"
            body += "\"username\":\"${username.text}\","
            body += "\"email\":\"${email.text}\","
            if(!phone.text.isEmpty()){
                body += "\"phone\":\"${phone.text}\","
            }
            body += "\"role\":\"${if (user_role.isChecked) "beekeeper" else "user"}\","
            body += "\"password\":\"${psw.text}\""
            body += "}"

            var api = Api()
            api.PostRequest("auth/signup/", body).start()
        }
    }


}