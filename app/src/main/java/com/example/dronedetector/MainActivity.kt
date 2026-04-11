package com.example.dronedetector

import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.dronedetector.utils.PermissionManager
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null

    private var isCompassMode = false

    private lateinit var wifiScanner: WifiScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize MapLibre Engine
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)

        // 2. Setup MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // 3. Setup My Location Button click listener
        findViewById<ImageButton>(R.id.btnMyLocation).setOnClickListener {
            handleMyLocationClick()
        }

        findViewById<ImageButton>(R.id.btnCompassMode).setOnClickListener {
            toggleCompassMode()
        }

        wifiScanner = WifiScanner(this)

        findViewById<Button>(R.id.btnWifiList).setOnClickListener {
            if (PermissionManager.hasLocationPermission(this)) {
                displayWifiList()
            } else {
                // Reuse your existing permission request logic
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            }
        }

        // 4. Load the Map
        setupOfflineMap()
    }

    private fun setupOfflineMap() {
        if (PermissionManager.hasMapStoragePermission(this)) {
            val mapFile = File(Environment.getExternalStorageDirectory(), "offline_maps/HTMLMap.mbtiles")

            if (mapFile.exists()) {
                mapView.getMapAsync { map ->
                    this.mapLibreMap = map
                    val styleJson = MapStyleProvider.getOfflineStyleJson(mapFile.absolutePath)
                    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                        // HERE: Use the function to decide if we turn on the blue dot immediately
                        if (PermissionManager.hasLocationPermission(this)) {
                            enableLocationComponent(style)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "File not found: ${mapFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } else {
            PermissionManager.requestMapStoragePermission(this)
        }
    }

    private fun handleMyLocationClick() {
        if (PermissionManager.hasLocationPermission(this)) {
            val location = mapLibreMap?.locationComponent?.lastKnownLocation
            if (location != null) {
                val position = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 16.0
                )
                mapLibreMap?.animateCamera(position)
            } else {
                Toast.makeText(this, "Acquiring GPS signal...", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Request Location Permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }
    }

    private fun toggleCompassMode() {
        val locationComponent = mapLibreMap?.locationComponent ?: return

        if (!PermissionManager.hasLocationPermission(this)) {
            handleMyLocationClick() // This will trigger the permission request
            return
        }

        isCompassMode = !isCompassMode

        if (isCompassMode) {
            // Mode 2: Map rotates as you turn the tablet
            locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING_COMPASS
            locationComponent.renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
            Toast.makeText(this, "Compass Mode: ON", Toast.LENGTH_SHORT).show()
        } else {
            // Mode 1: North stays at the top
            locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.NONE
            locationComponent.renderMode = org.maplibre.android.location.modes.RenderMode.NORMAL

            // Reset map rotation to North
            val resetCamera = org.maplibre.android.camera.CameraUpdateFactory.bearingTo(0.0)
            mapLibreMap?.animateCamera(resetCamera)

            Toast.makeText(this, "Compass Mode: OFF (North Up)", Toast.LENGTH_SHORT).show()
        }
    }


    private fun displayWifiList() {
        val networks = wifiScanner.getWifiList()

        // Create a simple string to display
        val sb = StringBuilder()
        sb.append("SSID | Freq | Signal\n-------------------\n")
        networks.take(10).forEach { // Show top 10
            sb.append("${it.ssid} | ${it.freq}MHz | ${it.signal}dBm\n")
        }

        // Show it in a simple Popup Alert
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Available WiFi Networks")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionManager.hasLocationPermission(this)) {
            val locationComponent = mapLibreMap?.locationComponent

            // This configuration tells the engine to start looking for a fix immediately
            val activationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .useDefaultLocationEngine(true) // Crucial for getting the signal faster
                .build()

            locationComponent?.activateLocationComponent(activationOptions)
            locationComponent?.isLocationComponentEnabled = true

            // This makes the blue dot look like a "Tactical" compass
            locationComponent?.renderMode = RenderMode.COMPASS
        }
    }

    // --- LifeCycle Methods (Mandatory for MapLibre) ---
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // If user returned from settings after granting storage permission
        if (PermissionManager.hasMapStoragePermission(this)) {
            // Only re-setup if map isn't already loaded
            if (mapLibreMap == null) setupOfflineMap()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            // Use your function to verify the grant
            if (PermissionManager.hasLocationPermission(this)) {
                mapLibreMap?.getStyle { style ->
                    enableLocationComponent(style)
                    Toast.makeText(this, "Location Enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}