package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** The one way the app draws text: size and line height are set by each caller, the typeface comes from the theme. */
@Composable
internal fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.current.onBackground,
    size: TextUnit = 16.sp,
    lineHeight: TextUnit = size * 1.45f,
    weight: FontWeight? = null,
    textAlign: TextAlign? = null,
    decoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            color = color,
            fontFamily = AppTheme.current.fontFamily,
            fontSize = size,
            lineHeight = lineHeight,
            fontWeight = weight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDecoration = decoration,
        ),
    )
}
