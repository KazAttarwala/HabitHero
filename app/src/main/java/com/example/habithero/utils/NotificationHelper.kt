package com.example.habithero.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habithero.MainActivity
import com.example.habithero.R
import com.example.habithero.model.Habit

class NotificationHelper(private val context: Context) {
    
    private val TAG = "NotificationHelper"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_DAILY_RECAP = "daily_recap"
        const val NOTIFICATION_ID_DAILY_RECAP = 1001
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Check if notification permissions are granted
     */
    fun hasNotificationPermission(): Boolean {
        // For Android 13+ (API 33+) we need to check the POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= 33) {
            val permissionState = ActivityCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            return permissionState == PackageManager.PERMISSION_GRANTED
        }
        // For older versions, notifications were always allowed
        return true
    }
    
    private fun createNotificationChannels() {
        Log.d(TAG, "Creating notification channels")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Daily Recap Channel
            val recapChannel = NotificationChannel(
                CHANNEL_ID_DAILY_RECAP,
                "Daily Habit Recap",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "End of day habit summary"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(recapChannel)
            Log.d(TAG, "Created channel $CHANNEL_ID_DAILY_RECAP with importance ${recapChannel.importance}")
            
            // List all channels (for debugging)
            notificationManager.notificationChannels.forEach { channel ->
                Log.d(TAG, "Channel: ${channel.id}, Name: ${channel.name}, Importance: ${channel.importance}")
            }
        } else {
            Log.d(TAG, "Android version < O, no need to create channels")
        }
    }
    
    fun showDailyRecapNotification(completedHabits: List<Habit>, incompleteHabits: List<Habit>) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing notification permission, cannot show notification")
            return
        }
        
        Log.i(TAG, "Building and showing daily recap notification")
        
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            contentIntent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create summary of completed habits
        val completedSummary = if (completedHabits.isEmpty()) {
            "No habits completed today"
        } else {
            "Completed ${completedHabits.size} habit(s): " + 
            completedHabits.joinToString(", ") { it.title }
        }
        
        // Create summary of incomplete habits
        val incompleteSummary = if (incompleteHabits.isEmpty()) {
            "All habits completed! Great job!"
        } else {
            "Incomplete ${incompleteHabits.size} habit(s): " + 
            incompleteHabits.joinToString(", ") { it.title }
        }
        
        val notificationText = "$completedSummary\n\n$incompleteSummary"
        Log.d(TAG, "Notification text: $notificationText")
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_RECAP)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Habit Recap")
            .setContentText("Tap to see your habit summary")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            // Show notification with compatibility wrapper
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_DAILY_RECAP, notification)
            
            Log.i(TAG, "Successfully showed notification with ID: $NOTIFICATION_ID_DAILY_RECAP")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when showing notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
} 