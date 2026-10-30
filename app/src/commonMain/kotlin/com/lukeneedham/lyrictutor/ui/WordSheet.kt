package com.lukeneedham.lyrictutor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.action_close
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The container of the word sheet: the standard modal bottom sheet, which opens half way and is dragged up to fill the screen, its content
 * scrolling once it is all the way up. Only the sheet itself is Material: every colour and the shape come from [AppTheme]. Its content is
 * [WordSheetContent]. Nothing is drawn while [selected] is null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordSheet(
    selected: SelectedWord?,
    wiktionary: WiktionaryUiState,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (selected == null) return
    val theme = AppTheme.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val closeDescription = stringResource(Res.string.action_close)
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shape,
        containerColor = theme.surface,
        contentColor = theme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = {
            // The handle closes the sheet: its touch area is wider and taller than what is drawn
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(48.dp)
                    .semantics { contentDescription = closeDescription }
                    .clickable(role = Role.Button) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.border))
            }
        },
    ) {
        // The sheet is the scrolling part: dragging the content moves the sheet first and scrolls the content once it is all the way up
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp).verticalScroll(rememberScrollState())) {
            WordSheetContent(
                word = selected.surface,
                language = selected.language,
                wiktionary = wiktionary,
                onOpenUrl = onOpenUrl,
                modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 32.dp),
            )
        }
    }
}
