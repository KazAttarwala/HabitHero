package com.example.habithero.ui.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.habithero.R
import com.example.habithero.databinding.FragmentSettingsBinding
import com.example.habithero.utils.DummyDataGenerator
import com.example.habithero.utils.NotificationPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        
        // Set up logout button
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
        
        // Set up notification settings
        setupNotificationSettings()
        
        // Set up developer options buttons
        setupDeveloperOptions()
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
    }
    
    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        // Navigate back to login
        findNavController().navigate(R.id.loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 