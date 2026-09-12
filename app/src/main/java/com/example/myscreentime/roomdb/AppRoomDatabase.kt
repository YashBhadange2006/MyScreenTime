package com.example.myscreentime.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppUsageEntity::class, TotalUsageEntity::class, ActivityDataEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun usageDao(): UsageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_data_table` (
                        `date` TEXT NOT NULL,
                        `walkingMs` INTEGER NOT NULL,
                        `walkingUpstairsMs` INTEGER NOT NULL,
                        `walkingDownstairsMs` INTEGER NOT NULL,
                        `sittingMs` INTEGER NOT NULL,
                        `standingMs` INTEGER NOT NULL,
                        `layingMs` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_usage_table ADD COLUMN lastTimeUsed INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getInstance(context: Context): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "app_usage_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
