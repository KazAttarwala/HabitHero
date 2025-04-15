package com.example.habithero.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.habithero.databinding.FragmentHabitEditBinding

class HabitEditFragment : Fragment() {
    private var _binding: FragmentHabitEditBinding? = null
    private val binding get() = _binding!!

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
        
        // Set up your habit edit UI interactions here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 