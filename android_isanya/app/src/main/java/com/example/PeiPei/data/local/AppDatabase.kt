// 文件说明：Room 数据库定义，集中声明实体、DAO 与数据库版本。

package com.example.Lulu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.Lulu.data.local.dao.ChatMessageDao
import com.example.Lulu.data.local.dao.ConversationDao
import com.example.Lulu.data.local.dao.ConversationMemberDao
import com.example.Lulu.data.local.dao.ServiceDao
import com.example.Lulu.data.local.dao.UserDao
import com.example.Lulu.data.model.ChatConversation
import com.example.Lulu.data.model.ChatConversationMember
import com.example.Lulu.data.model.ChatMessage
import com.example.Lulu.data.model.Service
import com.example.Lulu.data.model.User

@Database(
    entities = [
        User::class,
        Service::class,
        ChatConversation::class,
        ChatConversationMember::class,
        ChatMessage::class
    ],
    version = 59,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serviceDao(): ServiceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationMemberDao(): ConversationMemberDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** 清空本地 services 缓存（如旧版同步的广场/发现数据）。 */
        private val MIGRATION_51_52 = object : Migration(51, 52) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM services")
            }
        }

        private val MIGRATION_52_53 = object : Migration(52, 53) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN bookingTimeRangesJson TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_53_54 = object : Migration(53, 54) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN autoAcceptAfterPayment INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_54_55 = object : Migration(54, 55) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN bookingLeadHours REAL NOT NULL DEFAULT 0.5"
                )
            }
        }

        private val MIGRATION_55_56 = object : Migration(55, 56) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN prepaymentPercent INTEGER NOT NULL DEFAULT 30"
                )
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN fullRefundCancelLeadDays INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN serviceDeclarationsExtra TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        private val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_58_59 = object : Migration(58, 59) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE services ADD COLUMN bookingFutureOpenDays INTEGER NOT NULL DEFAULT 30"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) {
                    return existing
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pei_pei_database"
                )
                .addMigrations(
                    MIGRATION_51_52,
                    MIGRATION_52_53,
                    MIGRATION_53_54,
                    MIGRATION_54_55,
                    MIGRATION_55_56,
                    MIGRATION_56_57,
                    MIGRATION_57_58,
                    MIGRATION_58_59,
                )
                .fallbackToDestructiveMigration() // 开发阶段允许破坏性迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
