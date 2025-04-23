package com.example.habithero.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habithero.model.Habit
import com.example.habithero.repository.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/**
 * Worker to reset the progress of all active habits to 0 at midnight
 */
class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "MidnightResetWorker"
    private val habitRepository = HabitRepository()
    private val authManager = FirebaseAuth.getInstance()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            logTimezoneInfo()
            
            Log.i(TAG, "🔄 Starting midnight habit reset process")
            
            // Check if user is logged in
            val currentUser: FirebaseUser? = authManager.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "❌ No user logged in, skipping habit reset")
                return@withContext Result.failure()
            }
            
            val userId = currentUser.uid
            Log.d(TAG, "📱 Resetting habits for user: $userId")
            
            // Get all active habits for the user
            val habits = habitRepository.getHabitsForCurrentUser(includeDeleted = false)
            if (habits.isEmpty()) {
                Log.i(TAG, "ℹ️ No habits found for user, nothing to reset")
                return@withContext Result.success()
            }
            
            // Reset progress for each habit
            var successCount = 0
            var failureCount = 0
            
            habits.forEach { habit ->
                try {
                    if (!habit.deleted) {
                        val previousProgress = habit.progress
                        
                        // Reset the habit progress to 0
                        val updatedHabit = habit.copy(progress = 0)
                        habitRepository.updateHabit(updatedHabit)
                        
                        Log.d(TAG, "✅ Reset progress for '${habit.title}' from $previousProgress to 0")
                        successCount++
                    } else {
                        Log.d(TAG, "⏭️ Skipped inactive habit: '${habit.title}'")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to reset habit '${habit.title}': ${e.message}")
                    failureCount++
                }
            }
            
            Log.i(TAG, "🏁 Midnight reset completed - Reset $successCount habits successfully, $failureCount failures")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during midnight reset: ${e.message}", e)
            Result.failure()
        }
    }
    
    /**
     * Log detailed timezone information for debugging timezone issues
     */
    private fun logTimezoneInfo() {
        val currentTimezone = TimeZone.getDefault()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.time
        
        Log.d(TAG, "⏰ Worker execution - Timezone info:")
        Log.d(TAG, "⏰ Current timezone: ${currentTimezone.id} (${currentTimezone.displayName})")
        Log.d(TAG, "⏰ Current time: $currentTime")
        Log.d(TAG, "⏰ Timezone offset: ${currentTimezone.rawOffset / (60 * 60 * 1000)}h")
        Log.d(TAG, "⏰ In daylight time: ${currentTimezone.inDaylightTime(currentTime)}")
        
        // Log exact hour and minute
        Log.d(TAG, "⏰ Hour of day: ${calendar.get(Calendar.HOUR_OF_DAY)}")
        Log.d(TAG, "⏰ Minute: ${calendar.get(Calendar.MINUTE)}")
        
        // Log if this is midnight (0:00-0:59)
        val isMidnightHour = calendar.get(Calendar.HOUR_OF_DAY) == 0
        Log.d(TAG, "⏰ Is midnight hour: $isMidnightHour")
    }
} 