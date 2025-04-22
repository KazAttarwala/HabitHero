package com.example.habithero.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.habithero.HabitHeroApplication
import com.example.habithero.workers.DailyRecapWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manages notification preferences and scheduling
 */
class NotificationPreferences(private val context: Context) {
    
    private val TAG = "NotificationPreferences"
    private val PREFS_NAME = "habit_hero_prefs"
    private val DAILY_RECAP_ENABLED = "daily_recap_enabled"
    private val DAILY_RECAP_HOUR = "daily_recap_hour"
    private val DAILY_RECAP_MINUTE = "daily_recap_minute"
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Default values
    val DEFAULT_RECAP_HOUR = 21 // 9 PM
    val DEFAULT_RECAP_MINUTE = 0
    
    /**
     * Check if daily recap notifications are enabled
     */
    fun isDailyRecapEnabled(): Boolean {
        return prefs.getBoolean(DAILY_RECAP_ENABLED, false)
    }
    
    /**
     * Set daily recap enabled/disabled
     */
    fun setDailyRecapEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting daily recap enabled=$enabled")
        prefs.edit().putBoolean(DAILY_RECAP_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyRecap()
        } else {
            cancelDailyRecap()
        }
    }
    
    /**
     * Get the hour for daily recap notifications (24-hour format)
     */
    fun getDailyRecapHour(): Int {
        return prefs.getInt(DAILY_RECAP_HOUR, DEFAULT_RECAP_HOUR)
    }
    
    /**
     * Get the minute for daily recap notifications
     */
    fun getDailyRecapMinute(): Int {
        return prefs.getInt(DAILY_RECAP_MINUTE, DEFAULT_RECAP_MINUTE)
    }
    
    /**
     * Set the time for daily recap notifications
     */
    fun setDailyRecapTime(hour: Int, minute: Int) {
        Log.d(TAG, "Setting daily recap time to $hour:$minute")
        prefs.edit()
            .putInt(DAILY_RECAP_HOUR, hour)
            .putInt(DAILY_RECAP_MINUTE, minute)
            .apply()
        
        // If enabled, reschedule with new time
        if (isDailyRecapEnabled()) {
            scheduleDailyRecap()
        }
    }
    
    /**
     * Sends a test notification immediately for debugging
     */
    fun sendTestNotificationNow() {
        Log.i(TAG, "Sending test notification immediately")
        val workManager = WorkManager.getInstance(context)
        
        // Create a one-time work request with no delay
        val testWorkRequest = OneTimeWorkRequestBuilder<DailyRecapWorker>()
            .addTag("test_notification")
            .build()
        
        // Enqueue the work request
        workManager.enqueue(testWorkRequest)
        
        // Log the state
        workManager.getWorkInfoByIdLiveData(testWorkRequest.id).observeForever { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> Log.d(TAG, "Test notification work succeeded")
                WorkInfo.State.FAILED -> Log.e(TAG, "Test notification work failed")
                WorkInfo.State.RUNNING -> Log.d(TAG, "Test notification work running")
                else -> Log.d(TAG, "Test notification work state: ${workInfo.state}")
            }
        }
    }
    
    /**
     * Gets the status of any scheduled recap notifications
     * @return A string with the current status for debugging
     */
    fun getScheduledNotificationsStatus(): String {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfos(
            WorkQuery.Builder
                .fromUniqueWorkNames(listOf(HabitHeroApplication.DAILY_RECAP_WORK_NAME))
                .build()
        ).get()
        
        if (workInfos.isEmpty()) {
            return "No scheduled notifications found"
        }
        
        return workInfos.joinToString("\n") { workInfo ->
            "ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags.joinToString()}"
        }
    }
    
    /**
     * Schedule the daily recap notification worker
     */
    fun scheduleDailyRecap() {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate initial delay to the specified time
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val targetHour = getDailyRecapHour()
        val targetMinute = getDailyRecapMinute()
        
        // Calculate time difference
        var delayHours = targetHour - currentHour
        var delayMinutes = targetMinute - currentMinute
        
        if (delayMinutes < 0) {
            delayMinutes += 60
            delayHours -= 1
        }
        
        if (delayHours < 0) {
            delayHours += 24 // Schedule for tomorrow
        }
        
        // Convert to minutes
        val initialDelayMinutes = delayHours * 60 + delayMinutes
        
        Log.i(TAG, "Scheduling daily recap with initial delay of $initialDelayMinutes minutes " +
                "(current: $currentHour:$currentMinute, target: $targetHour:$targetMinute)")
        
        // Create work request
        val dailyRecapWork = PeriodicWorkRequestBuilder<DailyRecapWorker>(
            24, TimeUnit.HOURS) // Once per day
            .setInitialDelay(initialDelayMinutes.toLong(), TimeUnit.MINUTES)
            .addTag("daily_recap")
            .build()
        
        // Enqueue work request
        workManager.enqueueUniquePeriodicWork(
            HabitHeroApplication.DAILY_RECAP_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            dailyRecapWork
        )
        
        Log.d(TAG, "Daily recap scheduled successfully with ID: ${dailyRecapWork.id}")
    }
    
    /**
     * Cancel the daily recap notification worker
     */
    fun cancelDailyRecap() {
        Log.d(TAG, "Cancelling daily recap notifications")
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(HabitHeroApplication.DAILY_RECAP_WORK_NAME)
    }
} 