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

    /**
     * Add a new habit to Firestore
     * 
     * @param habit The habit to add (ID will be set by this method)
     * @return The ID of the created habit, or null if creation failed
     */
    suspend fun addHabit(habit: Habit): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            val habitId = habitsCollection.document().id
            val newHabit = habit.copy(id = habitId, userId = userId)
            habitsCollection.document(habitId).set(newHabit).await()
            habitId
        } catch (e: Exception) {
            Log.e("HabitRepository", "Error adding habit", e)
            null
        }
    }
    
    /**
     * Get a habit by its ID
     * 
     * @param habitId The ID of the habit to get
     * @return The habit, or null if not found
     */
    suspend fun getHabitById(habitId: String): Habit? {
        return try {
            val document = habitsCollection.document(habitId).get().await()
            if (document.exists()) {
                document.toObject(Habit::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HabitRepository", "Error getting habit by ID", e)
            null
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