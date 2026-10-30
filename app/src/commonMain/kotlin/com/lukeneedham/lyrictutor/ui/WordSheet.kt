package com.lukeneedham.lyrictutor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme

/** The container of the word sheet: a scrim and a surface at the bottom. Its content is [WordSheetContent]. */
@Composable
internal fun WordSheet(word: String?, wiktionary: WiktionaryUiState, onOpenUrl: (String) -> Unit, onHeightChange: (Int) -> Unit, onDismiss: () -> Unit) {
    val theme = AppTheme.current
    // The last word stays on screen while the sheet slides away
    var shown by remember { mutableStateOf(word) }
    if (word != null) shown = word

    AnimatedVisibility(visible = word != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = word != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            shown?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        // The height is passed on, so that the song can scroll the tapped word above the sheet
                        .onSizeChanged { onHeightChange(it.height) }
                        .background(theme.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        // Taps on the sheet itself must not reach the scrim
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .verticalScroll(rememberScrollState()),
                ) {
                    WordSheetContent(word = it, wiktionary = wiktionary, onOpenUrl = onOpenUrl, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}
