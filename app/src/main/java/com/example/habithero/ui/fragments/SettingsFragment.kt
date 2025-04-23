package com.example.habithero.ui.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.habithero.R
import com.example.habithero.databinding.FragmentSettingsBinding
import com.example.habithero.utils.DummyDataGenerator
import com.example.habithero.utils.NotificationPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.habithero.HabitHeroApplication
import com.example.habithero.repository.HabitEntryRepository
import com.example.habithero.repository.HabitRepository
import com.example.habithero.utils.MidnightResetScheduler

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private val dummyDataGenerator = DummyDataGenerator()
    private lateinit var notificationPreferences: NotificationPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize notification preferences
        notificationPreferences = NotificationPreferences(requireContext())
        
        // Set user's email in the UI
        auth.currentUser?.let { user ->
            binding.userEmailTextView.text = user.email
        }
        
        // Set up logout button
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
        
        // Set up change password button
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
        
        // Set up delete account button
        binding.deleteAccountButton.setOnClickListener {
            confirmDeleteAccount()
        }
        
        // Set up notification settings
        setupNotificationSettings()

        // Only show developer options in developer mode
        setupDeveloperOptionsVisibility()
        
        // Set up developer options
        setupDeveloperOptions()

        // Add secret developer mode toggle
        var tapCount = 0
        binding.titleTextView.setOnClickListener {
            tapCount++
            if (tapCount >= 7) { // Tap 7 times to toggle developer mode
                tapCount = 0
                val app = requireActivity().application as HabitHeroApplication
                app.isDeveloperMode = !app.isDeveloperMode
                setupDeveloperOptionsVisibility()
                
                // Show a toast to indicate the mode change
                Toast.makeText(
                    requireContext(),
                    if (app.isDeveloperMode) "Developer mode enabled" else "Developer mode disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupNotificationSettings() {
        // Set initial state of the switch
        binding.dailyRecapSwitch.isChecked = notificationPreferences.isDailyRecapEnabled()
        
        // Show the current time
        updateRecapTimeButtonText()
        
        // Handle switch changes
        binding.dailyRecapSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationPreferences.setDailyRecapEnabled(isChecked)
            if (isChecked) {
                Toast.makeText(requireContext(), "Daily recap enabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle time selection
        binding.recapTimeButton.setOnClickListener {
            showTimePickerDialog()
        }
        
        // Add test notification button handler
        binding.testNotificationButton.setOnClickListener {
            try {
                Toast.makeText(requireContext(), "Sending test notification...", Toast.LENGTH_SHORT).show()
                notificationPreferences.sendTestNotificationNow()
                
                // Display current notification status
                val status = notificationPreferences.getScheduledNotificationsStatus()
                Log.d("SettingsFragment", "Scheduled notifications: $status")
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error sending test notification", e)
                Toast.makeText(
                    requireContext(), 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun updateRecapTimeButtonText() {
        val hour = notificationPreferences.getDailyRecapHour()
        val minute = notificationPreferences.getDailyRecapMinute()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.recapTimeButton.text = timeFormat.format(calendar.time)
    }
    
    private fun showTimePickerDialog() {
        val hour = notificationPreferences.getDailyRecapHour()
        val minute = notificationPreferences.getDailyRecapMinute()
        
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                notificationPreferences.setDailyRecapTime(selectedHour, selectedMinute)
                updateRecapTimeButtonText()
                
                // If enabled, this will update the scheduled time
                if (notificationPreferences.isDailyRecapEnabled()) {
                    Toast.makeText(requireContext(), "Recap time updated", Toast.LENGTH_SHORT).show()
                }
            },
            hour,
            minute,
            false
        ).show()
    }
    
    private fun setupDeveloperOptionsVisibility() {
        // Show/hide developer section based on developer mode
        val devOptionsVisible = (requireActivity().application as HabitHeroApplication).isDeveloperMode
        
        // Hide/show developer options section
        binding.devOptionsHeader.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        binding.devDescriptionTextView.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        binding.generateDummyDataButton.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        binding.generatePatternsButton.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        binding.clearDummyDataButton.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        binding.testMidnightResetButton.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
        
        // Hide/show test notification button
        binding.testNotificationButton.visibility = if (devOptionsVisible) View.VISIBLE else View.GONE
    }
    
    private fun setupDeveloperOptions() {
        // Generate random test data
        binding.generateDummyDataButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                try {
                    val result = dummyDataGenerator.generateDummyData()
                    if (result) {
                        Toast.makeText(requireContext(), "Dummy data generated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to generate dummy data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        
        // Generate test patterns
        binding.generatePatternsButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                try {
                    val result = dummyDataGenerator.generateInterestingPatterns()
                    if (result) {
                        Toast.makeText(requireContext(), "Test patterns generated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to generate test patterns", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        
        // Clear test data
        binding.clearDummyDataButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                try {
                    val result = dummyDataGenerator.clearDummyData()
                    if (result) {
                        Toast.makeText(requireContext(), "Dummy data cleared successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to clear dummy data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        
        // Test midnight reset
        binding.testMidnightResetButton.setOnClickListener {
            try {
                Log.i("SettingsFragment", "🧪 Manually triggering midnight reset for debugging")
                Toast.makeText(requireContext(), "Triggering habit reset...", Toast.LENGTH_SHORT).show()
                
                // Trigger the reset worker
                MidnightResetScheduler.triggerResetForDebugging(requireContext())
                
                // Show message to check LogCat for results
                Toast.makeText(requireContext(), "Check LogCat for reset logs", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error triggering midnight reset: ${e.message}")
                Log.e("SettingsFragment", "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        // Navigate back to login
        findNavController().navigate(R.id.loginFragment)
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This will permanently delete all your data and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                // Delete user data from Firestore
                val result = deleteUserData(user)
                
                if (result) {
                    // Delete Firebase Auth account
                    user.delete().await()
                    
                    Toast.makeText(
                        requireContext(),
                        "Account deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to the login screen
                    findNavController().navigate(R.id.loginFragment)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete account data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error deleting account", e)
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private suspend fun deleteUserData(user: FirebaseUser): Boolean {
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val habitRepository = HabitRepository()
        val habitEntryRepository = HabitEntryRepository()
        
        try {
            // 1. Get all user's habits (including deleted ones)
            val habits = habitRepository.getHabitsForCurrentUser(includeDeleted = true)
            
            // 2. Delete all habit entries for each habit
            for (habit in habits) {
                habitEntryRepository.deleteEntriesForHabit(habit.id)
            }
            
            // 3. Delete all habits
            for (habit in habits) {
                habitRepository.deleteHabit(habit.id)
            }
            
            // 4. Delete user document from users collection
            db.collection("users").document(userId).delete().await()
            
            return true
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error deleting user data", e)
            return false
        }
    }

    private fun showChangePasswordDialog() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.email != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setMessage("We'll send a password reset link to your email: ${currentUser.email}")
                .setPositiveButton("Send Link") { _, _ ->
                    sendPasswordResetEmail(currentUser.email!!)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(
                requireContext(),
                "You must be logged in with an email address",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Password reset link sent to your email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 