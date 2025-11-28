package com.example.mobilellmchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.ModelMetadata

class AvailableModelsAdapter(
    private val models: List<ModelMetadata>,
    private val onDownloadClick: (ModelMetadata) -> Unit
) : RecyclerView.Adapter<AvailableModelsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModelName: TextView = view.findViewById(R.id.tvModelName)
        val tvModelSize: TextView = view.findViewById(R.id.tvModelSize)
        val tvModelDescription: TextView = view.findViewById(R.id.tvModelDescription)
        val tvRequiredRam: TextView = view.findViewById(R.id.tvRequiredRam)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]

        holder.tvModelName.text = model.displayName
        holder.tvModelSize.text = formatSize(model.size)
        holder.tvModelDescription.text = model.description
        holder.tvRequiredRam.text = "需要内存: ${model.requiredRam} MB"

        holder.btnDownload.setOnClickListener {
            onDownloadClick(model)
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
