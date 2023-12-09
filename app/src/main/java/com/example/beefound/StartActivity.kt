package com.example.beefound

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.beefound.R

class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)


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