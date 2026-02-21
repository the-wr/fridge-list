package com.fridgelist.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fridgelist.app.data.model.Tile
import com.fridgelist.app.data.model.TileState

@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gridRow: Int,
    val gridCol: Int,
    val iconName: String,
    val taskName: String,
    val taskId: String?,
    val state: String = TileState.NOT_NEEDED.name,
    val isOffGrid: Boolean = false
) {
    fun toTile() = Tile(
        id = id,
        gridRow = gridRow,
        gridCol = gridCol,
        iconName = iconName,
        taskName = taskName,
        taskId = taskId,
        state = TileState.valueOf(state),
        isOffGrid = isOffGrid
    )

    companion object {
        fun fromTile(tile: Tile) = TileEntity(
            id = tile.id,
            gridRow = tile.gridRow,
            gridCol = tile.gridCol,
            iconName = tile.iconName,
            taskName = tile.taskName,
            taskId = tile.taskId,
            state = tile.state.name,
            isOffGrid = tile.isOffGrid
        )
    }
}
