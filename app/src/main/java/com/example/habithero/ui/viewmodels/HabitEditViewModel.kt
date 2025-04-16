package com.example.habithero.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.model.Habit
import com.example.habithero.repository.HabitRepository
import kotlinx.coroutines.launch

class HabitEditViewModel : ViewModel() {
    private val habitRepository = HabitRepository()
    
    private val _habit = MutableLiveData<Habit?>()
    val habit: LiveData<Habit?> = _habit
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadHabit(habitId: String) {
        if (habitId.isBlank()) {
            _habit.value = null
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get all habits for the user and find the one with matching ID
                val habits = habitRepository.getHabitsForCurrentUser()
                _habit.value = habits.find { it.id == habitId }
            } catch (e: Exception) {
                _error.value = "Failed to load habit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveHabit(title: String, description: String, frequency: Int) {
        if (title.isBlank()) {
            _error.value = "Title cannot be empty"
            return
        }
        
        if (frequency < 1) {
            _error.value = "Frequency must be at least 1"
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentHabit = _habit.value
                val result = if (currentHabit != null) {
                    // Update existing habit
                    val updatedHabit = currentHabit.copy(
                        title = title,
                        description = description,
                        frequency = frequency
                    )
                    habitRepository.updateHabit(updatedHabit)
                } else {
                    // Create new habit
                    val newHabit = Habit(
                        title = title,
                        description = description,
                        frequency = frequency
                    )
                    habitRepository.addHabit(newHabit)
                }
                
                _saveSuccess.value = result
                if (!result) {
                    _error.value = "Failed to save habit"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
} 