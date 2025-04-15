package com.example.habithero.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 