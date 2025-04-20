package com.example.habithero.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val lastCompletedFormattedDate: String = if (lastCompletedDate > 0) SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(lastCompletedDate)) else "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdAtFormattedDate: String = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(createdAt))
) 