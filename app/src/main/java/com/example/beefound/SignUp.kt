package com.example.beefound

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.example.beefound.api.Api
import com.example.beefound.api.Middleware
import com.example.beefound.api.User
import com.example.beefound.databinding.ActivityLogInBinding
import com.example.beefound.databinding.ActivitySignUpBinding
import kotlinx.coroutines.awaitAll
import org.json.JSONObject


class SignUp : Activity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val logIn = findViewById<TextView>(R.id.logIn)
        logIn.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
        var success = false

        val signUp = findViewById<Button>(R.id.signUpButton)
        val username = findViewById<TextView>(R.id.username)
        val email = findViewById<TextView>(R.id.email)
        val phone = findViewById<TextView>(R.id.phone)
        val user_role_reg = findViewById<RadioButton>(R.id.regularUser)
        val user_role_beekeeper = findViewById<RadioButton>(R.id.beekeeper)
        val psw = findViewById<TextView>(R.id.password)
        val pswConfirm = findViewById<TextView>(R.id.confirmPassword)


        // check input for signup
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
            jsonObject.put("email",email.text.toString())
            jsonObject.put("phone", phone.text.toString())
            jsonObject.put("role", if (user_role_beekeeper.isChecked) "beekeeper" else "user")
            jsonObject.put("password", psw.text.toString())

            // send signup request to api and start main activity
            StartActivity.api.PostRequest("auth/signup/", jsonObject.toString(), fun (response: String){
                runOnUiThread {
                    kotlin.run {
                        val jsonObject = JSONObject(response)
                        val sessionT:String = jsonObject.get("session_token").toString()
                        val refreshT:String = jsonObject.get("refresh_token").toString()

                        StartActivity.api.Login(sessionT, refreshT)

                        intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                }}, fun (i:Int, s: String){
                    runOnUiThread {
                        kotlin.run {
                            Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
                        }
                    }}
            ).start()
        }
    }
}