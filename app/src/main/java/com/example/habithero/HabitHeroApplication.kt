package com.example.habithero

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.habithero.utils.NotificationPreferences

class HabitHeroApplication : Application(), Configuration.Provider {

    private lateinit var notificationPreferences: NotificationPreferences
    private val TAG = "HabitHeroApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate started")

        // Initialize notification preferences
        notificationPreferences = NotificationPreferences(this)

        // Log current notification settings
        Log.d(TAG, "Notification enabled: ${notificationPreferences.isDailyRecapEnabled()}")
        Log.d(TAG, "Notification time: ${notificationPreferences.getDailyRecapHour()}:${notificationPreferences.getDailyRecapMinute()}")

        // Reschedule daily recap notification if enabled
        if (notificationPreferences.isDailyRecapEnabled()) {
            Log.i(TAG, "Scheduling notifications on app start")
            notificationPreferences.scheduleDailyRecap()
        } else {
            Log.i(TAG, "Notifications not enabled, skipping scheduling")
        }

        Log.d(TAG, "Application onCreate completed")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        const val DAILY_RECAP_WORK_NAME = "daily_recap_notification"
    }
}