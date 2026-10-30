package com.lukeneedham.lyrictutor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A rounded text field whose border turns to the accent colour when focused, and red when [isError]. [trailing] sits inside its end. */
@Composable
internal fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val theme = AppTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> theme.error
        isFocused -> theme.primary
        else -> theme.border
    }
    val textStyle = TextStyle(color = theme.onSurface, fontFamily = theme.fontFamily, fontSize = 16.sp, lineHeight = 24.sp)
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        cursorBrush = SolidColor(theme.primary),
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(theme.surface, shape)
                    .border(if (isFocused || isError) 2.dp else 1.dp, borderColor, shape)
                    .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) {
                        AppText(text = placeholder, color = theme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                    }
                    innerTextField()
                }
                trailing?.invoke()
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AppTextField(value = "", onValueChange = {}, placeholder = "https://open.spotify.com/track/…")
        }
    }
}
