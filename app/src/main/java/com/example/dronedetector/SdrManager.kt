package com.example.dronedetector

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.util.Log // Added Import
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.json.JSONObject

class SdrManager(private val context: Context, private val onDataReceived: (DroneSignal) -> Unit) {
    private var usbSerialPort: UsbSerialPort? = null
    private val baudRate = 115200

    fun initConnection() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)

        if (availableDrivers.isEmpty()) {
            Log.d("SDR_DEBUG", "No USB Serial devices found") // Added Log
            return
        }

        val driver = availableDrivers[0]
        if (!manager.hasPermission(driver.device)) {
            val flags = PendingIntent.FLAG_IMMUTABLE
            val intent = PendingIntent.getBroadcast(context, 0, Intent("com.android.example.USB_PERMISSION"), flags)
            manager.requestPermission(driver.device, intent)
            return
        }

        val connection = manager.openDevice(driver.device) ?: return
        val port = driver.ports[0]

        try {
            port.open(connection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            Log.d("SDR_DEBUG", "Serial Port Opened Successfully") // Added Log
            startReading()
        } catch (e: Exception) {
            Log.e("SDR_DEBUG", "Error opening port: ${e.message}") // Added Log
            e.printStackTrace()
        }
    }

    private fun startReading() {
        Thread {
            val buffer = ByteArray(4096)
            var accumulator = ""
            while (true) {
                try {
                    val len = usbSerialPort?.read(buffer, 1000) ?: 0
                    if (len > 0) {
                        val chunk = String(buffer, 0, len)

                        // --- 1. Log Raw Data from SDR ---
                        Log.d("SDR_RAW", "RAW CHUNK: $chunk")

                        accumulator += chunk
                        if (accumulator.contains("\n")) {
                            val lines = accumulator.split("\n")
                            for (i in 0 until lines.size - 1) {
                                parseAndSend(lines[i].trim())
                            }
                            accumulator = lines.last()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SDR_DEBUG", "Read Error: ${e.message}") // Added Log
                    break
                }
            }
        }.start()
    }

    private fun parseAndSend(rawString: String) {
        try {
            val jsonStart = rawString.indexOf("{")
            if (jsonStart == -1) return
            val json = JSONObject(rawString.substring(jsonStart))

            val signal = DroneSignal(
                frequency = json.optDouble("freq", 0.0),
                altitude = if (json.has("altitude")) json.optDouble("altitude", 0.0) else json.optDouble("heigth", 0.0),
                latitude = json.optDouble("drone_lat", 0.0),
                longitude = json.optDouble("drone_lon", 0.0),
                deviceType = json.optString("device_type", "Unknown")
            )

            // --- 2. Log Parsed Signal ---
            Log.d("SDR_DEBUG", "Parsed Signal: Freq=${signal.frequency}, Alt=${signal.altitude}")

            onDataReceived(signal)
        } catch (e: Exception) {
            Log.e("SDR_DEBUG", "Parse Error: ${e.message}") // Added Log
            e.printStackTrace()
        }
    }
}