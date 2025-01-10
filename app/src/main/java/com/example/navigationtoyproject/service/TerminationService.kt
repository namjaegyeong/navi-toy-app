package com.example.navigationtoyproject.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.navigationtoyproject.api.di.RetrofitClient
import com.example.navigationtoyproject.datastore.DataStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TerminationService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        // Initialize any resources if needed
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Not used in this example
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Called when the task is removed from recent apps

        // Perform your API call here
        performTerminationApiCall()

        Thread.sleep(3000)

        // Optionally, stop the service
        stopSelf()

        super.onTaskRemoved(rootIntent)
    }

    private fun performTerminationApiCall() {
        serviceScope.launch {
            try {
                // Access DataStore to get mapVersion
                val oldMapVersion = DataStoreHelper.getMapVersion(applicationContext).first() ?: "0.0.0"

                // Make the API call
                val response = RetrofitClient.apiService.findRecentMapVersion()

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("TerminationService", "API call successful: $it")
                        // Handle the successful response as needed

                        val newMapVersion = it.mapVersion
                        val preprocessedOldValue = extractFinalNumber(oldMapVersion)
                        val preprocessedNewValue = extractFinalNumber(newMapVersion)

                        if (preprocessedOldValue!! < preprocessedNewValue!!) {
                            // Update mapVersion in DataStore
                            DataStoreHelper.setMapVersion(applicationContext, newMapVersion)
                            Log.d("TerminationService", "mapVersion updated to $newMapVersion")

                            // Set requestFlag to 1
                            DataStoreHelper.setRequestFlag(applicationContext, 1)
                            Log.d("TerminationService", "Request flag set to 1")
                        }
                    }
                } else {
                    Log.e("TerminationService", "API call failed: ${response.code()} ${response.message()}")
                    // Handle the error response as needed
                }
            } catch (e: Exception) {
                Log.e("TerminationService", "API call exception: ${e.localizedMessage}")
                // Handle exceptions as needed
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d("TerminationService", "Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    private fun extractFinalNumber(version: String): Int? {
        val parts = version.split(".")
        val lastPart = parts.lastOrNull()
        return lastPart?.toIntOrNull()
    }
}