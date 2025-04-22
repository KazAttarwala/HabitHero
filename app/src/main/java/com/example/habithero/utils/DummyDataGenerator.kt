package com.example.habithero.utils

import android.util.Log
import com.example.habithero.model.Habit
import com.example.habithero.model.HabitEntry
import com.example.habithero.repository.HabitEntryRepository
import com.example.habithero.repository.HabitRepository
import com.google.firebase.Timestamp
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
        val habitsToCreate = listOf(
            createHabit(userId, "Morning Meditation", "Start the day with 10 minutes of meditation", 1, Random.nextInt(0, 10)),
            createHabit(userId, "Drink Water", "Drink 8 glasses of water daily", 8, Random.nextInt(0, 15)),
            createHabit(userId, "Exercise", "30 minutes of physical activity", 1, Random.nextInt(0, 20)),
            createHabit(userId, "Read", "Read for at least 30 minutes", 1, Random.nextInt(0, 12))
        )
        
        // Add habits to Firestore and create entries for each
        val createdHabits = mutableListOf<Habit>()
        
        for (habitTemplate in habitsToCreate) {
            // Add the habit and get its ID
            val habitId = habitRepository.addHabit(habitTemplate)
            
            // If habitId is not null, get the created habit and generate entries
            if (habitId != null) {
                // Get the created habit using its ID
                val createdHabit = habitRepository.getHabitById(habitId)
                
                // If the habit was found, generate entries
                if (createdHabit != null) {
                    createdHabits.add(createdHabit)
                    generateWeeklyEntries(createdHabit)
                }
            }
        }
        
        return@withContext createdHabits.isNotEmpty()
    }
    
    /**
     * Creates a set of interesting habit patterns with varying streak lengths
     * and completion rates to test different scenarios
     */
    suspend fun generateInterestingPatterns() = withContext(Dispatchers.IO) {
        val userId = habitRepository.getCurrentUserId() ?: return@withContext false
        
        // Create habits with specific patterns
        val habitsToCreate = listOf(
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
        
        // Add habits to Firestore and generate their entries
        val createdHabits = mutableListOf<Habit>()
        
        for ((index, habitTemplate) in habitsToCreate.withIndex()) {
            // Add the habit and get its ID
            val habitId = habitRepository.addHabit(habitTemplate)
            
            // If habitId is not null, get the created habit and generate entries
            if (habitId != null) {
                // Get the created habit using its ID
                val createdHabit = habitRepository.getHabitById(habitId)
                
                // If the habit was found, generate entries
                if (createdHabit != null) {
                    createdHabits.add(createdHabit)
                    
                    // Generate specific patterns based on the index
                    when (index) {
                        0 -> generatePerfectStreakEntries(createdHabit)  // Perfect streak
                        1 -> generateGoodStreakEntries(createdHabit)     // Good streak
                        2 -> generateStrugglingHabitEntries(createdHabit) // Struggling habit
                        3 -> generateNewHabitEntries(createdHabit)       // New habit
                        4 -> generateBrokenStreakEntries(createdHabit)   // Broken streak
                    }
                }
            }
        }
        
        return@withContext createdHabits.isNotEmpty()
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
        val calendar = Calendar.getInstance()
        
        // Set last completed date to yesterday if streak > 0
        val lastCompletedDate = if (streak > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            Timestamp(calendar.time)
        } else {
            // For broken streak, set to a few days ago
            calendar.add(Calendar.DAY_OF_YEAR, -3)
            Timestamp(calendar.time)
        }
        
        // Set created date to 30 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val createdAt = Timestamp(calendar.time)
        
        return Habit(
            id = "", // ID will be set by HabitRepository.addHabit
            userId = userId,
            title = title,
            description = description,
            frequency = frequency,
            progress = if (streak > 0) frequency else Random.nextInt(0, frequency),
            completed = streak > 0,
            streak = streak,
            lastCompletedDate = lastCompletedDate,
            createdAt = createdAt,
            deleted = false
        )
    }
    
    /**
     * Generates random HabitEntry records for the past week
     * for the given habit
     */
    private suspend fun generateWeeklyEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Generate entries for the past 7 days
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Decide if this day has an entry (80% chance)
            if (Random.nextFloat() < 0.8f) {
                val progress = when {
                    habit.streak > 10 -> Random.nextInt(habit.frequency, habit.frequency + 2)
                    habit.streak > 5 -> Random.nextInt(habit.frequency - 1, habit.frequency + 2)
                    else -> Random.nextInt(0, habit.frequency + 1)
                }
                
                val completed = progress >= habit.frequency
                
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = progress,
                    completed = completed
                )

                habitEntryRepository.addHabitEntry(entry)
            }
        }
    }

    // Helper methods for specific patterns
    
    private suspend fun generatePerfectStreakEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Generate entries for the past 7 days - all completed
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            val entry = HabitEntry(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = Timestamp(calendar.time),
                progress = habit.frequency + 1,
                completed = true
            )
            
            habitEntryRepository.addHabitEntry(entry)
        }
    }
    
    private suspend fun generateGoodStreakEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Generate entries for the past 7 days - 5/7 days completed
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Skip day 3 and 6 (to create 5/7 completion)
            if (dayOffset != 3 && dayOffset != 6) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = habit.frequency / 2,
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
        }
    }
    
    private suspend fun generateStrugglingHabitEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Generate entries for the past 7 days - 3/7 days completed
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Complete only days 1, 4, and 7
            if (dayOffset == 1 || dayOffset == 4 || dayOffset == 7) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = if (Random.nextBoolean()) 1 else 0,
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
        }
    }
    
    private suspend fun generateNewHabitEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Only add entries for the last 2 days
        for (dayOffset in 2 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            val entry = HabitEntry(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = Timestamp(calendar.time),
                progress = habit.frequency,
                completed = true
            )
            
            habitEntryRepository.addHabitEntry(entry)
        }
    }
    
    private suspend fun generateBrokenStreakEntries(habit: Habit) {
        val calendar = Calendar.getInstance()
        
        // Generate entries for the past 7 days
        for (dayOffset in 7 downTo 1) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            // Good streak for days 7-4, then missing for 3-1
            if (dayOffset >= 4) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = habit.frequency,
                    completed = true
                )
                
                habitEntryRepository.addHabitEntry(entry)
            } else if (dayOffset == 3) {
                val entry = HabitEntry(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = Timestamp(calendar.time),
                    progress = habit.frequency / 2,
                    completed = false
                )
                
                habitEntryRepository.addHabitEntry(entry)
            }
            // No entries for days 2-1
        }
    }
    
    /**
     * Clears all generated test data (both habits and their entries)
     */
    suspend fun clearDummyData() = withContext(Dispatchers.IO) {
        // Get all habits for the current user, including deleted ones
        val habits = habitRepository.getHabitsForCurrentUser(includeDeleted = true)
        val habitIds = habits.map { it.id }
        
        // Delete all entries for each habit
        try {
            for (habitId in habitIds) {
                // Delete entries first
                val success = habitEntryRepository.deleteEntriesForHabit(habitId)
                if (!success) {
                    Log.e("DummyDataGenerator", "Failed to delete entries for habit $habitId")
                }
                
                // Then delete the habit
                habitRepository.deleteHabit(habitId)
            }
        } catch (e: Exception) {
            Log.e("DummyDataGenerator", "Error clearing dummy data", e)
            return@withContext false
        }
        
        return@withContext true
    }
}