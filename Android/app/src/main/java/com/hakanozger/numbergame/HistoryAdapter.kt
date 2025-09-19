package com.hakanozger.numbergame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val historyList: MutableList<GuessResult>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var isHackerTheme = true

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGuess: TextView = itemView.findViewById(R.id.tvGuess)
        val tvCorrectDigits: TextView = itemView.findViewById(R.id.tvCorrectDigits)
        val tvCorrectPositions: TextView = itemView.findViewById(R.id.tvCorrectPositions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        
        holder.tvGuess.text = item.guess
        holder.tvCorrectDigits.text = item.correctDigits.toString()
        holder.tvCorrectPositions.text = item.correctPositions.toString()
        
        // Apply theme colors
        applyThemeToViewHolder(holder)
        
        // Add subtle animation for new items
        if (position == 0) {
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateTheme(isHacker: Boolean) {
        isHackerTheme = isHacker
        notifyDataSetChanged()
    }

    private fun applyThemeToViewHolder(holder: HistoryViewHolder) {
        val context = holder.itemView.context
        
        if (isHackerTheme) {
            holder.tvGuess.setTextColor(ContextCompat.getColor(context, R.color.hacker_text))
            holder.tvCorrectDigits.setTextColor(ContextCompat.getColor(context, R.color.hacker_accent))
            holder.tvCorrectPositions.setTextColor(ContextCompat.getColor(context, R.color.success_green))
        } else {
            holder.tvGuess.setTextColor(ContextCompat.getColor(context, R.color.modern_text))
            holder.tvCorrectDigits.setTextColor(ContextCompat.getColor(context, R.color.modern_accent))
            holder.tvCorrectPositions.setTextColor(ContextCompat.getColor(context, R.color.success_green))
        }
    }
}
