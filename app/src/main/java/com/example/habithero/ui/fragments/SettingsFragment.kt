package com.example.habithero.ui.fragments

import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private val dummyDataGenerator = DummyDataGenerator()

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
        
        // Set up logout button
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
        
        // Set up developer options buttons
        setupDeveloperOptions()
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