package com.bimarihaunter.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OutbreakReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(reports: List<OutbreakReportEntity>)
    
    @Query("SELECT * FROM outbreak_reports ORDER BY published_at DESC LIMIT 50")
    fun getAllReports(): Flow<List<OutbreakReportEntity>>
    
    @Query("SELECT * FROM outbreak_reports WHERE disease = :disease ORDER BY published_at DESC")
    fun getReportsByDisease(disease: String): Flow<List<OutbreakReportEntity>>
    
    @Query("DELETE FROM outbreak_reports WHERE cached_at < :cutoffTime")
    fun deleteOldReports(cutoffTime: Long)

    @Query("DELETE FROM outbreak_reports WHERE id NOT IN (SELECT id FROM outbreak_reports ORDER BY cached_at DESC LIMIT :maxCount)")
    fun trimReports(maxCount: Int)
    
    @Query("SELECT COUNT(*) FROM outbreak_reports")
    fun getReportCount(): Int
}
