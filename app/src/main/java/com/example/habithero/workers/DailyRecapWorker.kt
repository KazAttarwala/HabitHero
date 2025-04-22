package com.example.habithero.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habithero.repository.HabitRepository
import com.example.habithero.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyRecapWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "DailyRecapWorker"
    private val habitRepository = HabitRepository()
    private val notificationHelper = NotificationHelper(context)
    private val auth = FirebaseAuth.getInstance()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting daily recap work")
            
            // Check if user is logged in
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "User not logged in, cannot fetch habits")
                return@withContext Result.failure()
            }
            
            Log.d(TAG, "User logged in as ${currentUser.email}, fetching habits")
            
            // Fetch all habits for the current user
            val habits = habitRepository.getHabitsForCurrentUser()
            
            if (habits.isEmpty()) {
                Log.d(TAG, "No habits found for user ${currentUser.uid}")
                return@withContext Result.success()
            }
            
            // Separate habits into completed and incomplete
            val completedHabits = habits.filter { it.completed }
            val incompleteHabits = habits.filter { !it.completed }
            
            Log.i(TAG, "Found ${completedHabits.size} completed and ${incompleteHabits.size} incomplete habits")
            
            // Show detailed information about habits
            completedHabits.forEach { habit ->
                Log.d(TAG, "Completed: ${habit.title} (ID: ${habit.id})")
            }
            
            incompleteHabits.forEach { habit ->
                Log.d(TAG, "Incomplete: ${habit.title} (ID: ${habit.id}, Progress: ${habit.progress}/${habit.frequency})")
            }
            
            // Show notification with the habit summaries
            Log.i(TAG, "Sending recap notification")
            notificationHelper.showDailyRecapNotification(completedHabits, incompleteHabits)
            
            Log.i(TAG, "Daily recap work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyRecapWorker", e)
            Result.failure()
        }
    }
} 