package com.example.habithero.repository

import android.util.Log
import com.example.habithero.model.HabitEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class HabitEntryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val entriesCollection = db.collection("habit_entries")
    private val auth = FirebaseAuth.getInstance()
    private val habitRepository = HabitRepository()

    suspend fun addHabitEntry(habitEntry: HabitEntry): Boolean {
        return try {
            val entryId = entriesCollection.document().id
            val newEntry = habitEntry.copy(id = entryId)
            entriesCollection.document(entryId).set(newEntry).await()
            true
        } catch (e: Exception) {
            Log.e("HabitEntryRepository", "Error adding habit entry", e)
            false
        }
    }

    suspend fun getWeeklyHabitEntries(habitId: String): List<HabitEntry> {
        try {
            // Calculate one week ago from current time
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val oneWeekAgo = calendar.timeInMillis

            return entriesCollection
                .whereEqualTo("habitId", habitId)
                .whereGreaterThanOrEqualTo("date", oneWeekAgo)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(HabitEntry::class.java)
        } catch (e: Exception) {
            Log.e("HabitEntryRepository", "Error getting weekly entries", e)
            return emptyList()
        }
    }
    
    /**
     * Get habit entries within a specific date range
     * 
     * @param habitId The ID of the habit to get entries for
     * @param startDate The start date timestamp (inclusive)
     * @param endDate The end date timestamp (inclusive)
     * @return List of habit entries within the date range
     */
    suspend fun getHabitEntriesInRange(habitId: String, startDate: Long, endDate: Long): List<HabitEntry> {
        try {
            return entriesCollection
                .whereEqualTo("habitId", habitId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(HabitEntry::class.java)
        } catch (e: Exception) {
            Log.e("HabitEntryRepository", "Error getting entries in range", e)
            return emptyList()
        }
    }

    suspend fun getAllHabitEntriesForUser(): List<HabitEntry> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val habits = habitRepository.getHabitsForCurrentUser()
        val habitIds = habits.map { it.id }
        
        if (habitIds.isEmpty()) return emptyList()
        
        try {
            // Get entries for all user's habits from the last 30 days
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val thirtyDaysAgo = calendar.timeInMillis
            
            return entriesCollection
                .whereIn("habitId", habitIds)
                .whereGreaterThanOrEqualTo("date", thirtyDaysAgo)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(HabitEntry::class.java)
        } catch (e: Exception) {
            Log.e("HabitEntryRepository", "Error getting user habit entries", e)
            return emptyList()
        }
    }
} 