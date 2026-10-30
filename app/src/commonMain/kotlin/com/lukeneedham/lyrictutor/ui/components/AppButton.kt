package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal enum class AppButtonStyle { Primary, Secondary, Tonal, Plain }

/** A pill button. [Primary] is the main action of a view, [Secondary] the same in the second colour, [Tonal] a quieter one, [Plain] only coloured text. */
@Composable
internal fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    icon: AppIcon? = null,
) {
    val theme = AppTheme.current
    val container = when (style) {
        AppButtonStyle.Primary -> theme.primary
        AppButtonStyle.Secondary -> theme.secondary
        AppButtonStyle.Tonal -> theme.primaryContainer
        AppButtonStyle.Plain -> androidx.compose.ui.graphics.Color.Transparent
    }
    val content = when (style) {
        AppButtonStyle.Primary -> theme.onPrimary
        AppButtonStyle.Secondary -> theme.onSecondary
        AppButtonStyle.Tonal -> theme.onPrimaryContainer
        AppButtonStyle.Plain -> theme.primary
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) AppIconImage(icon = icon, tint = content, iconSize = 20.dp)
        AppText(text = text, color = content, size = 15.sp, weight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            AppButton(text = "Play", onClick = {}, icon = AppIcon.Play)
            AppButton(text = "Again", onClick = {}, style = AppButtonStyle.Secondary, icon = AppIcon.Refresh)
            AppButton(text = "Again", onClick = {}, style = AppButtonStyle.Tonal, icon = AppIcon.Refresh)
            AppButton(text = "Cancel", onClick = {}, style = AppButtonStyle.Plain)
        }
    }
}
