package com.engelsimmanuel.cardinfofinder

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        const val CAMERA_REQUEST_CODE: Int = 1997
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        when (ActivityCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            true -> {
                // use camera hardware here
            }
            false -> {
                // request user permisssion to access camera
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_CODE
                )
            }
        }

    }
}