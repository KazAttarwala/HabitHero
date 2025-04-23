package com.example.habithero.repository

import android.util.Log
import com.example.habithero.model.Habit
import com.google.firebase.Timestamp
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

    suspend fun getHabitsForCurrentUser(includeDeleted: Boolean = false): List<Habit> {
        val userId = getCurrentUserId() ?: return emptyList()
        return try {
            val query = habitsCollection
                .whereEqualTo("userId", userId)
            
            //Filter out deleted habits unless specifically requested
            val filteredQuery = if (!includeDeleted) {
                query.whereEqualTo("deleted", false)
            } else {
                query
            }

            filteredQuery
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Habit::class.java)
        } catch (e: Exception) {
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
            
            // Check for existing habits with the same title (including deleted ones)
            val existingHabits = habitsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("title", habit.title)
                .get()
                .await()
                .toObjects(Habit::class.java)
                
            // If a habit with same title exists, don't create a new one
            if (existingHabits.isNotEmpty()) {
                val existingHabit = existingHabits.first()
                
                // If it's just marked as deleted, restore it instead of creating a new one
                if (existingHabit.deleted) {
                    val restoredHabit = existingHabit.copy(
                        deleted = false,
                        streak = 0,
                        progress = 0,
                        completed = false,
                        lastCompletedDate = null
                    )
                    habitsCollection.document(existingHabit.id).set(restoredHabit).await()
                    return existingHabit.id
                }
                
                // Otherwise, don't allow duplicate title
                return null
            }
            
            val habitId = habitsCollection.document().id
            val newHabit = habit.copy(
                id = habitId, 
                userId = userId,
            )
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
     * @param includeDeleted Whether to include deleted habits
     * @return The habit, or null if not found
     */
    suspend fun getHabitById(habitId: String, includeDeleted: Boolean = false): Habit? {
        return try {
            val document = habitsCollection.document(habitId).get().await()
            if (document.exists()) {
                val habit = document.toObject(Habit::class.java)
                if (habit != null && (includeDeleted || !habit.deleted)) {
                    habit
                } else {
                    null
                }
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
            Log.d("HabitRepository", "📝 Updating habit: '${habit.title}' (ID: ${habit.id})")
            Log.d("HabitRepository", "📊 Habit details - Progress: ${habit.progress}/${habit.frequency}, Completed: ${habit.completed}")
            
            // Verify the habit exists before updating
            val existingHabit = habitsCollection.document(habit.id).get().await().toObject(Habit::class.java)
            
            if (existingHabit != null) {
                Log.d("HabitRepository", "🔍 Found existing habit '${existingHabit.title}' with progress: ${existingHabit.progress}/${existingHabit.frequency}, completed: ${existingHabit.completed}")
                
                // Perform the update
                habitsCollection.document(habit.id).set(habit).await()
                
                // Verify the update was successful by fetching the habit again
                val updatedHabit = habitsCollection.document(habit.id).get().await().toObject(Habit::class.java)
                
                if (updatedHabit != null) {
                    val updateSuccessful = updatedHabit.progress == habit.progress && updatedHabit.completed == habit.completed
                    
                    if (updateSuccessful) {
                        Log.d("HabitRepository", "✅ Successfully updated habit '${habit.title}' (ID: ${habit.id})")
                    } else {
                        Log.e("HabitRepository", "⚠️ Habit updated but values don't match! Expected progress: ${habit.progress}, actual: ${updatedHabit.progress}")
                    }
                    
                    return updateSuccessful
                } else {
                    Log.e("HabitRepository", "❌ Failed to verify update for habit '${habit.title}' (ID: ${habit.id})")
                    return false
                }
            } else {
                Log.e("HabitRepository", "❌ Cannot update habit '${habit.title}' (ID: ${habit.id}) - not found in database")
                return false
            }
        } catch (e: Exception) {
            Log.e("HabitRepository", "❌ Error updating habit '${habit.title}' (ID: ${habit.id}): ${e.message}")
            Log.e("HabitRepository", "Stack trace: ${e.stackTraceToString()}")
            false
        }
    }

    /**
     * Soft delete a habit by marking it as deleted
     * 
     * @param habitId The ID of the habit to mark as deleted
     * @return true if successful, false otherwise
     */
    suspend fun markHabitAsDeleted(habitId: String): Boolean {
        return try {
            val habit = getHabitById(habitId, includeDeleted = true)
            if (habit != null) {
                val updatedHabit = habit.copy(deleted = true)
                habitsCollection.document(habitId).set(updatedHabit).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("HabitRepository", "Error marking habit as deleted", e)
            false
        }
    }

    /**
     * Hard delete a habit (completely removes from database)
     * This should only be used when really necessary
     */
    suspend fun deleteHabit(habitId: String): Boolean {
        return try {
            habitsCollection.document(habitId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("HabitRepository", "Error deleting habit", e)
            false
        }
    }
} 