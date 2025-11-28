package com.example.mobilellmchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.ModelMetadata
import java.io.File

class DownloadedModelsAdapter(
    private val models: List<ModelMetadata>,
    private val onDeleteClick: (ModelMetadata) -> Unit
) : RecyclerView.Adapter<DownloadedModelsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModelName: TextView = view.findViewById(R.id.tvDownloadedModelName)
        val tvModelSize: TextView = view.findViewById(R.id.tvDownloadedModelSize)
        val tvDownloadDate: TextView = view.findViewById(R.id.tvDownloadDate)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_downloaded, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]

        holder.tvModelName.text = model.displayName
        holder.tvModelSize.text = formatSize(model.size)
        holder.tvDownloadDate.text = "已下载"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(model)
        }
    }

    override fun getItemCount() = models.size

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
