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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InsightsViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    private val habitEntryRepository = HabitEntryRepository()
    
    private val _habits = MutableLiveData<List<Habit>>()
    val habits: LiveData<List<Habit>> = _habits
    
    private val _selectedHabit = MutableLiveData<Habit>()
    val selectedHabit: LiveData<Habit> = _selectedHabit
    
    private val _weeklyData = MutableLiveData<Map<String, Int>>()
    val weeklyData: LiveData<Map<String, Int>> = _weeklyData
    
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
                
                // Select the first habit by default if available
                if (habitsFromDb.isNotEmpty() && _selectedHabit.value == null) {
                    selectHabit(habitsFromDb.first())
                }
            } catch (e: Exception) {
                _error.value = "Failed to load habits: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectHabit(habit: Habit) {
        _selectedHabit.value = habit
        loadWeeklyData(habit.id)
    }
    
    private fun loadWeeklyData(habitId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Get weekly entries for this habit
                val entries = habitEntryRepository.getWeeklyHabitEntries(habitId)
                
                // Create a map of day to progress
                val weeklyProgress = createWeeklyDataMap(entries)
                _weeklyData.value = weeklyProgress
                
            } catch (e: Exception) {
                _error.value = "Failed to load weekly data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun createWeeklyDataMap(entries: List<HabitEntry>): Map<String, Int> {
        // Create a map of day labels to progress values
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableMapOf<String, Int>()
        
        // Initialize the past 7 days with 0 progress
        val calendar = Calendar.getInstance()
        // Start from today
        val days = mutableListOf<String>()
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayLabel = dayFormat.format(calendar.time)
            days.add(dayLabel)
            result[dayLabel] = 0
            calendar.add(Calendar.DAY_OF_YEAR, i) // Reset back to today
        }
        
        // Fill in actual progress data from entries
        for (entry in entries) {
            val date = Date(entry.date)
            val dayLabel = dayFormat.format(date)
            // If multiple entries exist for the same day, use the highest progress
            val currentProgress = result[dayLabel] ?: 0
            if (entry.progress > currentProgress) {
                result[dayLabel] = entry.progress
            }
        }
        
        // Sort by the day of week
        val sortedResult = mutableMapOf<String, Int>()
        for (day in days) {
            sortedResult[day] = result[day] ?: 0
        }
        
        return sortedResult
    }
    
    fun getCompletionRate(): Int {
        val selectedHabit = _selectedHabit.value ?: return 0
        val weeklyData = _weeklyData.value ?: return 0
        
        if (weeklyData.isEmpty()) return 0
        
        val completedDays = weeklyData.count { it.value >= selectedHabit.frequency }
        return (completedDays * 100 / weeklyData.size)
    }
} 