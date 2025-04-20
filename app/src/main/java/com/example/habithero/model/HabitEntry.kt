package com.example.habithero.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class HabitEntry(
    val id: String = "",
    val habitId: String = "",
    val date: Timestamp = Timestamp.now(),
    val progress: Int = 0,
    val completed: Boolean = false
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(date.toDate())
    }
} 