package com.example.mobilellmchat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 下载进度数据类
 * ✅ 与 ModelDownloadService 和 DownloadActivity 完全匹配
 */
@Parcelize
data class DownloadProgress(
    val percent: Int,              // 下载百分比（0-100）
    val downloadedBytes: Long,     // 已下载字节数
    val totalBytes: Long,          // 总字节数
    val speed: Long,               // 下载速度（字节/秒）
    val speedText: String,         // 格式化的速度文本
    val timeLeft: Long,            // 剩余时间（秒）
    val timeLeftText: String       // 格式化的剩余时间文本
) : Parcelable
