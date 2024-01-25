package com.example.beefound

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.beefound.R
import com.example.beefound.api.Api
import com.example.beefound.api.Middleware
import com.example.beefound.api.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class StartActivity : Activity() {
    companion object {
        lateinit var api: Api
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        api = Api(this)

        api.GetRequest("auth/test/",
            fun(response: String) {
                Middleware.getUser(fun(user: User) {
                    runOnUiThread {
                        kotlin.run {
                            Log.d("test", "user: " + user.username)
                        }
                    }
                }).start()
                Toast.makeText(this, "Logged in", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
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