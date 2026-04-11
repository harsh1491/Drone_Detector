package com.example.dronedetector

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

data class WifiData(val ssid: String, val freq: Int, val signal: Int)

class WifiScanner(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getWifiList(): List<WifiData> {
        // Local check to satisfy the compiler and prevent SecurityException
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return emptyList()
        }

        // Now the compiler knows we've checked the permission
        wifiManager.startScan()

        // 2. Read the results (will be the freshest available)
        val results = wifiManager.scanResults

        return results.map {
            WifiData(it.SSID ?: "<Hidden>", it.frequency, it.level)
        }.sortedByDescending { it.signal }
    }
}