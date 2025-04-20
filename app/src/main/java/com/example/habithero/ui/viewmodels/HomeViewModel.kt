package com.example.habithero.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.model.Habit
import com.example.habithero.model.HabitEntry
import com.example.habithero.repository.HabitEntryRepository
import com.example.habithero.repository.HabitRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    private val habitEntryRepository = HabitEntryRepository()
    
    private val _habits = MutableLiveData<List<Habit>>()
    val habits: LiveData<List<Habit>> = _habits
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadHabits()
    }

    fun loadHabits() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val habitsFromDb = habitRepository.getHabitsForCurrentUser()
                _habits.value = habitsFromDb
            } catch (e: Exception) {
                _error.value = "Failed to load habits: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addHabit(title: String, description: String) {
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                val newHabit = Habit(title = title, description = description)
                val habitId = habitRepository.addHabit(newHabit)
                if (habitId != null) {
                    loadHabits() // Refresh the list
                } else {
                    _error.value = "Failed to add habit"
                }
            } catch (e: Exception) {
                _error.value = "Failed to add habit: ${e.message}"
            }
        }
    }

    fun incrementHabitProgress(habit: Habit) {
        viewModelScope.launch {
            try {
                // If progress is already at or over frequency, do nothing
                if (habit.progress >= habit.frequency) return@launch
                
                // Increment progress
                val newProgress = habit.progress + 1
                // Update completed status if needed
                val isCompleted = newProgress >= habit.frequency
                
                // Current time 
                val currentTimeMillis = System.currentTimeMillis()
                
                // Calculate streak if this increment completes the habit
                val (newStreak, lastCompletedDate) = if (isCompleted) {
                    calculateNewStreak(habit, currentTimeMillis)
                } else {
                    Pair(habit.streak, habit.lastCompletedDate)
                }
                
                val updatedHabit = habit.copy(
                    progress = newProgress,
                    completed = isCompleted,
                    streak = newStreak,
                    lastCompletedDate = lastCompletedDate
                )
                
                val success = habitRepository.updateHabit(updatedHabit)
                if (success) {
                    // Record this progress in habit entries
                    val habitEntry = HabitEntry(
                        habitId = habit.id,
                        date = currentTimeMillis,
                        progress = newProgress,
                        completed = isCompleted
                    )
                    habitEntryRepository.addHabitEntry(habitEntry)
                    loadHabits() // Refresh the list
                } else {
                    _error.value = "Failed to update habit"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update habit progress: ${e.message}"
            }
        }
    }
    
    private fun calculateNewStreak(habit: Habit, currentTimeMillis: Long): Pair<Int, Long> {
        // Get day start for today
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStartMillis = calendar.timeInMillis
        
        // Get day start for last completed
        calendar.timeInMillis = habit.lastCompletedDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val lastCompletedDayStartMillis = calendar.timeInMillis
        
        // Get day start for yesterday
        calendar.timeInMillis = todayStartMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStartMillis = calendar.timeInMillis
        
        // Calculate the new streak
        val newStreak = when {
            // First completion ever
            habit.lastCompletedDate == 0L -> 1
            // If completed yesterday, streak continues
            lastCompletedDayStartMillis == yesterdayStartMillis -> habit.streak + 1
            // If already completed today, keep current streak
            lastCompletedDayStartMillis == todayStartMillis -> habit.streak
            // Otherwise reset streak
            else -> 1
        }
        
        return Pair(newStreak, currentTimeMillis)
    }
    
    fun resetHabitProgress(habit: Habit) {
        viewModelScope.launch {
            try {
                val updatedHabit = habit.copy(
                    progress = 0,
                    completed = false
                )
                
                val success = habitRepository.updateHabit(updatedHabit)
                if (success) {
                    loadHabits() // Refresh the list
                } else {
                    _error.value = "Failed to reset habit progress"
                }
            } catch (e: Exception) {
                _error.value = "Failed to reset habit progress: ${e.message}"
            }
        }
    }
    
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(habitId)
                loadHabits() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to delete habit: ${e.message}"
            }
        }
    }
} 