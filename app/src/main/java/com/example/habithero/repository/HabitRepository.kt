package com.example.habithero.repository

import android.util.Log
import com.example.habithero.model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HabitRepository {
    private val db = FirebaseFirestore.getInstance()
    private val habitsCollection = db.collection("habits")
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getHabitsForCurrentUser(): List<Habit> {
        val userId = getCurrentUserId() ?: return emptyList()
        return try {
            habitsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Habit::class.java)
        } catch (e: Exception) {
            // log error
            e.printStackTrace()
            Log.e("HabitRepository", "Error getting habits for current user", e)
            emptyList()
        }
    }

    suspend fun addHabit(habit: Habit): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            val habitId = habitsCollection.document().id
            val newHabit = habit.copy(id = habitId, userId = userId)
            habitsCollection.document(habitId).set(newHabit).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateHabit(habit: Habit): Boolean {
        return try {
            habitsCollection.document(habit.id).set(habit).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteHabit(habitId: String): Boolean {
        return try {
            habitsCollection.document(habitId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
} 