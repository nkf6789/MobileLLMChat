package com.example.mobilellmchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.ModelMetadata

/**
 * 可下载模型列表适配器
 */
class AvailableModelsAdapter(
    private val onDownloadClick: (ModelMetadata) -> Unit
) : ListAdapter<ModelMetadata, AvailableModelsAdapter.ViewHolder>(ModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvModelName: TextView = itemView.findViewById(R.id.tvModelName)
        private val tvModelSize: TextView = itemView.findViewById(R.id.tvModelSize)
        private val tvModelRam: TextView = itemView.findViewById(R.id.tvModelRam)
        private val tvModelDescription: TextView = itemView.findViewById(R.id.tvModelDescription)
        private val btnDownload: Button = itemView.findViewById(R.id.btnDownload)

        fun bind(model: ModelMetadata) {
            tvModelName.text = model.displayName
            tvModelSize.text = "大小: ${model.getFormattedSize()}"
            tvModelRam.text = "需要: ${model.getFormattedRam()}"  // ✅ 使用新方法
            tvModelDescription.text = model.description

            btnDownload.setOnClickListener {
                onDownloadClick(model)
            }
        }
    }

    private class ModelDiffCallback : DiffUtil.ItemCallback<ModelMetadata>() {
        override fun areItemsTheSame(oldItem: ModelMetadata, newItem: ModelMetadata): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ModelMetadata, newItem: ModelMetadata): Boolean {
            return oldItem == newItem
        }
    }
}
