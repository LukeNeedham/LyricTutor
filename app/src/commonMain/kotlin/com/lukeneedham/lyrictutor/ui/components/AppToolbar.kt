package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.action_back
import com.lukeneedham.lyrictutor.generated.resources.action_open_settings
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The toolbar of every page, so that nothing in it moves from page to page: always 64dp high under the status bar, the back button first
 * (when there is [onBack]), then the [title], and the [actions] at the end. [center] is drawn in the middle of the toolbar, over the rest
 * (a page uses it for something that arrives when the page is scrolled). It has no background of its own.
 */
@Composable
internal fun AppToolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    center: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                AppIconButton(icon = AppIcon.Back, contentDescription = stringResource(Res.string.action_back), onClick = onBack)
            }
            if (title != null) {
                AppText(
                    text = title,
                    size = 22.sp,
                    weight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 16.dp),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            actions()
        }
        // Clear of the back button and of the same width on the other side, so that it is centred on the screen
        if (center != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(horizontal = 52.dp)) { center() }
        }
    }
}

/** The way to Settings, the same button in the same place on every page that has it. */
@Composable
internal fun SettingsAction(onClick: () -> Unit) {
    AppIconButton(
        icon = AppIcon.Sliders,
        contentDescription = stringResource(Res.string.action_open_settings),
        onClick = onClick,
    )
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        AppToolbar(title = "Settings", onBack = {}, actions = { SettingsAction(onClick = {}) })
    }
}
