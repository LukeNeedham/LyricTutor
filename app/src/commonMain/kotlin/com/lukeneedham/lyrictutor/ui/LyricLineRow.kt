package com.lukeneedham.lyrictutor.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.data.annotation.LineAnnotation
import com.lukeneedham.lyrictutor.data.lyrics.LyricLine
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * One line of the lyrics, every word of which can be tapped, and its translation under it. [isTranslating]: the translation is
 * still on its way, so a placeholder takes its place. [selectedToken] is the place of the word whose sheet is open, if it is in this line, and [onSelectedBoundsChange] gets where it is on the screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LyricLineRow(
    line: LyricLine,
    annotation: LineAnnotation?,
    isTranslating: Boolean,
    selectedToken: Int?,
    onWordClick: (word: String, language: String?, tokenIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSelectedBoundsChange: (Rect) -> Unit = {},
) {
    val theme = AppTheme.current
    val tokens = remember(line.text) { line.text.split(' ').filter { it.isNotEmpty() } }
    Column(modifier = modifier.fillMaxWidth()) {
        // Wiktionary is asked about a word when it is tapped, in the language the AI gave to the line
        FlowRow {
            for ((tokenIndex, token) in tokens.withIndex()) {
                val isSelected = tokenIndex == selectedToken
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onWordClick(token, annotation?.languageOf(token), tokenIndex) }
                        .padding(horizontal = 1.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AppText(
                        text = token,
                        color = if (isSelected) theme.primary else theme.onBackground,
                        size = 22.sp,
                        lineHeight = 28.sp,
                        weight = FontWeight.SemiBold,
                        modifier = Modifier
                            .then(if (isSelected) Modifier.onGloballyPositioned { onSelectedBoundsChange(it.boundsInWindow()) } else Modifier)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
        val translation = annotation?.translation
        if (translation != null) {
            AppText(
                text = translation,
                color = theme.onSurfaceVariant,
                size = 16.sp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        } else if (isTranslating) {
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
                label = "pulseAlpha",
            )
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, top = 6.dp)
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .alpha(pulse)
                    .clip(RoundedCornerShape(7.dp))
                    .background(theme.surfaceVariant),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LyricLineRow(
                line = LyricLine("Uno dos tres cuatro"),
                annotation = LineAnnotation(index = 0, translation = "One two three four", language = "Spanish"),
                isTranslating = false,
                selectedToken = 1,
                onWordClick = { _, _, _ -> },
            )
            LyricLineRow(
                line = LyricLine("Cinco seis siete ocho"),
                annotation = null,
                isTranslating = true,
                selectedToken = null,
                onWordClick = { _, _, _ -> },
            )
        }
    }
}
