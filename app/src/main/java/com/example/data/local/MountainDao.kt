package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MountainDao {
    @Query("SELECT * FROM mountains ORDER BY altitude DESC")
    fun getAllMountains(): Flow<List<MountainEntity>>

    @Query("SELECT * FROM mountains WHERE isPinned = 1 ORDER BY altitude DESC")
    fun getPinnedMountains(): Flow<List<MountainEntity>>

    @Query("SELECT * FROM mountains WHERE persianName LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR persianProvince LIKE '%' || :query || '%' ORDER BY altitude DESC")
    fun searchMountains(query: String): Flow<List<MountainEntity>>

    @Query("SELECT * FROM mountains WHERE persianProvince = :province ORDER BY altitude DESC")
    fun getMountainsByProvince(province: String): Flow<List<MountainEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMountains(mountains: List<MountainEntity>): List<Long>

    @Update
    suspend fun updateMountain(mountain: MountainEntity): Int

    @Delete
    suspend fun deleteMountain(mountain: MountainEntity): Int

    @Query("UPDATE mountains SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedStatus(id: Int, isPinned: Boolean): Int

    @Query("SELECT COUNT(*) FROM mountains")
    suspend fun getMountainCount(): Int

    @Query("SELECT * FROM mountains")
    suspend fun getMountainList(): List<MountainEntity>
}
