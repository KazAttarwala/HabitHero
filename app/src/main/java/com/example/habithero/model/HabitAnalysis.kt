package com.example.habithero.model

data class HabitAnalysis(
    val summary: String,
    val recommendations: List<String>,
    val suggestedImprovements: List<String>
) 