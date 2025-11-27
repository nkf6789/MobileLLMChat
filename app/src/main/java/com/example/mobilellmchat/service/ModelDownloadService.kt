package com.example.mobilellmchat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.DownloadStatus
import com.example.mobilellmchat.model.DownloadTask
import com.example.mobilellmchat.model.ModelMetadata
import com.example.mobilellmchat.utils.ModelFileManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 模型下载服务
 *
 * 功能：
 * - 后台下载模型文件
 * - 断点续传
 * - 进度通知
 * - SHA256 校验
 */
class ModelDownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "ACTION_PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_MODEL_NAME = "EXTRA_MODEL_NAME"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var fileManager: ModelFileManager
    private lateinit var notificationManager: NotificationManager
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 下载任务状态
    private val _downloadTaskFlow = MutableStateFlow<DownloadTask?>(null)
    val downloadTaskFlow: StateFlow<DownloadTask?> = _downloadTaskFlow

    private var currentDownloadJob: Job? = null
    private var isPaused = false

    inner class LocalBinder : Binder() {
        fun getService(): ModelDownloadService = this@ModelDownloadService
    }

    override fun onCreate() {
        super.onCreate()
        fileManager = ModelFileManager(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME)
                if (modelName != null) {
                    startDownload(modelName)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> pauseDownload()
            ACTION_CANCEL_DOWNLOAD -> cancelDownload()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 开始下载模型
     */
    fun startDownload(modelName: String) {
        if (currentDownloadJob?.isActive == true) {
            android.util.Log.w("DownloadService", "已有下载任务在进行")
            return
        }

        val metadata = ModelMetadata.getByName(modelName)
        if (metadata == null) {
            updateTaskStatus(
                modelName,
                DownloadStatus.FAILED,
                error = "未找到模型元数据"
            )
            return
        }

        // 检查存储空间
        if (!fileManager.hasEnoughSpace(metadata.size)) {
            updateTaskStatus(
                modelName,
                DownloadStatus.FAILED,
                error = "存储空间不足，需要 ${metadata.getFormattedSize()}"
            )
            return
        }

        isPaused = false
        currentDownloadJob = serviceScope.launch {
            downloadModel(metadata)
        }
    }

    /**
     * 暂停下载
     */
    fun pauseDownload() {
        isPaused = true
        currentDownloadJob?.cancel()
        _downloadTaskFlow.value?.let { task ->
            updateTaskStatus(task.modelName, DownloadStatus.PAUSED)
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        _downloadTaskFlow.value?.let { task ->
            fileManager.deleteTempDownload(task.modelName)
            updateTaskStatus(task.modelName, DownloadStatus.CANCELLED)
        }

        // ✅ 使用新 API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }


    /**
     * 执行下载逻辑
     */
    private suspend fun downloadModel(metadata: ModelMetadata) {
        val tempFilePath = fileManager.getTempDownloadPath(metadata.name)
        val tempFile = File(tempFilePath)

        // 计算已下载的字节数（支持断点续传）
        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        updateTaskStatus(
            metadata.name,
            DownloadStatus.DOWNLOADING,
            downloadedBytes = downloadedBytes,
            totalBytes = metadata.size
        )

        try {
            val request = Request.Builder()
                .url(metadata.url)
                .apply {
                    if (downloadedBytes > 0) {
                        addHeader("Range", "bytes=$downloadedBytes-")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("下载失败: HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("响应体为空")
            val totalBytes = downloadedBytes + (body.contentLength())

            body.byteStream().use { input ->
                FileOutputStream(tempFile, true).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = downloadedBytes
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastReadBytes = downloadedBytes

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isPaused) {
                            break
                        }

                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        // 每 500ms 更新一次进度
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime > 500) {
                            val speed = ((totalRead - lastReadBytes) * 1000 / (now - lastUpdateTime))
                            val progress = ((totalRead * 100) / totalBytes).toInt()
                            val timeLeft = if (speed > 0) {
                                (totalBytes - totalRead) / speed
                            } else 0L

                            updateTaskStatus(
                                metadata.name,
                                DownloadStatus.DOWNLOADING,
                                progress = progress,
                                downloadedBytes = totalRead,
                                totalBytes = totalBytes,
                                speed = speed,
                                estimatedTimeLeft = timeLeft
                            )

                            lastUpdateTime = now
                            lastReadBytes = totalRead
                        }
                    }

                    if (isPaused) {
                        updateTaskStatus(metadata.name, DownloadStatus.PAUSED)
                        return
                    }
                }
            }

            // 下载完成，进行 SHA256 校验
            updateNotification("正在校验文件完整性...", 100)

            if (!fileManager.verifyModelIntegrity(metadata.name, metadata.sha256)) {
                tempFile.delete()
                throw Exception("文件校验失败，请重试")
            }

            // 重命名临时文件
            if (!fileManager.finalizeTempDownload(metadata.name)) {
                throw Exception("文件保存失败")
            }

            updateTaskStatus(
                metadata.name,
                DownloadStatus.SUCCESS,
                progress = 100,
                downloadedBytes = metadata.size,
                totalBytes = metadata.size
            )

            // ✅ 使用新 API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            showCompletionNotification(metadata.name)

        } catch (e: Exception) {
            android.util.Log.e("DownloadService", "下载失败", e)
            updateTaskStatus(
                metadata.name,
                DownloadStatus.FAILED,
                error = e.message ?: "未知错误"
            )
        }
    }

    /**
     * 更新任务状态
     */
    private fun updateTaskStatus(
        modelName: String,
        status: DownloadStatus,
        progress: Int = _downloadTaskFlow.value?.progress ?: 0,
        downloadedBytes: Long = _downloadTaskFlow.value?.downloadedBytes ?: 0,
        totalBytes: Long = _downloadTaskFlow.value?.totalBytes ?: 0,
        speed: Long = _downloadTaskFlow.value?.speed ?: 0,
        error: String? = null,
        estimatedTimeLeft: Long = _downloadTaskFlow.value?.estimatedTimeLeft ?: 0
    ) {
        val task = DownloadTask(
            modelName = modelName,
            status = status,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speed = speed,
            error = error,
            estimatedTimeLeft = estimatedTimeLeft
        )

        _downloadTaskFlow.value = task

        // 更新通知
        when (status) {
            DownloadStatus.DOWNLOADING -> {
                updateNotification(
                    "下载中: $modelName",
                    progress,
                    "${task.getProgressText()} | ${task.getSpeedText()}"
                )
            }
            DownloadStatus.PAUSED -> {
                updateNotification("已暂停: $modelName", progress)
            }
            DownloadStatus.FAILED -> {
                updateNotification("下载失败: $error", progress)
            }
            else -> {}
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "模型下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示模型下载进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 更新通知
     */
    private fun updateNotification(
        title: String,
        progress: Int,
        content: String = ""
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * 显示完成通知
     */
    private fun showCompletionNotification(modelName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("下载完成")
            .setContentText(modelName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}
