package com.fridgelist.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao
}
