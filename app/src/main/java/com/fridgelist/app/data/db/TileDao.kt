package com.fridgelist.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TileDao {

    @Query("SELECT * FROM tiles WHERE isOffGrid = 0 ORDER BY gridRow, gridCol")
    fun observeActiveTiles(): Flow<List<TileEntity>>

    @Query("SELECT * FROM tiles WHERE isOffGrid = 1")
    suspend fun getOffGridTiles(): List<TileEntity>

    @Query("SELECT * FROM tiles WHERE id = :id")
    suspend fun getById(id: Long): TileEntity?

    @Query("SELECT * FROM tiles WHERE gridRow = :row AND gridCol = :col AND isOffGrid = 0")
    suspend fun getAtPosition(row: Int, col: Int): TileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tile: TileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiles: List<TileEntity>)

    @Update
    suspend fun update(tile: TileEntity)

    @Delete
    suspend fun delete(tile: TileEntity)

    @Query("DELETE FROM tiles")
    suspend fun deleteAll()

    @Query("UPDATE tiles SET state = :state WHERE id = :id")
    suspend fun updateState(id: Long, state: String)

    @Query("UPDATE tiles SET taskId = :taskId WHERE id = :id")
    suspend fun updateTaskId(id: Long, taskId: String)

    @Query("UPDATE tiles SET taskId = NULL")
    suspend fun clearAllTaskIds()

    @Query("UPDATE tiles SET gridRow = :row, gridCol = :col, isOffGrid = 0 WHERE id = :id")
    suspend fun moveTile(id: Long, row: Int, col: Int)

    @Query("UPDATE tiles SET isOffGrid = 1 WHERE id = :id")
    suspend fun moveToOffGrid(id: Long)

    @Query("SELECT * FROM tiles WHERE taskName = :name LIMIT 1")
    suspend fun findByTaskName(name: String): TileEntity?
}
