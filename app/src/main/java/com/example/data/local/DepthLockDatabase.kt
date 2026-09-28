package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DepthWallpaper

@Database(entities = [DepthWallpaper::class], version = 1, exportSchema = false)
abstract class DepthLockDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: DepthLockDatabase? = null

        fun getDatabase(context: Context): DepthLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DepthLockDatabase::class.java,
                    "depthlock_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
