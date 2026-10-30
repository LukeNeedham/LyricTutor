package com.lukeneedham.lyrictutor.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.lukeneedham.lyrictutor.data.track.SpotifyLinkParser
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Saving the back stack needs every route registered: iOS and the web have no reflection to find them. A new [Route] member is
 * added here too (docs/app-shell.md in LukeNeedham/ci-workflows).
 */
private val backStackConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Home::class, Route.Home.serializer())
            subclass(Route.Song::class, Route.Song.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.Debug::class, Route.Debug.serializer())
            subclass(Route.Logs::class, Route.Logs.serializer())
            subclass(Route.Crashes::class, Route.Crashes.serializer())
        }
    }
}

private const val SLIDE_MILLIS = 320

/** Owns the back stack and the [NavDisplay]. Called only by [Root]. [sharedText] is what another app shared to this one. */
@Composable
fun AppNavigation(sharedText: String? = null) {
    val backStack = rememberNavBackStack(backStackConfig, Route.Home)

    // Text shared to the app (Spotify's Share sheet): open the song it names, if any
    LaunchedEffect(sharedText) {
        val trackId = sharedText?.let(SpotifyLinkParser::parseTrackId) ?: return@LaunchedEffect
        backStack.add(Route.Song(trackId))
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // Going forward the new page slides in from the right and the old one out to the left; going back, the other way round
        transitionSpec = {
            slideInHorizontally(animationSpec = tween(SLIDE_MILLIS)) { it } togetherWith
                slideOutHorizontally(animationSpec = tween(SLIDE_MILLIS)) { -it }
        },
        popTransitionSpec = {
            slideInHorizontally(animationSpec = tween(SLIDE_MILLIS)) { -it } togetherWith
                slideOutHorizontally(animationSpec = tween(SLIDE_MILLIS)) { it }
        },
        entryProvider = { key -> routeEntryProvider(backStack, key) },
    )
}

private fun routeEntryProvider(backStack: NavBackStack<NavKey>, key: NavKey): NavEntry<NavKey> {
    val route = key as Route
    return NavEntry(route) {
        Router(backStack, route)
    }
}
