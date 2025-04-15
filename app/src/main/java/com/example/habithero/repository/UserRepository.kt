package com.example.habithero.repository

import com.example.habithero.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")
    
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    fun createUserInFirestore(user: User): Task<Void> {
        return usersCollection.document(user.id).set(user)
    }
    
    fun getUserData(userId: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        usersCollection.document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject<User>()
                onSuccess(user)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
    
    fun updateUserProfile(userId: String, updates: Map<String, Any>): Task<Void> {
        return usersCollection.document(userId).update(updates)
    }
    
    fun createUserAfterRegistration(firebaseUser: FirebaseUser): Task<Void> {
        val newUser = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: ""
        )
        return createUserInFirestore(newUser)
    }
} 