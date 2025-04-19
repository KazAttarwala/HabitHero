package com.example.habithero.utils

import com.example.habithero.model.Habit
import com.example.habithero.model.HabitEntry
import com.example.habithero.repository.HabitEntryRepository
import com.example.habithero.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

/**
 * Utility class to generate dummy data for testing the insights screen
 */
class DummyDataGenerator {
    private val habitRepository = HabitRepository()
    private val habitEntryRepository = HabitEntryRepository()

    /**
     * Creates a set of sample habits and their corresponding entries
     * for the currently logged-in user
     */
    suspend fun generateDummyData() = withContext(Dispatchers.IO) {
        val userId = habitRepository.getCurrentUserId() ?: return@withContext false
        
        // Create sample habits
        val habits = listOf(
            createHabit(userId, "Morning Meditation", "Start the day with 10 minutes of meditation", 1, Random.nextInt(0, 10)),
            createHabit(userId, "Drink Water", "Drink 8 glasses of water daily", 8, Random.nextInt(0, 15)),
            createHabit(userId, "Exercise", "30 minutes of physical activity", 1, Random.nextInt(0, 20)),
            createHabit(userId, "Read", "Read for at least 30 minutes", 1, Random.nextInt(0, 12))
        )
        
        // Add habits to Firestore
        val habitIds = mutableListOf<String>()
        for (habit in habits) {
            val success = habitRepository.addHabit(habit)
            if (success) habitIds.add(habit.id)
        }
        
        // Generate entries for each habit
        for (habitId in habitIds) {
            generateWeeklyEntries(habitId)
        }
        
        return@withContext habitIds.isNotEmpty()
    }
    
    /**
     * Creates a set of interesting habit patterns with varying streak lengths
     * and completion rates to test different scenarios
     */
    suspend fun generateInterestingPatterns() = withContext(Dispatchers.IO) {
        val userId = habitRepository.getCurrentUserId() ?: return@withContext false
        
        // Create habits with specific patterns
        val habits = listOf(
            // Perfect streak - 100% completion
            createHabit(userId, "Perfect Streak", "A perfect habit with all days completed", 1, 14),
            
            // Good streak - ~80% completion
            createHabit(userId, "Good Streak", "A good habit with most days completed", 1, 8),
            
            // Struggling streak - ~50% completion
            createHabit(userId, "Struggling Habit", "A habit with roughly half the days completed", 2, 4),
            
            // Just started - new habit
            createHabit(userId, "New Habit", "A newly created habit", 1, 1),
            
            // Broken streak - was good, now broken
            createHabit(userId, "Broken Streak", "Previously good streak now broken", 1, 0)
        )
        
        // Add habits to Firestore
        val habitIds = mutableListOf<String>()
        for (habit in habits) {
            val success = habitRepository.addHabit(habit)
            if (success) habitIds.add(habit.id)
        }
        
        // Generate specific pattern entries
        for (i in 0 until habitIds.size) {
            when (i) {
                0 -> generatePerfectStreakEntries(habitIds[i])  // Perfect streak
                1 -> generateGoodStreakEntries(habitIds[i])     // Good streak
                2 -> generateStrugglingHabitEntries(habitIds[i]) // Struggling habit
                3 -> generateNewHabitEntries(habitIds[i])       // New habit
                4 -> generateBrokenStreakEntries(habitIds[i])   // Broken streak
            }
        }
        
        return@withContext habitIds.isNotEmpty()
    }
    
    /**
     * Creates a habit with the specified attributes
     */
    private fun createHabit(
        userId: String,
        title: String,
        description: String,
        frequency: Int,
        streak: Int
    ): Habit {
        val habitId = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance()
        
        // Set last completed date to yesterday if streak > 0
        if (streak > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Otherwise, set to a few days ago to show broken streak
            calendar.add(Calendar.DAY_OF_YEAR, -3)
        }
        
        return Habit(
            id = habitId,
            userId = userId,
            title = title,
            description = description,
            frequency = frequency,
            progress = if (streak > 0) frequency else Random.nextInt(0, frequency),
            completed = streak > 0,
            streak = streak,
            lastCompletedDate = calendar.timeInMillis,
            createdAt = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // Created 30 days ago
        )
    }
    
    /**
     * Generates random HabitEntry records for the past week
     * for the given habitId
     */
    private suspend fun generateWeeklyEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Generate entries for the past 7 days
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Decide if this day has an entry (80% chance)
            if (Random.nextFloat() < 0.8f) {
                val progress = when {
                    // For higher streaks, make more days successful
                    habit.streak > 10 -> Random.nextInt(habit.frequency, habit.frequency + 2)
                    habit.streak > 5 -> Random.nextInt(habit.frequency - 1, habit.frequency + 2)
                    else -> Random.nextInt(0, habit.frequency + 1)
                }
                
                val completed = progress >= habit.frequency
                
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = progress,
                    completed = completed
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    // Helper methods for specific patterns
    
    private suspend fun generatePerfectStreakEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Generate entries for the past 7 days - all completed
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            val entry = HabitEntry(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = calendar.timeInMillis,
                progress = habit.frequency + 1, // Always exceeding target
                completed = true
            )
            
            habitEntryRepository.addHabitEntry(entry)
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    private suspend fun generateGoodStreakEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Generate entries for the past 7 days - 5/7 days completed fully
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Skip day 3 and 6 (to create 5/7 completion)
            if (dayOffset != 3 && dayOffset != 6) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else {
                // For missed days, still log partial progress
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = habit.frequency / 2, // Half done
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    private suspend fun generateStrugglingHabitEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Generate entries for the past 7 days - 3/7 days completed
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Complete only days 1, 4, and 7
            if (dayOffset == 1 || dayOffset == 4 || dayOffset == 7) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else {
                // For missed days, small or no progress
                val progress = if (Random.nextBoolean()) 1 else 0
                
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = progress,
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    private suspend fun generateNewHabitEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Only add entries for the last 2 days
        for (dayOffset in 2 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            val entry = HabitEntry(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = calendar.timeInMillis,
                progress = habit.frequency,
                completed = true
            )
            
            habitEntryRepository.addHabitEntry(entry)
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    private suspend fun generateBrokenStreakEntries(habitId: String) {
        val calendar = Calendar.getInstance()
        val habit = habitRepository.getHabitsForCurrentUser().find { it.id == habitId } ?: return
        
        // Generate entries for the past 7 days
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Good streak for days 7-4, then missing for 3-1
            if (dayOffset >= 4) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else if (dayOffset == 3) {
                // Partial on day 3
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = calendar.timeInMillis,
                    progress = habit.frequency / 2,
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
            // No entries for days 2-1
            
            // Reset calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    /**
     * Clears all generated test data
     */
    suspend fun clearDummyData() = withContext(Dispatchers.IO) {
        val habits = habitRepository.getHabitsForCurrentUser()
        
        for (habit in habits) {
            habitRepository.deleteHabit(habit.id)
        }
        
        return@withContext true
    }
} 