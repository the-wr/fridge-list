package com.fridgelist.app.data.model

enum class ProviderType {
    TODOIST,
    MICROSOFT_TODO,
    GOOGLE_TASKS,
    TICKTICK
}

enum class TileState {
    NEEDED,
    NOT_NEEDED
}

/**
 * A grocery item tile on the grid.
 * Stored in Room. Each tile maps to one task in the connected todo provider.
 */
data class Tile(
    val id: Long = 0,
    val gridRow: Int,
    val gridCol: Int,
    val iconName: String,          // e.g. "tomato" — matches the SVG asset name
    val taskName: String,          // e.g. "Tomato" — the task name in the provider
    val taskId: String?,           // provider-specific task ID, null if not yet synced
    val state: TileState = TileState.NOT_NEEDED,
    val isOffGrid: Boolean = false // tile was displaced when grid was shrunk
)

data class GridConfig(
    val columns: Int = 8,
    val rows: Int = 11
)

/**
 * Represents an item in the icon catalog.
 */
data class IconItem(
    val name: String,
    val displayName: String,
    val category: IconCategory
)

enum class IconCategory(val displayName: String) {
    VEGETABLES("Vegetables"),
    FRUIT("Fruit"),
    DAIRY_EGGS("Dairy & Eggs"),
    MEAT("Meat"),
    FISH_SEAFOOD("Fish & Seafood"),
    BAKERY("Bakery"),
    DRY_GOODS("Dry Goods & Pantry"),
    OILS_CONDIMENTS("Oils, Sauces & Condiments"),
    SPICES_HERBS("Spices & Herbs"),
    DRINKS("Drinks"),
    FROZEN("Frozen"),
    SNACKS("Snacks"),
    HOUSEHOLD("Household"),
    PERSONAL_CARE("Personal Care"),
    BABY_PET("Baby & Pet")
}

sealed class SyncResult {
    object Success : SyncResult()
    data class Failure(val message: String) : SyncResult()
    object AuthRequired : SyncResult()
    object Offline : SyncResult()
}

sealed class AppError {
    object Offline : AppError()
    object AuthRequired : AppError()
    data class SyncFailed(val message: String) : AppError()
}
