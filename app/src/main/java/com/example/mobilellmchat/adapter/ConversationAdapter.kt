package com.example.mobilellmchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.R
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val onConversationClick: (ConversationEntity) -> Unit
) : ListAdapter<ConversationEntity, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ConversationViewHolder(view, onConversationClick)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ConversationViewHolder(
        itemView: View,
        private val onClick: (ConversationEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleView: TextView = itemView.findViewById(android.R.id.text1)
        private val timeView: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(conversation: ConversationEntity) {
            titleView.text = conversation.title
            timeView.text = formatTime(conversation.updatedAt)

            itemView.setOnClickListener {
                onClick(conversation)
            }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}

class ConversationDiffCallback : DiffUtil.ItemCallback<ConversationEntity>() {
    override fun areItemsTheSame(oldItem: ConversationEntity, newItem: ConversationEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ConversationEntity, newItem: ConversationEntity): Boolean {
        return oldItem == newItem
    }
}
