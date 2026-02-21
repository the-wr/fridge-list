package com.fridgelist.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

/**
 * Renders a grocery icon by name. Falls back to a placeholder if the icon asset
 * is not found (e.g. during development before all SVGs are generated).
 */
@Composable
fun GroceryIcon(
    iconName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    val resId = remember(iconName) {
        context.resources.getIdentifier(
            "ic_grocery_$iconName",
            "drawable",
            context.packageName
        )
    }

    if (resId != 0) {
        val vector = ImageVector.vectorResource(id = resId)
        Image(
            imageVector = vector,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = colorFilter
        )
    } else {
        // Placeholder while SVG assets are not yet added
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}
