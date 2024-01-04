package com.example.beefound

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ProfileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val save = findViewById<Button>(R.id.btn_save)
        save.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, HomeFragment::class.java)
            startActivity(intent)
        }

        val back = findViewById<TextView>(R.id.back)
        back.setOnClickListener {
            // Start the SignUpActivity
            val intent = Intent(this, HomeFragment::class.java)
            startActivity(intent)
        }
    }
}
