package org.schabi.newpipe.player.transcript

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import org.schabi.newpipe.databinding.TranscriptItemBinding
import java.util.Locale

class TranscriptAdapter(
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<TranscriptAdapter.ViewHolder>(), Filterable {

    private var items: List<TranscriptItem> = emptyList()
    private var filteredItems: List<TranscriptItem> = emptyList()
    private var activePositionMs: Long = -1L
    private var activeIndex: Int = -1
    private var searchQuery: String = ""

    fun setItems(newItems: List<TranscriptItem>) {
        items = newItems
        filteredItems = newItems
        searchQuery = ""
        updateActiveIndex()
        notifyDataSetChanged()
    }

    fun setActivePosition(positionMs: Long) {
        if (activePositionMs == positionMs) return
        activePositionMs = positionMs
        val oldIndex = activeIndex
        updateActiveIndex()
        if (oldIndex != activeIndex) {
            if (oldIndex >= 0 && oldIndex < filteredItems.size) {
                notifyItemChanged(oldIndex)
            }
            if (activeIndex >= 0 && activeIndex < filteredItems.size) {
                notifyItemChanged(activeIndex)
            }
        }
    }

    fun getActiveIndex(): Int = activeIndex

    private fun updateActiveIndex() {
        activeIndex = filteredItems.indexOfFirst {
            activePositionMs >= it.startMs && activePositionMs < it.endMs
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TranscriptItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.bind(item, position == activeIndex, searchQuery)
    }

    override fun getItemCount(): Int = filteredItems.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.getDefault())?.trim() ?: ""
                val results = FilterResults()
                if (query.isEmpty()) {
                    results.values = items
                } else {
                    results.values = items.filter {
                        it.text.lowercase(Locale.getDefault()).contains(query)
                    }
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = (results?.values as? List<TranscriptItem>) ?: emptyList()
                searchQuery = constraint?.toString() ?: ""
                updateActiveIndex()
                notifyDataSetChanged()
            }
        }
    }

    inner class ViewHolder(private val binding: TranscriptItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TranscriptItem, isActive: Boolean, query: String) {
            binding.timestamp.text = item.formattedTimestamp
            
            // Highlight search query
            val text = item.text
            if (query.isNotEmpty() && text.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))) {
                val spannable = SpannableString(text)
                val queryLower = query.lowercase(Locale.getDefault())
                val textLower = text.lowercase(Locale.getDefault())
                var startPos = textLower.indexOf(queryLower)
                while (startPos >= 0) {
                    spannable.setSpan(
                        BackgroundColorSpan(Color.parseColor("#55FFD54F")), // Translucent orange/yellow
                        startPos,
                        startPos + query.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    startPos = textLower.indexOf(queryLower, startPos + query.length)
                }
                binding.text.text = spannable
            } else {
                binding.text.text = text
            }

            binding.root.isSelected = isActive
            binding.root.setOnClickListener {
                onItemClick(item.startMs)
            }
        }
    }
}
