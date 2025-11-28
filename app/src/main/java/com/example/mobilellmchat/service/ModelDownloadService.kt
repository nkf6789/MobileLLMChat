package com.example.mobilellmchat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.mobilellmchat.DownloadActivity
import com.example.mobilellmchat.R
import com.example.mobilellmchat.model.DownloadProgress
import com.example.mobilellmchat.utils.ModelFileManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelDownloadService : Service() {

    companion object {
        private const val TAG = "ModelDownloadService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download_channel"

        const val ACTION_DOWNLOAD_PROGRESS = "com.example.mobilellmchat.DOWNLOAD_PROGRESS"
        const val ACTION_DOWNLOAD_COMPLETE = "com.example.mobilellmchat.DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_ERROR = "com.example.mobilellmchat.DOWNLOAD_ERROR"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private var isCancelled = false

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelName = intent?.getStringExtra("model_name") ?: return START_NOT_STICKY
        val modelUrl = intent.getStringExtra("model_url") ?: return START_NOT_STICKY
        val modelSize = intent.getLongExtra("model_size", 0L)

        startForeground(NOTIFICATION_ID, createNotification("准备下载...", 0))

        downloadJob = serviceScope.launch {
            try {
                downloadFile(modelName, modelUrl, modelSize)
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                sendError(e.message ?: "未知错误")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isCancelled = true
        downloadJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "模型下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示模型下载进度"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String, progress: Int): android.app.Notification {
        val intent = Intent(this, DownloadActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("模型下载中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private suspend fun downloadFile(modelName: String, modelUrl: String, modelSize: Long) {
        val fileManager = ModelFileManager.getInstance(this)
        val tempDir = fileManager.getTempDir()
        val modelDir = fileManager.getModelDir()

        val tempFile = File(tempDir, "$modelName.download")
        val finalFile = File(modelDir, modelName)

        var downloadedSize = if (tempFile.exists()) tempFile.length() else 0L

        val request = Request.Builder()
            .url(modelUrl)
            .header("Range", "bytes=$downloadedSize-")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP错误: ${response.code}")
            }

            val totalSize = response.header("Content-Length")?.toLongOrNull() ?: modelSize
            val inputStream = response.body?.byteStream() ?: throw Exception("无法获取下载流")

            FileOutputStream(tempFile, true).use { output ->
                val buffer = ByteArray(8192)
                var lastUpdateTime = System.currentTimeMillis()
                var lastDownloadedSize = downloadedSize
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        Log.d(TAG, "下载已取消")
                        return
                    }

                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= 500) {
                        val speed = ((downloadedSize - lastDownloadedSize) * 1000 / (currentTime - lastUpdateTime))
                        val percent = ((downloadedSize * 100) / totalSize).toInt()
                        val timeLeft = if (speed > 0) (totalSize - downloadedSize) / speed else 0

                        val progress = DownloadProgress(
                            percent = percent,
                            downloadedBytes = downloadedSize,
                            totalBytes = totalSize,
                            speed = speed,              // ✅ 保持不变（DownloadProgress已定义此参数）
                            speedText = formatSpeed(speed),
                            timeLeft = timeLeft,        // ✅ 保持不变（DownloadProgress已定义此参数）
                            timeLeftText = formatTime(timeLeft)
                        )

                        sendProgress(progress)
                        updateNotification("下载中: $percent%", percent)


                        lastUpdateTime = currentTime
                        lastDownloadedSize = downloadedSize
                    }
                }
            }
        }

        if (!isCancelled) {
            if (tempFile.renameTo(finalFile)) {
                sendComplete(modelName)
                stopSelf()
            } else {
                throw Exception("文件重命名失败")
            }
        }
    }

    private fun sendProgress(progress: DownloadProgress) {
        val intent = Intent(ACTION_DOWNLOAD_PROGRESS).apply {
            putExtra("progress", progress)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendComplete(modelName: String) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE).apply {
            putExtra("model_name", modelName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("下载完成")
            .setContentText(modelName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendError(error: String) {
        val intent = Intent(ACTION_DOWNLOAD_ERROR).apply {
            putExtra("error", error)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun updateNotification(text: String, progress: Int) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(text, progress))
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0))
        }
    }

    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
        }
    }
}
