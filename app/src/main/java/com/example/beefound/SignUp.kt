package com.example.beefound

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.example.beefound.api.Api
import org.json.JSONObject


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
        val psw = findViewById<TextView>(R.id.password)
        val pswConfirm = findViewById<TextView>(R.id.confirmPassword)


        signUp.setOnClickListener {
            val user = username.text.toString()
            val password = psw.text.toString()
            if (user.isEmpty()) {
                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if ((user.length < 4) or (user.length > 50)) {
                Toast.makeText(this, "Username has to be between 4 and 50 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if ((password.length < 6) or (password.length > 50)) {
                Toast.makeText(this, "Password has to be between 6 and 50 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pswConfirm.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter password a second time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != pswConfirm.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonObject = JSONObject()
            jsonObject.put("username", username.text.toString())
            jsonObject.put("email", email.text.toString())
            jsonObject.put("phone", phone.text.toString())
            jsonObject.put("role", if (user_role.isChecked) "beekeeper" else "user")
            jsonObject.put("password", psw.text.toString())

            val api = Api()
            api.PostRequest("auth/signup/", jsonObject.toString()).start()
        }
    }
}