package com.example.habithero.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
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
    val lastCompletedDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun getFormattedLastCompletedDate(): String {
        return lastCompletedDate?.let { 
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(it.toDate())
        } ?: ""
    }

    fun getFormattedCreatedDate(): String {
        return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(createdAt.toDate())
    }
} 