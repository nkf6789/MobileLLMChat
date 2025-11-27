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
import com.example.mobilellmchat.utils.ModelFileManager

/**
 * 已下载模型列表适配器
 */
class DownloadedModelsAdapter(
    private val onDeleteClick: (ModelFileManager.ModelFileInfo) -> Unit
) : ListAdapter<ModelFileManager.ModelFileInfo, DownloadedModelsAdapter.ViewHolder>(
    ModelFileDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_downloaded, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDownloadedModelName: TextView =
            itemView.findViewById(R.id.tvDownloadedModelName)
        private val tvDownloadedModelSize: TextView =
            itemView.findViewById(R.id.tvDownloadedModelSize)
        private val btnDeleteModel: Button = itemView.findViewById(R.id.btnDeleteModel)

        fun bind(model: ModelFileManager.ModelFileInfo) {
            tvDownloadedModelName.text = model.name
            tvDownloadedModelSize.text = model.sizeFormatted

            btnDeleteModel.setOnClickListener {
                onDeleteClick(model)
            }
        }
    }

    private class ModelFileDiffCallback : DiffUtil.ItemCallback<ModelFileManager.ModelFileInfo>() {
        override fun areItemsTheSame(
            oldItem: ModelFileManager.ModelFileInfo,
            newItem: ModelFileManager.ModelFileInfo
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(
            oldItem: ModelFileManager.ModelFileInfo,
            newItem: ModelFileManager.ModelFileInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}
