package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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

/**
 * The name of a language in a pill. With [onClick] it is a button, [isSelected] when it is the one the songs are narrowed to; the touch area
 * is 48 dp high around the pill. Without it, it is only a label.
 */
@Composable
internal fun LanguageTag(
    language: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val theme = AppTheme.current
    val pill = @Composable {
        AppText(
            text = language,
            color = if (isSelected) theme.onPrimary else theme.onPrimaryContainer,
            size = 13.sp,
            weight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (isSelected) theme.primary else theme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
    if (onClick == null) {
        Box(modifier = modifier) { pill() }
    } else {
        Box(
            modifier = modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { pill() }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) { LanguageTag(language = "Spanish", onClick = {}) }
    }
}
