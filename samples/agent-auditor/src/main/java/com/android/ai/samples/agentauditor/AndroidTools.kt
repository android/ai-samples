package com.android.ai.samples.agentauditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import androidx.core.net.toUri

class AndroidTools @Inject constructor(@param:ApplicationContext private val context: Context) {

    /**
     * Tool: get_device_location
     * Description: Gets the current location of the user's Android device (city name).
     * Hardcoded to return "San Francisco" for the demo.
     */
    suspend fun getDeviceLocation(): String {
        // Simulate a slight delay as if calling location services
        delay(500)
        return "San Francisco"
    }

    /**
     * Tool: open_maps
     * Description: Opens the maps application to search for a location.
     */
    fun openMaps(locationName: String) {
        val gmmIntentUri = "geo:0,0?q=${Uri.encode(locationName)}".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        // Ensure the intent resolves before starting
        mapIntent.setPackage("com.google.android.apps.maps")
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Start activity if possible, otherwise let system handle generic geo intent
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
             val genericIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
             genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             context.startActivity(genericIntent)
        }
    }
}
