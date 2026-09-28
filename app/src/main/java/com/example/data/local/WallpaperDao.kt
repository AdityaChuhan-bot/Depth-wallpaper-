package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DepthWallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM depth_wallpapers ORDER BY createdAt DESC")
    fun getAllWallpapers(): Flow<List<DepthWallpaper>>

    @Query("SELECT * FROM depth_wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: Long): DepthWallpaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: DepthWallpaper): Long

    @Update
    suspend fun updateWallpaper(wallpaper: DepthWallpaper)

    @Delete
    suspend fun deleteWallpaper(wallpaper: DepthWallpaper)

    @Query("DELETE FROM depth_wallpapers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM depth_wallpapers")
    fun getWallpaperCount(): Flow<Int>
}
