package com.example.habithero.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.habithero.R
import com.example.habithero.model.Habit

class HabitAdapter(
    private val onHabitClick: (Habit) -> Unit,
    private val onHabitCompleteToggle: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        holder.bind(habit)
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.habitTitleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.habitDescriptionTextView)
        private val completedCheckBox: CheckBox = itemView.findViewById(R.id.habitCompletedCheckBox)

        fun bind(habit: Habit) {
            titleTextView.text = habit.title
            descriptionTextView.text = habit.description
            completedCheckBox.isChecked = habit.completed

            // Handle click on the whole item
            itemView.setOnClickListener {
                onHabitClick(habit)
            }

            // Handle checkbox changes
            completedCheckBox.setOnClickListener {
                onHabitCompleteToggle(habit)
            }
        }
    }

    private class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem == newItem
        }
    }
} 