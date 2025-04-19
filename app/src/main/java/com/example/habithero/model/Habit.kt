package com.example.habithero.model

data class Habit(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val frequency: Int = 1,  // Number of times the habit needs to be done daily
    val progress: Int = 0,   // Current progress (times completed today)
    val completed: Boolean = false,
    val streak: Int = 0,     // Current streak (days in a row completed)
    val lastCompletedDate: Long = 0, // Timestamp of last completion
    val createdAt: Long = System.currentTimeMillis()
) 