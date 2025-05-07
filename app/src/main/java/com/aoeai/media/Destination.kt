package com.aoeai.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.aoeai.media.view.gallery.AlbumView

/**
 * Contract for information needed on every Rally navigation destination
 */
interface Destination {
    val icon: ImageVector
    val route: Int
    val screen: @Composable () -> Unit
}

/**
 * Rally app navigation destinations
 */
object Home : Destination {
    override val icon = Icons.Filled.Home
    override val route = R.string.photo
    override val screen: @Composable () -> Unit = { AlbumView() }
}

// Screens to be displayed in the top RallyTabRow
val rallyTabRowScreens = listOf(Home)