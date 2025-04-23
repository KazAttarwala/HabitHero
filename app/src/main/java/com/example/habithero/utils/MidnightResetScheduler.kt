package com.example.habithero.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.habithero.workers.MidnightResetWorker
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Utility class to schedule the midnight reset worker
 */
class MidnightResetScheduler(private val context: Context) {
    
    private val TAG = "MidnightResetScheduler"
    private val MIDNIGHT_RESET_WORK_NAME = "midnight_habit_reset"
    
    /**
     * Log detailed timezone information for debugging
     */
    fun logTimezoneInfo() {
        val currentTimezone = TimeZone.getDefault()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.time
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        Log.d(TAG, "⏰ Current timezone debugging info:")
        Log.d(TAG, "⏰ Timezone ID: ${currentTimezone.id}")
        Log.d(TAG, "⏰ Display name: ${currentTimezone.displayName}")
        Log.d(TAG, "⏰ Current time: $currentTime")
        Log.d(TAG, "⏰ Timezone offset: ${currentTimezone.rawOffset / (60 * 60 * 1000)}h")
        Log.d(TAG, "⏰ In daylight time: ${currentTimezone.inDaylightTime(currentTime)}")
        Log.d(TAG, "⏰ Hour of day: $currentHour")
        Log.d(TAG, "⏰ Minute: $currentMinute")
        Log.d(TAG, "⏰ Is midnight hour: ${currentHour == 0}")
    }
    
    /**
     * Schedule the midnight reset worker to run at 12:00 AM every day
     */
    fun scheduleMidnightReset() {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate initial delay to the next midnight
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Calculate time until midnight
        var delayHours = 24 - currentHour
        var delayMinutes = -currentMinute
        
        if (delayMinutes < 0) {
            delayMinutes += 60
            delayHours -= 1
        }
        
        // Convert to minutes
        val initialDelayMinutes = delayHours * 60 + delayMinutes
        
        Log.i(TAG, "Scheduling midnight reset with initial delay of $initialDelayMinutes minutes " +
                "(current time: $currentHour:$currentMinute, target: 00:00)")
        
        // Create work request to run at midnight daily
        val resetWorkRequest = PeriodicWorkRequestBuilder<MidnightResetWorker>(
            24, TimeUnit.HOURS) // Once per day
            .setInitialDelay(initialDelayMinutes.toLong(), TimeUnit.MINUTES)
            .addTag("midnight_reset")
            .build()
        
        // Enqueue work request as unique periodic work
        workManager.enqueueUniquePeriodicWork(
            MIDNIGHT_RESET_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            resetWorkRequest
        )
        
        Log.d(TAG, "Midnight reset scheduled successfully with ID: ${resetWorkRequest.id}")
    }
    
    /**
     * Trigger the midnight reset worker immediately for testing/debugging
     */
    fun triggerMidnightResetNow() {
        Log.i(TAG, "🧪 Manually triggering midnight reset for debugging")
        val workManager = WorkManager.getInstance(context)
        
        // Create a one-time work request to run immediately
        val debugResetRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .addTag("debug_midnight_reset")
            .build()
        
        // Enqueue the work request
        workManager.enqueue(debugResetRequest)
        
        // Show a toast notification
        Toast.makeText(context, "Running habit reset test...", Toast.LENGTH_SHORT).show()
        
        // Log status updates
        workManager.getWorkInfoByIdLiveData(debugResetRequest.id).observeForever { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    Log.d(TAG, "✅ Debug midnight reset completed successfully")
                    Toast.makeText(context, "Habit reset completed", Toast.LENGTH_SHORT).show()
                }
                WorkInfo.State.FAILED -> {
                    Log.e(TAG, "❌ Debug midnight reset failed")
                    Toast.makeText(context, "Habit reset failed", Toast.LENGTH_SHORT).show()
                }
                WorkInfo.State.RUNNING -> {
                    Log.d(TAG, "⏳ Debug midnight reset running...")
                }
                else -> {
                    Log.d(TAG, "Debug midnight reset state: ${workInfo.state}")
                }
            }
        }
    }
    
    /**
     * Test the timezone behavior by scheduling a worker with a specific delay
     * and logging timezone information
     */
    fun testTimezoneScheduling(delayMinutes: Long = 2) {
        Log.i(TAG, "🧪 Testing timezone scheduling with ${delayMinutes}min delay")
        val workManager = WorkManager.getInstance(context)
        
        // Log detailed timezone information
        logTimezoneInfo()
        
        // Create a test work request with short delay
        val testRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag("timezone_test")
            .build()
        
        // Enqueue the work
        workManager.enqueue(testRequest)
        
        // Show a toast notification
        Toast.makeText(context, 
            "Timezone test scheduled (${delayMinutes}min delay)", 
            Toast.LENGTH_SHORT
        ).show()
        
        // Log the scheduled time
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, delayMinutes.toInt())
        Log.d(TAG, "📅 Scheduled to run at: ${calendar.time}")
        
        // Monitor the worker's progress
        workManager.getWorkInfoByIdLiveData(testRequest.id).observeForever { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    val completionTime = Calendar.getInstance().time
                    Log.d(TAG, "✅ Timezone test completed at: $completionTime")
                    Toast.makeText(context, "Timezone test completed", Toast.LENGTH_SHORT).show()
                }
                WorkInfo.State.FAILED -> {
                    Log.e(TAG, "❌ Timezone test failed")
                    Toast.makeText(context, "Timezone test failed", Toast.LENGTH_SHORT).show()
                }
                WorkInfo.State.RUNNING -> {
                    val runTime = Calendar.getInstance().time
                    Log.d(TAG, "⏳ Timezone test running at: $runTime")
                }
                else -> {
                    Log.d(TAG, "Timezone test state: ${workInfo.state}")
                }
            }
        }
    }
    
    /**
     * Cancel the midnight reset worker
     */
    fun cancelMidnightReset() {
        Log.d(TAG, "Cancelling midnight reset worker")
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(MIDNIGHT_RESET_WORK_NAME)
    }
    
    companion object {
        /**
         * Initialize the midnight reset worker on app start
         */
        fun initialize(context: Context) {
            val scheduler = MidnightResetScheduler(context)
            scheduler.logTimezoneInfo() // Log timezone info on initialization
            scheduler.scheduleMidnightReset()
            Log.i("MidnightResetScheduler", "Midnight reset worker initialized")
        }
        
        /**
         * Static method to trigger a reset for easy access
         */
        fun triggerResetForDebugging(context: Context) {
            val scheduler = MidnightResetScheduler(context)
            scheduler.logTimezoneInfo() // Log timezone info before triggering reset
            scheduler.triggerMidnightResetNow()
        }
        
        /**
         * Static method to test timezone scheduling
         */
        fun testTimezoneForDebugging(context: Context, delayMinutes: Long = 2) {
            val scheduler = MidnightResetScheduler(context)
            scheduler.testTimezoneScheduling(delayMinutes)
        }
        
        /**
         * Static method to just log timezone info
         */
        fun logTimezoneInfoForDebugging(context: Context) {
            val scheduler = MidnightResetScheduler(context)
            scheduler.logTimezoneInfo()
        }
    }
} 