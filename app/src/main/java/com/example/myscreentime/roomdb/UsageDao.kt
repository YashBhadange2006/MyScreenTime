package com.example.myscreentime.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageData(usageList: List<AppUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTotalData(totalData: TotalUsageEntity)

    @Query("SELECT * FROM app_usage_table WHERE date = :date ORDER BY totalTimeInForeground DESC")
    suspend fun getUsageRowsForDate(date: String): List<AppUsageEntity>

    @Query("SELECT * FROM total_usage_table WHERE date = :date LIMIT 1")
    suspend fun getTotalUsageForDate(date: String): TotalUsageEntity?

    @Query("SELECT date FROM total_usage_table ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSavedDate(): String?

    // Query to delete data past 30 days
    @Query("DELETE FROM app_usage_table WHERE date < :cutoffDate")
    suspend fun deleteOldAppUsage(cutoffDate: String)

    @Query("DELETE FROM total_usage_table WHERE date < :cutoffDate")
    suspend fun deleteOldTotalUsage(cutoffDate: String)
}
