package com.example.habithero.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HabitEntry(
    val id: String = "",
    val habitId: String = "",
    val date: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(date)),
    val progress: Int = 0,
    val completed: Boolean = false
) 