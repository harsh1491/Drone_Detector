package com.example.dronedetector

import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.dronedetector.utils.PermissionManager
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

class MainActivity : AppCompatActivity() {

    // UI and Map Components
    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null
    private var isCompassMode = false
    private lateinit var wifiScanner: WifiScanner
    private var droneMarker: org.maplibre.android.annotations.Marker? = null

    // Hardware Manager
    private lateinit var sdrManager: SdrManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize MapLibre Engine
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_main)

        // 2. Setup View references and Scanners
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        wifiScanner = WifiScanner(this)

        // 3. Initialize SdrManager (ONE BLOCK ONLY)
        sdrManager = SdrManager(this) { signal ->
            runOnUiThread {
                // Update the UI Table with real-time stats including deviceType
                updateThreatTable(signal.frequency, signal.altitude, signal.deviceType)

                // Update the Drone Position on the Map (only if GPS is valid)
                if (signal.latitude != 0.0 && signal.longitude != 0.0) {
                    updateDroneMarker(signal.latitude, signal.longitude)
                }
            }
        }

        // 4. Start the SDR Hardware connection (Call this AFTER defining sdrManager)
        sdrManager.initConnection()

        // 5. Load Offline Map Data
        setupOfflineMap()

        // 6. Setup Button Click Listeners
        findViewById<ImageButton>(R.id.btnMyLocation).setOnClickListener {
            handleMyLocationClick()
        }

        findViewById<ImageButton>(R.id.btnCompassMode).setOnClickListener {
            toggleCompassMode()
        }

        findViewById<Button>(R.id.btnWifiList).setOnClickListener {
            if (PermissionManager.hasLocationPermission(this)) {
                displayWifiList()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            }
        }

        findViewById<Button>(R.id.btnResetCache).setOnClickListener {
            resetSystem()
        }
    }

    private fun updateThreatTable(freq: Double, altitude: Double, deviceType: String) {
        val tl = findViewById<TableLayout>(R.id.tableLayout) ?: return

        val freqString = String.format("%.2f MHz", freq)
        val powerString = String.format("%.1f m", altitude) // Using alt for power/height
        val bearingString = "---" // Placeholder for later

        val tacticalColor = android.graphics.Color.parseColor("#0083e5")

        if (tl.childCount <= 1) {
            // --- CREATE NEW ROW ---
            val newRow = TableRow(this)

            val tvFreq = createTacticalTextView(freqString, tacticalColor)
            val tvPower = createTacticalTextView(powerString, tacticalColor)
            val tvBearing = createTacticalTextView(bearingString, tacticalColor)
            val tvDevice = createTacticalTextView(deviceType, tacticalColor)

            newRow.addView(tvFreq)
            newRow.addView(tvPower)
            newRow.addView(tvBearing)
            newRow.addView(tvDevice)

            tl.addView(newRow, 1)
        } else {
            // --- UPDATE EXISTING ROW ---
            val row = tl.getChildAt(1) as? TableRow
            if (row != null && row.childCount >= 4) {
                (row.getChildAt(0) as? TextView)?.text = freqString
                (row.getChildAt(1) as? TextView)?.text = powerString
                (row.getChildAt(2) as? TextView)?.text = bearingString
                (row.getChildAt(3) as? TextView)?.text = deviceType
            }
        }
    }

    // Helper function to keep the code clean and consistent
    private fun createTacticalTextView(content: String, color: Int): TextView {
        return TextView(this).apply {
            text = content
            setPadding(8, 8, 8, 8)
            setTextColor(color)
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE // Tactical look

        }
    }

    private fun updateDroneMarker(lat: Double, lon: Double) {
        val pos = LatLng(lat, lon)

        if (droneMarker == null) {
            try {
                val iconFactory = org.maplibre.android.annotations.IconFactory.getInstance(this)

                // Get original image and resize it
                val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.drone)
                val bitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 80, 80, false)
                val droneIcon = iconFactory.fromBitmap(resizedBitmap)

                droneMarker = mapLibreMap?.addMarker(org.maplibre.android.annotations.MarkerOptions()
                    .position(pos)
                    .icon(droneIcon)
                    .title("Target Drone"))

            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to standard marker
                droneMarker = mapLibreMap?.addMarker(org.maplibre.android.annotations.MarkerOptions().position(pos))
            }
        } else {
            droneMarker?.position = pos
        }
    }

    private fun setupOfflineMap() {
        if (PermissionManager.hasMapStoragePermission(this)) {
            val mapFile = File(Environment.getExternalStorageDirectory(), "offline_maps/HTMLMap.mbtiles")

            if (mapFile.exists()) {
                mapView.getMapAsync { map ->
                    this.mapLibreMap = map
                    val styleJson = MapStyleProvider.getOfflineStyleJson(mapFile.absolutePath)
                    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                        if (PermissionManager.hasLocationPermission(this)) {
                            enableLocationComponent(style)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Map file not found: ${mapFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } else {
            PermissionManager.requestMapStoragePermission(this)
        }
    }

    private fun handleMyLocationClick() {
        if (PermissionManager.hasLocationPermission(this)) {
            val location = mapLibreMap?.locationComponent?.lastKnownLocation
            if (location != null) {
                val position = CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16.0)
                mapLibreMap?.animateCamera(position)
            } else {
                Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun toggleCompassMode() {
        val locationComponent = mapLibreMap?.locationComponent ?: return
        if (!PermissionManager.hasLocationPermission(this)) return

        isCompassMode = !isCompassMode
        if (isCompassMode) {
            locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING_COMPASS
            locationComponent.renderMode = RenderMode.COMPASS
            Toast.makeText(this, "Compass Mode: ON", Toast.LENGTH_SHORT).show()
        } else {
            locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.NONE
            locationComponent.renderMode = RenderMode.NORMAL
            mapLibreMap?.animateCamera(CameraUpdateFactory.bearingTo(0.0))
            Toast.makeText(this, "Compass Mode: OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayWifiList() {
        val networks = wifiScanner.getWifiList()
        val sb = StringBuilder().append("SSID | Freq | Signal\n-------------------\n")
        networks.take(10).forEach {
            sb.append("${it.ssid} | ${it.freq}MHz | ${it.signal}dBm\n")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("WiFi Scan (Tactical)")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionManager.hasLocationPermission(this)) {
            val locationComponent = mapLibreMap?.locationComponent
            val activationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .useDefaultLocationEngine(true)
                .build()
            locationComponent?.activateLocationComponent(activationOptions)
            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.renderMode = RenderMode.COMPASS
        }
    }

    private fun resetSystem() {
        // 1. Clear the Map Marker
        droneMarker?.let {
            mapLibreMap?.removeMarker(it)
            droneMarker = null // Crucial so that the next detection creates a fresh marker
        }

        // 2. Clear the Threat Log Table
        val tl = findViewById<TableLayout>(R.id.tableLayout)
        if (tl != null) {
            // We keep the header (index 0), so we remove all views from index 1 onwards
            val childCount = tl.childCount
            if (childCount > 1) {
                tl.removeViews(1, childCount - 1)
            }
        }

        Toast.makeText(this, "System Cache Cleared", Toast.LENGTH_SHORT).show()
    }

    // --- Mandatory Map Lifecycle Methods ---
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (mapLibreMap == null) setupOfflineMap()
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