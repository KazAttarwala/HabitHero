package com.example.habithero.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.habithero.databinding.FragmentInsightsBinding
import com.example.habithero.model.Habit
import com.example.habithero.ui.viewmodels.InsightsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var habitAdapter: ArrayAdapter<String>
    private var habitList = listOf<Habit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up habit spinner
        setupHabitSpinner()
        
        // Set up the bar chart
        setupBarChart()
        
        // Set up week navigation
        setupWeekNavigation()
        
        // Set up observers
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Reload habits when fragment becomes visible again
        // This ensures new habits are shown in the dropdown
        viewModel.loadHabits()
    }
    
    private fun setupHabitSpinner() {
        habitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        )
        habitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.habitSpinner.adapter = habitAdapter
        
        binding.habitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (habitList.isNotEmpty() && position < habitList.size) {
                    viewModel.selectHabit(habitList[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupBarChart() {
        val barChart = binding.weeklyBarChart
        
        // Configure X axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        
        // Configure left Y axis
        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        
        // Configure right Y axis
        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false
        
        // Other chart configuration
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setFitBars(true)
        barChart.animateY(500)
    }
    
    private fun setupWeekNavigation() {
        // Previous week button
        binding.prevWeekButton.setOnClickListener {
            viewModel.navigateToPreviousWeek()
        }
        
        // Next week button
        binding.nextWeekButton.setOnClickListener {
            viewModel.navigateToNextWeek()
        }
        
        // Reset to current week button
        binding.resetToCurrentWeekButton.setOnClickListener {
            viewModel.resetToCurrentWeek()
        }
        
        // Disable next week button initially if we're at current week
        updateWeekNavigationButtons(0)
    }
    
    private fun updateWeekNavigationButtons(weekOffset: Int) {
        // Only enable the next week button if not at current week
        binding.nextWeekButton.isEnabled = weekOffset < 0
        
        // Always enable the previous week button
        binding.prevWeekButton.isEnabled = true
        
        // Only enable reset button if not at current week
        binding.resetToCurrentWeekButton.isEnabled = weekOffset < 0
    }
    
    private fun observeViewModel() {
        // Observe habits for spinner
        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            habitList = habits
            val habitNames = habits.map { it.title }
            habitAdapter.clear()
            habitAdapter.addAll(habitNames)
            habitAdapter.notifyDataSetChanged()
        }
        
        // Observe selected habit
        viewModel.selectedHabit.observe(viewLifecycleOwner) { habit ->
            binding.currentStreakTextView.text = "${habit.streak} days"
            
            // Update completion rate
            val completionRate = viewModel.getCompletionRate()
            binding.completionRateTextView.text = "$completionRate%"
        }
        
        // Observe weekly data for chart
        viewModel.weeklyData.observe(viewLifecycleOwner) { weeklyData ->
            updateBarChart(weeklyData)
            
            // Update completion rate when data changes
            val completionRate = viewModel.getCompletionRate()
            binding.completionRateTextView.text = "$completionRate%"
        }
        
        // Observe week offset
        viewModel.weekOffset.observe(viewLifecycleOwner) { offset ->
            updateWeekNavigationButtons(offset)
        }
        
        // Observe date range
        viewModel.dateRange.observe(viewLifecycleOwner) { dateRange ->
            binding.weekRangeTextView.text = dateRange
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
    
    private fun updateBarChart(weeklyData: Map<String, Int>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // Add data to entries and labels
        weeklyData.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Daily Progress")
        dataSet.color = Color.rgb(65, 105, 225) // Royal Blue
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        
        // Set frequency target line if we have a selected habit
        viewModel.selectedHabit.value?.let { habit ->
            dataSet.setDrawValues(true)
        }
        
        // Create bar data
        val barData = BarData(dataSet)
        
        // Get the chart and update it
        val barChart = binding.weeklyBarChart
        barChart.data = barData
        
        // Set X axis labels
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        
        // Refresh the chart
        barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 