package com.lukeneedham.lyrictutor.ui

import androidx.compose.runtime.Composable
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import com.lukeneedham.lyrictutor.ui.theme.toDebugTheme
import com.lukeneedham.common.ui.ProvideDebugSettings

/** The one composable `MainActivity` calls: everything the UI sits in, then the navigation. [sharedText] is text shared to the app. */
@Composable
fun Root(sharedText: String? = null) {
    AppTheme {
        ProvideDebugSettings(theme = AppTheme.current.toDebugTheme()) {
            AppNavigation(sharedText = sharedText)
        }
    }
}
