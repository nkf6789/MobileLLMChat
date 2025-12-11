package com.example.mobilellmchat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.AvailableModelsAdapter
import com.example.mobilellmchat.adapter.DownloadedModelsAdapter
import com.example.mobilellmchat.model.DownloadProgress
import com.example.mobilellmchat.model.ModelMetadata
import com.example.mobilellmchat.service.ModelDownloadService
import com.example.mobilellmchat.utils.ModelFileManager
import com.google.android.material.snackbar.Snackbar
import java.io.File

class DownloadActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DownloadActivity"
    }

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var tvAvailableSpace: TextView
    private lateinit var tvModelsSize: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var btnCleanTemp: Button
    private lateinit var recyclerAvailableModels: RecyclerView
    private lateinit var recyclerDownloadedModels: RecyclerView
    private lateinit var tvNoDownloadedModels: TextView
    private lateinit var cardDownloadTask: CardView
    private lateinit var tvDownloadingModel: TextView
    private lateinit var tvDownloadProgress: TextView
    private lateinit var tvDownloadSpeed: TextView
    private lateinit var tvDownloadTimeLeft: TextView
    private lateinit var progressDownload: ProgressBar
    private lateinit var btnCancelDownload: Button

    // Data
    private val availableModels = mutableListOf<ModelMetadata>()
    private val downloadedModels = mutableListOf<ModelMetadata>()
    private lateinit var availableModelsAdapter: AvailableModelsAdapter
    private lateinit var downloadedModelsAdapter: DownloadedModelsAdapter
    private var currentDownloadingModel: String? = null

    // Broadcast Receiver
    private val downloadProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ModelDownloadService.ACTION_DOWNLOAD_PROGRESS -> {
                    val progress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("progress", DownloadProgress::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("progress")
                    }
                    progress?.let { updateDownloadProgress(it) }
                }
                ModelDownloadService.ACTION_DOWNLOAD_COMPLETE -> {
                    val modelName = intent.getStringExtra("model_name") ?: return
                    onDownloadComplete(modelName)
                }
                ModelDownloadService.ACTION_DOWNLOAD_ERROR -> {
                    val error = intent.getStringExtra("error") ?: "未知错误"
                    onDownloadError(error)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        initViews()
        setupToolbar()
        setupRecyclerViews()
        loadModels()
        updateStorageInfo()
        registerBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadProgressReceiver)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvAvailableSpace = findViewById(R.id.tvAvailableSpace)
        tvModelsSize = findViewById(R.id.tvModelsSize)
        progressStorage = findViewById(R.id.progressStorage)
        btnCleanTemp = findViewById(R.id.btnCleanTemp)
        recyclerAvailableModels = findViewById(R.id.recyclerAvailableModels)
        recyclerDownloadedModels = findViewById(R.id.recyclerDownloadedModels)
        tvNoDownloadedModels = findViewById(R.id.tvNoDownloadedModels)
        cardDownloadTask = findViewById(R.id.cardDownloadTask)
        tvDownloadingModel = findViewById(R.id.tvDownloadingModel)
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress)
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed)
        tvDownloadTimeLeft = findViewById(R.id.tvDownloadTimeLeft)
        progressDownload = findViewById(R.id.progressDownload)
        btnCancelDownload = findViewById(R.id.btnCancelDownload)

        btnCleanTemp.setOnClickListener { cleanTempFiles() }
        btnCancelDownload.setOnClickListener { cancelDownload() }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "模型下载管理"
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        availableModelsAdapter = AvailableModelsAdapter(availableModels) { model ->
            downloadModel(model)
        }
        recyclerAvailableModels.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = availableModelsAdapter
        }

        downloadedModelsAdapter = DownloadedModelsAdapter(downloadedModels) { model ->
            showDeleteDialog(model)
        }
        recyclerDownloadedModels.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = downloadedModelsAdapter
        }
    }

    private fun loadModels() {
        // ✅ Q4 模型（原有）
        val qwen2Q4Model = ModelMetadata(
            name = "qwen2-0.5b-instruct-q4_0.gguf",
            displayName = "Qwen2-0.5B 中文对话模型 (Q4)",
            size = 352_000_000L,  // 约 336 MB
            requiredRam = 512,
            url = "https://hf-mirror.com/Qwen/Qwen2-0.5B-Instruct-GGUF/resolve/main/qwen2-0_5b-instruct-q4_0.gguf",
            sha256 = "",
            description = "标准版本，平衡速度与质量"
        )

        // ✅ Q3 模型（新增）
        val qwen2Q3Model = ModelMetadata(
            name = "qwen2-0.5b-instruct-q3_k_m.gguf",
            displayName = "Qwen2-0.5B 中文对话模型 (Q3)",
            size = 246_000_000L,  // 约 235 MB
            requiredRam = 384,
            url = "https://hf-mirror.com/Qwen/Qwen2-0.5B-Instruct-GGUF/resolve/main/qwen2-0_5b-instruct-q3_k_m.gguf",
            sha256 = "",
            description = "轻量版本，更小体积更快速度"
        )

        // ✅ TinyLlama（预置模型，不显示在下载列表）
        val tinyLlamaModel = ModelMetadata(
            name = "tinyllama-1.1b-chat-q4_0.gguf",
            displayName = "TinyLlama 1.1B (已预置)",
            size = 607_000_000L,
            requiredRam = 1024,
            url = "https://hf-mirror.com/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_0.gguf",
            sha256 = "",
            description = "预置的英文对话模型（无需下载）"
        )

        availableModels.clear()
        downloadedModels.clear()

        val fileManager = ModelFileManager.getInstance(this)

        // ✅ 检查哪些模型已下载
        listOf(qwen2Q4Model, qwen2Q3Model, tinyLlamaModel).forEach { model ->
            val modelDir = fileManager.getModelDir()
            val modelFile = File(modelDir, model.name)

            if (modelFile.exists() && modelFile.length() > 0) {
                downloadedModels.add(model)
            } else {
                // TinyLlama 已预置，不显示在可下载列表中
                if (model.name != "tinyllama-1.1b-chat-q4_0.gguf") {
                    availableModels.add(model)
                }
            }
        }

        availableModelsAdapter.notifyDataSetChanged()
        downloadedModelsAdapter.notifyDataSetChanged()
        updateDownloadedModelsVisibility()
    }

    private fun updateStorageInfo() {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val availableBytes = statFs.availableBytes
        val totalBytes = statFs.totalBytes

        val availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)

        tvAvailableSpace.text = String.format("可用: %.2f GB / %.2f GB", availableGB, totalGB)

        val fileManager = ModelFileManager.getInstance(this)
        val modelDir = fileManager.getModelDir()
        val modelsSizeMB = modelDir.listFiles()
            ?.filter { it.isFile && it.extension == "gguf" }
            ?.sumOf { it.length() }
            ?.div(1024 * 1024) ?: 0L

        tvModelsSize.text = "模型占用: $modelsSizeMB MB"

        val usedPercent = ((totalBytes - availableBytes) * 100 / totalBytes).toInt()
        progressStorage.progress = usedPercent
    }

    private fun cleanTempFiles() {
        // ✅ 添加确认对话框
        AlertDialog.Builder(this)
            .setTitle("确认清理")
            .setMessage("确定要清理所有临时文件吗？此操作不可恢复。")
            .setPositiveButton("清理") { _, _ ->
                val fileManager = ModelFileManager.getInstance(this)
                val deleted = fileManager.cleanTempFiles()

                Snackbar.make(
                    findViewById(android.R.id.content),
                    "已清理 $deleted 个临时文件",
                    Snackbar.LENGTH_SHORT
                ).show()

                updateStorageInfo()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun downloadModel(model: ModelMetadata) {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val availableBytes = statFs.availableBytes

        if (availableBytes < model.size * 1.1) {
            AlertDialog.Builder(this)
                .setTitle("存储空间不足")
                .setMessage("需要至少 ${model.size / (1024 * 1024)} MB 空间，请清理后重试")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认下载")
            .setMessage("确定要下载 ${model.displayName}（${model.size / (1024 * 1024)} MB）吗？")
            .setPositiveButton("下载") { _, _ ->
                startDownload(model)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startDownload(model: ModelMetadata) {
        currentDownloadingModel = model.name

        val intent = Intent(this, ModelDownloadService::class.java).apply {
            putExtra("model_name", model.name)
            putExtra("model_url", model.url)
            putExtra("model_size", model.size)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        cardDownloadTask.visibility = View.VISIBLE
        tvDownloadingModel.text = "正在下载: ${model.displayName}"

        Snackbar.make(
            findViewById(android.R.id.content),
            "开始下载 ${model.displayName}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun updateDownloadProgress(progress: DownloadProgress) {
        progressDownload.progress = progress.percent
        tvDownloadProgress.text = "${progress.percent}%"
        tvDownloadSpeed.text = progress.speedText
        tvDownloadTimeLeft.text = "剩余时间: ${progress.timeLeftText}"
    }

    private fun onDownloadComplete(modelName: String) {
        cardDownloadTask.visibility = View.GONE
        currentDownloadingModel = null

        Snackbar.make(
            findViewById(android.R.id.content),
            "✅ 下载完成: $modelName",
            Snackbar.LENGTH_LONG
        ).show()

        loadModels()
        updateStorageInfo()
    }

    private fun onDownloadError(error: String) {
        cardDownloadTask.visibility = View.GONE
        currentDownloadingModel = null

        AlertDialog.Builder(this)
            .setTitle("下载失败")
            .setMessage(error)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun cancelDownload() {
        AlertDialog.Builder(this)
            .setTitle("确认取消")
            .setMessage("确定要取消当前下载吗？")
            .setPositiveButton("确定") { _, _ ->
                val intent = Intent(this, ModelDownloadService::class.java)
                stopService(intent)

                cardDownloadTask.visibility = View.GONE
                currentDownloadingModel = null

                Snackbar.make(
                    findViewById(android.R.id.content),
                    "已取消下载",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteDialog(model: ModelMetadata) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除 ${model.displayName} 吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteModel(model)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteModel(model: ModelMetadata) {
        val fileManager = ModelFileManager.getInstance(this)
        val modelDir = fileManager.getModelDir()
        val modelFile = File(modelDir, model.name)

        if (modelFile.exists() && modelFile.delete()) {
            val index = downloadedModels.indexOf(model)
            if (index != -1) {
                downloadedModels.removeAt(index)
                downloadedModelsAdapter.notifyItemRemoved(index)
            }

            availableModels.add(model)
            availableModelsAdapter.notifyItemInserted(availableModels.size - 1)

            updateDownloadedModelsVisibility()
            updateStorageInfo()

            Snackbar.make(
                findViewById(android.R.id.content),
                "已删除 ${model.name}",
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                "删除失败",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateDownloadedModelsVisibility() {
        if (downloadedModels.isEmpty()) {
            tvNoDownloadedModels.visibility = View.VISIBLE
            recyclerDownloadedModels.visibility = View.GONE
        } else {
            tvNoDownloadedModels.visibility = View.GONE
            recyclerDownloadedModels.visibility = View.VISIBLE
        }
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(ModelDownloadService.ACTION_DOWNLOAD_PROGRESS)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_COMPLETE)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_ERROR)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadProgressReceiver, filter)
    }
}