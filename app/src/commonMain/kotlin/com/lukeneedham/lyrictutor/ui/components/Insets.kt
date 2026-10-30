package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * The app draws behind the system bars. The toolbar clears the status bar by itself; this is for the bottom of a scrolling list, so that
 * its last item can be scrolled above the navigation bar while the list still passes under it.
 */
@Composable
internal fun navigationBarHeight(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Keeps a page clear of a display cutout or bars at its sides (a phone in landscape), which only matter there. */
@Composable
internal fun Modifier.sideInsetsPadding(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
