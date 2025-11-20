package com.example.mobilellmchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mobilellmchat.data.local.entity.ConversationEntity
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity

@Database(entities = [ConversationEntity::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    // 确保你的 ChatRepository 中使用的是 conversationDao() 还是 chatDao()，请保持一致
    // 如果之前代码用的是 chatDao，请在这里加一行 abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                ).fallbackToDestructiveMigration() // 开发阶段允许重建数据库
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
