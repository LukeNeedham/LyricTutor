package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.track.PlayProvider
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.provider_spotify
import com.lukeneedham.lyrictutor.generated.resources.provider_youtube
import com.lukeneedham.lyrictutor.ui.components.AppIcon
import com.lukeneedham.lyrictutor.ui.components.AppIconImage
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The service the play button uses: the chosen one with a chevron, which opens the list of services below it. Choosing one closes the list. */
@Composable
internal fun PlayProviderDropdown(
    selected: PlayProvider,
    onSelect: (PlayProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(role = Role.DropdownList) { expanded = !expanded }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppText(text = label(selected), color = theme.onSurfaceVariant, size = 14.sp, weight = FontWeight.Medium)
            AppIconImage(
                icon = AppIcon.Chevron,
                tint = theme.onSurfaceVariant,
                iconSize = 18.dp,
                modifier = Modifier.rotate(if (expanded) -90f else 90f),
            )
        }
        if (expanded) {
            Column(modifier = Modifier.width(180.dp).clip(RoundedCornerShape(16.dp)).background(theme.surface)) {
                for (provider in PlayProvider.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.RadioButton) {
                                expanded = false
                                onSelect(provider)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = label(provider),
                            color = if (provider == selected) theme.primary else theme.onSurface,
                            weight = if (provider == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (provider == selected) AppIconImage(icon = AppIcon.Check, tint = theme.primary, iconSize = 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun label(provider: PlayProvider) = when (provider) {
    PlayProvider.Spotify -> stringResource(Res.string.provider_spotify)
    PlayProvider.YouTube -> stringResource(Res.string.provider_youtube)
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        PlayProviderDropdown(selected = PlayProvider.YouTube, onSelect = {}, modifier = Modifier.padding(16.dp))
    }
}
