package com.example.habithero.model

data class Habit(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) 