package com.example.habithero.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.habithero.R
import com.example.habithero.databinding.FragmentHomeBinding
import com.example.habithero.model.Habit
import com.example.habithero.repository.UserRepository
import com.example.habithero.ui.adapters.HabitAdapter
import com.example.habithero.ui.viewmodels.HomeViewModel
import com.example.habithero.utils.HabitCompletionEffect
import com.example.habithero.utils.SoundFileCreator
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository()
        
        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, navigate to login
            findNavController().navigate(R.id.loginFragment)
            return
        }
        
        // Create a sound file for habit completion (only needed for this example)
        SoundFileCreator.createSuccessSound(requireContext())
        
        // Set user email in the UI
        binding.userEmailTextView.text = currentUser.email
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Set up observers
        observeViewModel()
        
        // Set up add habit button click
        binding.addHabitButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_habitEditFragment)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload habits when fragment resumes (e.g., after adding/editing a habit)
        viewModel.loadHabits()
    }
    
    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onHabitClick = { habit ->
                navigateToEditHabit(habit)
            },
            onHabitCompleteToggle = { habit ->
                viewModel.toggleHabitCompletion(habit)
                // Play confetti and sound when completed
                if (!habit.completed) {
                    HabitCompletionEffect.playCompletionEffects(requireContext(), binding.konfettiView)
                }
            },
            onProgressIncrement = { habit ->
                val oldProgress = habit.progress
                viewModel.incrementHabitProgress(habit)
                
                // If this increment caused the habit to be completed, show celebration
                if (oldProgress + 1 >= habit.frequency) {
                    HabitCompletionEffect.playCompletionEffects(requireContext(), binding.konfettiView)
                }
            }
        )
        
        binding.habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }
    }
    
    private fun observeViewModel() {
        // Observe habits
        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            habitAdapter.submitList(habits)

            // Show/hide empty state
            binding.emptyStateTextView.visibility = if (habits.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToEditHabit(habit: Habit) {
        val bundle = Bundle().apply {
            putString("habitId", habit.id)
        }
        findNavController().navigate(R.id.action_homeFragment_to_habitEditFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 