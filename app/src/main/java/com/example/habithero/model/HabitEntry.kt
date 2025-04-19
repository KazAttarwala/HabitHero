package com.example.habithero.model

data class HabitEntry(
    val id: String = "",
    val habitId: String = "",
    val date: Long = System.currentTimeMillis(),
    val progress: Int = 0,
    val completed: Boolean = false
) 