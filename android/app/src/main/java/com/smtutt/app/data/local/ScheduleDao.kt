package com.smtutt.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSchedule(schedule: CachedScheduleEntity)

    @Query("SELECT * FROM cached_schedules WHERE targetKey = :targetKey LIMIT 1")
    suspend fun getCachedSchedule(targetKey: String): CachedScheduleEntity?

    @Query("SELECT * FROM cached_schedules ORDER BY cachedAt DESC")
    fun getAllCachedSchedules(): Flow<List<CachedScheduleEntity>>

    @Query("DELETE FROM cached_schedules WHERE targetKey = :targetKey")
    suspend fun deleteCachedSchedule(targetKey: String)

    @Query("DELETE FROM cached_schedules")
    suspend fun clearAllCachedSchedules()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites ORDER BY isDefault DESC, addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultFavorite(): FavoriteEntity?

    @Query("UPDATE favorites SET isDefault = 0")
    suspend fun clearDefaultFavorite()

    @Query("UPDATE favorites SET isDefault = 1 WHERE id = :id")
    suspend fun markDefaultFavorite(id: String)

    @Transaction
    suspend fun setDefaultFavorite(id: String) {
        clearDefaultFavorite()
        markDefaultFavorite(id)
    }

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean
}
