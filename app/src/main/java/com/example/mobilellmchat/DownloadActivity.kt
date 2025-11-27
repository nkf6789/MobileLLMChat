package com.example.mobilellmchat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilellmchat.adapter.AvailableModelsAdapter
import com.example.mobilellmchat.adapter.DownloadedModelsAdapter
import com.example.mobilellmchat.model.DownloadStatus
import com.example.mobilellmchat.model.ModelMetadata
import com.example.mobilellmchat.service.ModelDownloadService
import com.example.mobilellmchat.utils.ModelFileManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 模型下载管理界面
 *
 * 功能：
 * - 显示可下载和已下载的模型列表
 * - 管理下载任务（开始/暂停/取消）
 * - 显示存储空间信息
 * - 删除已下载的模型
 */
class DownloadActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvAvailableSpace: TextView
    private lateinit var tvModelsSize: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var btnCleanTemp: Button

    private lateinit var cardDownloadTask: CardView
    private lateinit var tvDownloadingModel: TextView
    private lateinit var progressDownload: ProgressBar
    private lateinit var tvDownloadProgress: TextView
    private lateinit var tvDownloadSpeed: TextView
    private lateinit var tvDownloadTimeLeft: TextView
    private lateinit var btnPauseResume: Button
    private lateinit var btnCancelDownload: Button

    private lateinit var recyclerAvailableModels: RecyclerView
    private lateinit var recyclerDownloadedModels: RecyclerView
    private lateinit var tvNoDownloadedModels: TextView

    private lateinit var fileManager: ModelFileManager
    private lateinit var availableModelsAdapter: AvailableModelsAdapter
    private lateinit var downloadedModelsAdapter: DownloadedModelsAdapter

    private var downloadService: ModelDownloadService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ModelDownloadService.LocalBinder
            downloadService = binder.getService()
            serviceBound = true
            observeDownloadTask()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        initViews()
        initFileManager()
        setupToolbar()
        setupRecyclerViews()
        loadStorageInfo()
        loadModels()
        setupListeners()
        bindDownloadService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvAvailableSpace = findViewById(R.id.tvAvailableSpace)
        tvModelsSize = findViewById(R.id.tvModelsSize)
        progressStorage = findViewById(R.id.progressStorage)
        btnCleanTemp = findViewById(R.id.btnCleanTemp)

        cardDownloadTask = findViewById(R.id.cardDownloadTask)
        tvDownloadingModel = findViewById(R.id.tvDownloadingModel)
        progressDownload = findViewById(R.id.progressDownload)
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress)
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed)
        tvDownloadTimeLeft = findViewById(R.id.tvDownloadTimeLeft)
        btnPauseResume = findViewById(R.id.btnPauseResume)
        btnCancelDownload = findViewById(R.id.btnCancelDownload)

        recyclerAvailableModels = findViewById(R.id.recyclerAvailableModels)
        recyclerDownloadedModels = findViewById(R.id.recyclerDownloadedModels)
        tvNoDownloadedModels = findViewById(R.id.tvNoDownloadedModels)
    }

    private fun initFileManager() {
        fileManager = ModelFileManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // 可下载模型列表
        availableModelsAdapter = AvailableModelsAdapter(
            onDownloadClick = { model -> startDownload(model) }
        )
        recyclerAvailableModels.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = availableModelsAdapter
        }

        // 已下载模型列表
        downloadedModelsAdapter = DownloadedModelsAdapter(
            onDeleteClick = { model -> confirmDelete(model) }
        )
        recyclerDownloadedModels.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = downloadedModelsAdapter
        }
    }

    private fun setupListeners() {
        btnCleanTemp.setOnClickListener {
            val count = fileManager.cleanAllTempDownloads()
            Toast.makeText(this, "已清理 $count 个临时文件", Toast.LENGTH_SHORT).show()
            loadStorageInfo()
        }

        btnPauseResume.setOnClickListener {
            downloadService?.pauseDownload()
        }

        btnCancelDownload.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("取消下载")
                .setMessage("确定要取消当前下载吗？")
                .setPositiveButton("确定") { _, _ ->
                    downloadService?.cancelDownload()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun bindDownloadService() {
        val intent = Intent(this, ModelDownloadService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadStorageInfo() {
        val storageInfo = fileManager.getStorageInfo()

        tvAvailableSpace.text = "可用: ${storageInfo.availableFormatted}"
        tvModelsSize.text = "模型占用: ${storageInfo.modelsSizeFormatted}"
        progressStorage.progress = storageInfo.getUsagePercentage()
    }

    private fun loadModels() {
        // 加载可下载模型列表
        val availableModels = ModelMetadata.getAvailableModels()
        val downloadedNames = fileManager.listDownloadedModels().map { it.name }

        // 过滤掉已下载的模型
        val notDownloaded = availableModels.filter { it.name !in downloadedNames }
        availableModelsAdapter.submitList(notDownloaded)

        // 加载已下载模型列表
        val downloadedModels = fileManager.getDownloadedModelsInfo()
        downloadedModelsAdapter.submitList(downloadedModels)

        // 显示/隐藏空状态
        tvNoDownloadedModels.visibility = if (downloadedModels.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun startDownload(model: ModelMetadata) {
        // 检查存储空间
        if (!fileManager.hasEnoughSpace(model.size)) {
            AlertDialog.Builder(this)
                .setTitle("存储空间不足")
                .setMessage("需要 ${model.getFormattedSize()}，" +
                        "当前可用 ${fileManager.getAvailableSpaceFormatted()}")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        // 启动下载服务
        val intent = Intent(this, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_START_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_MODEL_NAME, model.name)
        }
        startService(intent)

        // 绑定服务以监听进度
        if (!serviceBound) {
            bindDownloadService()
        }

        Toast.makeText(this, "开始下载 ${model.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun observeDownloadTask() {
        lifecycleScope.launch {
            downloadService?.downloadTaskFlow?.collectLatest { task ->
                if (task == null) {
                    cardDownloadTask.visibility = View.GONE
                    return@collectLatest
                }

                cardDownloadTask.visibility = View.VISIBLE
                tvDownloadingModel.text = task.modelName
                progressDownload.progress = task.progress
                tvDownloadProgress.text = task.getProgressText()
                tvDownloadSpeed.text = task.getSpeedText()
                tvDownloadTimeLeft.text = "剩余时间: ${task.getTimeLeftText()}"

                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        btnPauseResume.text = "暂停"
                        btnPauseResume.isEnabled = true
                        btnCancelDownload.isEnabled = true
                    }
                    DownloadStatus.PAUSED -> {
                        btnPauseResume.text = "继续"
                        btnPauseResume.isEnabled = true
                        btnCancelDownload.isEnabled = true
                    }
                    DownloadStatus.SUCCESS -> {
                        cardDownloadTask.visibility = View.GONE
                        Toast.makeText(
                            this@DownloadActivity,
                            "下载完成: ${task.modelName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadModels()
                        loadStorageInfo()
                    }
                    DownloadStatus.FAILED -> {
                        cardDownloadTask.visibility = View.GONE
                        AlertDialog.Builder(this@DownloadActivity)
                            .setTitle("下载失败")
                            .setMessage(task.error ?: "未知错误")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                    DownloadStatus.CANCELLED -> {
                        cardDownloadTask.visibility = View.GONE
                        Toast.makeText(
                            this@DownloadActivity,
                            "已取消下载",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun confirmDelete(model: ModelFileManager.ModelFileInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除模型")
            .setMessage("确定要删除 ${model.name} (${model.sizeFormatted}) 吗？")
            .setPositiveButton("删除") { _, _ ->
                if (fileManager.deleteModel(model.name)) {
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                    loadModels()
                    loadStorageInfo()
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
