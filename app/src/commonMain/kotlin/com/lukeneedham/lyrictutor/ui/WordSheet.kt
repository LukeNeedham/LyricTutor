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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.action_close
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** The container of the word sheet: a scrim and a surface at the bottom. Its content is [WordSheetContent]. */
@Composable
internal fun WordSheet(
    selected: SelectedWord?,
    wiktionary: WiktionaryUiState,
    onOpenUrl: (String) -> Unit,
    onHeightChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = AppTheme.current
    val closeDescription = stringResource(Res.string.action_close)
    // The last word stays on screen while the sheet slides away
    var shown by remember { mutableStateOf(selected) }
    if (selected != null) shown = selected

    AnimatedVisibility(visible = selected != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            shown?.let { word ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // A fixed height, so that the sheet does not change size when what Wiktionary says arrives
                        .fillMaxHeight(0.55f)
                        // The height is passed on, so that the song can scroll the tapped word above the sheet
                        .onSizeChanged { onHeightChange(it.height) }
                        .background(theme.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        // Taps on the sheet itself must not reach the scrim
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // The handle closes the sheet: its touch area is wider and taller than what is drawn
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(120.dp)
                                .height(48.dp)
                                .semantics { contentDescription = closeDescription }
                                .clickable(role = Role.Button, onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.border))
                        }
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            WordSheetContent(
                                word = word.surface,
                                language = word.language,
                                wiktionary = wiktionary,
                                onOpenUrl = onOpenUrl,
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 0.dp, bottom = 24.dp).navigationBarsPadding(),
                            )
                        }
                    }
                }
            }
        }
    }
}
