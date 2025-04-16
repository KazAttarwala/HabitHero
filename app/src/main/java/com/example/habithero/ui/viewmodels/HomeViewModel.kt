package com.example.habithero.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.model.Habit
import com.example.habithero.repository.HabitRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    
    private val _habits = MutableLiveData<List<Habit>>()
    val habits: LiveData<List<Habit>> = _habits
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadHabits()
        
        // Add some sample habits if collection is empty
        // TODO: remove this after testing
        viewModelScope.launch {
            val currentHabits = habitRepository.getHabitsForCurrentUser()
            if (currentHabits.isEmpty()) {
                addSampleHabits()
            }
        }
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

    // TODO: remove this method after testing
    private suspend fun addSampleHabits() {
        val sampleHabits = listOf(
            Habit(title = "Drink Water", description = "Drink 8 glasses of water daily"),
            Habit(title = "Exercise", description = "30 minutes of exercise"),
            Habit(title = "Read", description = "Read for 20 minutes")
        )
        
        for (habit in sampleHabits) {
            habitRepository.addHabit(habit)
        }
        
        // Reload habits after adding samples
        loadHabits()
    }
    
    fun addHabit(title: String, description: String) {
        if (title.isBlank()) return
        
        viewModelScope.launch {
            try {
                val newHabit = Habit(title = title, description = description)
                habitRepository.addHabit(newHabit)
                loadHabits() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to add habit: ${e.message}"
            }
        }
    }
    
    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            try {
                val updatedHabit = habit.copy(completed = !habit.completed)
                habitRepository.updateHabit(updatedHabit)
                loadHabits() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to update habit: ${e.message}"
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