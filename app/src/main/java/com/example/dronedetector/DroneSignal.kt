package com.example.dronedetector

data class DroneSignal(
    val frequency: Double,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val deviceType: String
)