package com.example.habithero.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.habithero.databinding.FragmentHabitEditBinding
import com.example.habithero.ui.viewmodels.HabitEditViewModel
import com.google.android.material.slider.Slider

class HabitEditFragment : Fragment() {
    private var _binding: FragmentHabitEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HabitEditViewModel by viewModels()
    private var habitId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get habit ID from arguments if editing an existing habit
        habitId = arguments?.getString("habitId")
        
        // Set up slider change listener
        binding.frequencySlider.addOnChangeListener { _, value, _ ->
            binding.frequencyValueText.text = value.toInt().toString()
        }
        
        // Set up save button
        binding.saveButton.setOnClickListener {
            saveHabit()
        }
        
        // Set up observers
        observeViewModel()
        
        // Load habit data if editing
        habitId?.let {
            if (it.isNotEmpty()) {
                viewModel.loadHabit(it)
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe habit data for prefilling form when editing
        viewModel.habit.observe(viewLifecycleOwner) { habit ->
            habit?.let {
                binding.titleEditText.setText(it.title)
                binding.descriptionEditText.setText(it.description)
                binding.frequencySlider.value = it.frequency.toFloat()
                binding.frequencyValueText.text = it.frequency.toString()
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isLoading
        }
        
        // Observe save success
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate back to home screen
                findNavController().navigateUp()
            }
        }
        
        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveHabit() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val frequency = binding.frequencySlider.value.toInt()
        
        viewModel.saveHabit(title, description, frequency)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 