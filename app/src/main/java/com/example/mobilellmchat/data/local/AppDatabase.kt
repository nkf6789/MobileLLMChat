package com.example.mobilellmchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mobilellmchat.data.local.dao.ConversationDao
import com.example.mobilellmchat.data.local.entity.ChatMessageEntity
import com.example.mobilellmchat.data.local.entity.ConversationEntity

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilellm_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 未来版本升级时使用的迁移示例
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 示例：添加新字段
                // database.execSQL("ALTER TABLE conversations ADD COLUMN new_column TEXT")
            }
        }
    }
}
