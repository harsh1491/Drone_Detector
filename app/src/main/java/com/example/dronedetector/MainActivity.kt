package com.example.dronedetector

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.example.dronedetector.R

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//
//
//            // Convert 24dp to actual pixels to add to the system padding
//            val customPadding = (20 * resources.displayMetrics.density).toInt()
//
//            // Add the system bar spacing PLUS your 24dp custom padding
//            v.setPadding(
//                systemBars.left + customPadding,
//                systemBars.top + customPadding,
//                systemBars.right + customPadding,
//                systemBars.bottom + customPadding
//            )
//            insets
//        }
    }
}