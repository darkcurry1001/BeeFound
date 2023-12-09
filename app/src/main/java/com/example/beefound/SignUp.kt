package com.example.beefound

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class SignUp : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val logIn: TextView = findViewById<TextView>(R.id.logIn)
        logIn.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }
}