package com.example.mobilellmchat.model

/**
 * 下载状态
 */
enum class DownloadStatus {
    /** 等待开始 */
    PENDING,

    /** 下载中 */
    DOWNLOADING,

    /** 已暂停 */
    PAUSED,

    /** 下载成功 */
    SUCCESS,

    /** 下载失败 */
    FAILED,

    /** 已取消 */
    CANCELLED;

    /**
     * 是否为终止状态
     */
    fun isTerminal(): Boolean {
        return this in listOf(SUCCESS, FAILED, CANCELLED)
    }

    /**
     * 是否可恢复
     */
    fun isResumable(): Boolean {
        return this in listOf(PAUSED, FAILED)
    }
}

/**
 * 下载任务信息
 */
data class DownloadTask(
    val modelName: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0, // 0-100
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: Long = 0, // 字节/秒
    val error: String? = null,
    val startTime: Long = System.currentTimeMillis(),
    val estimatedTimeLeft: Long = 0 // 秒
) {
    /**
     * 获取进度百分比文本
     */
    fun getProgressText(): String = "$progress%"

    /**
     * 获取下载速度文本
     */
    fun getSpeedText(): String {
        return when {
            speed < 1024 -> "$speed B/s"
            speed < 1024 * 1024 -> "${speed / 1024} KB/s"
            else -> String.format("%.2f MB/s", speed / (1024.0 * 1024))
        }
    }

    /**
     * 获取剩余时间文本
     */
    fun getTimeLeftText(): String {
        if (estimatedTimeLeft <= 0) return "计算中..."

        val hours = estimatedTimeLeft / 3600
        val minutes = (estimatedTimeLeft % 3600) / 60
        val seconds = estimatedTimeLeft % 60

        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟${seconds}秒"
            else -> "${seconds}秒"
        }
    }
}
