package com.example.habithero.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.habithero.R
import com.example.habithero.model.Habit

class HabitAdapter(
    private val onHabitClick: (Habit) -> Unit,
    private val onProgressIncrement: (Habit) -> Unit,
    private val onHabitDelete: (Habit) -> Unit
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
        private val progressTextView: TextView = itemView.findViewById(R.id.habitProgressTextView)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.habitProgressBar)
        private val incrementButton: ImageButton = itemView.findViewById(R.id.incrementButton)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

        fun bind(habit: Habit) {
            titleTextView.text = habit.title
            descriptionTextView.text = habit.description
            
            // Configure progress display
            progressTextView.text = "Progress: ${habit.progress}/${habit.frequency}"
            
            // Calculate and set progress percentage
            val progressPercentage = if (habit.frequency > 0) {
                (habit.progress.toFloat() / habit.frequency.toFloat() * 100).toInt()
            } else {
                0
            }
            progressBar.progress = progressPercentage
            
            // Enable/disable increment button based on completion status
            incrementButton.isEnabled = !habit.completed && habit.progress < habit.frequency
            
            // Handle click on the whole item
            itemView.setOnClickListener {
                onHabitClick(habit)
            }

            // Handle increment button click
            incrementButton.setOnClickListener {
                onProgressIncrement(habit)
            }

            // Setup menu button
            menuButton.setOnClickListener { view ->
                showPopupMenu(view, habit)
            }
        }

        private fun showPopupMenu(view: View, habit: Habit) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.habit_menu)
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        onHabitDelete(habit)
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
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