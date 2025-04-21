package com.example.habithero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.api.AnthropicService
import com.example.habithero.model.Habit
import com.example.habithero.model.HabitAnalysis
import com.example.habithero.model.HabitEntry
import com.example.habithero.repository.HabitEntryRepository
import com.example.habithero.repository.HabitRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class InsightsViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    private val habitEntryRepository = HabitEntryRepository()
    private val anthropicService = AnthropicService()
    
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
    
    // Week navigation properties
    private val _weekOffset = MutableLiveData<Int>(0) // 0 = current week, -1 = last week, etc.
    val weekOffset: LiveData<Int> = _weekOffset
    
    private val _dateRange = MutableLiveData<String>()
    val dateRange: LiveData<String> = _dateRange
    
    // Habit analysis properties
    private val _habitAnalysis = MutableLiveData<HabitAnalysis>()
    val habitAnalysis: LiveData<HabitAnalysis> = _habitAnalysis
    
    private val _isAnalysisLoading = MutableLiveData<Boolean>(false)
    val isAnalysisLoading: LiveData<Boolean> = _isAnalysisLoading
    
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
        loadWeeklyData(habit.id, _weekOffset.value ?: 0)
    }
    
    fun navigateToPreviousWeek() {
        val currentOffset = _weekOffset.value ?: 0
        val newOffset = currentOffset - 1
        _weekOffset.value = newOffset
        _selectedHabit.value?.let { loadWeeklyData(it.id, newOffset) }
    }
    
    fun navigateToNextWeek() {
        val currentOffset = _weekOffset.value ?: 0
        val newOffset = currentOffset + 1
        // Don't allow navigating to future weeks
        if (newOffset <= 0) {
            _weekOffset.value = newOffset
            _selectedHabit.value?.let { loadWeeklyData(it.id, newOffset) }
        }
    }
    
    fun resetToCurrentWeek() {
        _weekOffset.value = 0
        _selectedHabit.value?.let { loadWeeklyData(it.id, 0) }
    }
    
    private fun loadWeeklyData(habitId: String, weekOffset: Int = 0) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Get entries for the selected week
                val entries = loadEntriesForWeek(habitId, weekOffset)
                
                // Create a map of day to progress
                val weeklyProgress = createWeeklyDataMap(entries, weekOffset)
                _weeklyData.value = weeklyProgress
                
                // Update date range
                updateDateRange(weekOffset)
                
            } catch (e: Exception) {
                _error.value = "Failed to load weekly data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadEntriesForWeek(habitId: String, weekOffset: Int): List<HabitEntry> {
        // Calculate the date range for the selected week
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        
        // If weekOffset is negative, move back that many weeks
        if (weekOffset < 0) {
            calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        
        // Find the start of the week (Sunday in most cases)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Move the calendar to the start of the week and set to midnight UTC
        calendar.add(Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        
        // Move to the end of the week and set to last millisecond of the day
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val weekEnd = calendar.timeInMillis
        
        // Get entries in the date range
        return habitEntryRepository.getHabitEntriesInRange(habitId, weekStart, weekEnd)
    }
    
    private fun createWeeklyDataMap(entries: List<HabitEntry>, weekOffset: Int): Map<String, Int> {
        // Create a map of day labels to progress values
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableMapOf<String, Int>()
        
        // Initialize the days of the selected week with 0 progress
        val calendar = Calendar.getInstance()
        
        // Adjust to the selected week
        if (weekOffset < 0) {
            calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        
        // Find the start of the week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.add(Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1))
        
        // Collect the days of the week
        val days = mutableListOf<String>()
        for (i in 0..6) {
            // No need to reset the calendar since we're moving forward one day at a time
            val dayLabel = dayFormat.format(calendar.time)
            days.add(dayLabel)
            result[dayLabel] = 0
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Fill in actual progress data from entries
        for (entry in entries) {
            val date = entry.date.toDate()
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
    
    private fun updateDateRange(weekOffset: Int) {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // Adjust to the selected week
        if (weekOffset < 0) {
            calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        
        // Find the start of the week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.add(Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1))
        val weekStart = dateFormat.format(calendar.time)
        
        // Move to the end of the week
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val weekEnd = dateFormat.format(calendar.time)
        
        _dateRange.value = "$weekStart - $weekEnd"
    }
    
    fun getCompletionRate(): Int {
        val selectedHabit = _selectedHabit.value ?: return 0
        val weeklyData = _weeklyData.value ?: return 0
        
        if (weeklyData.isEmpty()) return 0
        
        val completedDays = weeklyData.count { it.value >= selectedHabit.frequency }
        return (completedDays * 100 / weeklyData.size)
    }
    
    fun requestHabitAnalysis() {
        val selectedHabit = _selectedHabit.value ?: return
        val weeklyData = _weeklyData.value ?: return
        
        viewModelScope.launch {
            try {
                _isAnalysisLoading.value = true
                val completionRate = getCompletionRate()
                
                Log.d("InsightsViewModel", "Requesting analysis for habit: ${selectedHabit.title}")
                val analysis = anthropicService.analyzeHabitData(
                    selectedHabit,
                    weeklyData,
                    completionRate
                )
                Log.d("InsightsViewModel", "Received analysis: $analysis")
                
                _habitAnalysis.value = analysis
            } catch (e: Exception) {
                Log.e("InsightsViewModel", "Analysis failed", e)
                _error.value = "Failed to get habit analysis: ${e.message}"
            } finally {
                _isAnalysisLoading.value = false
            }
        }
    }
} 