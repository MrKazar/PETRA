package com.example.porjectpetra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.porjectpetra.data.local.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC")
    fun getAllMetrics(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE id = :metricId")
    fun getMetricById(metricId: Long): Flow<BodyMetric?>

    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMetric(): Flow<BodyMetric?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: BodyMetric): Long

    @Update
    suspend fun updateMetric(metric: BodyMetric)

    @Delete
    suspend fun deleteMetric(metric: BodyMetric)
}
