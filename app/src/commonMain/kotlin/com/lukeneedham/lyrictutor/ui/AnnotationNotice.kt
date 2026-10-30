package com.lukeneedham.lyrictutor.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.lyrictutor.generated.resources.Res
import com.lukeneedham.lyrictutor.generated.resources.annotation_daily_limit
import com.lukeneedham.lyrictutor.generated.resources.annotation_failed
import com.lukeneedham.lyrictutor.generated.resources.annotation_invalid_key
import com.lukeneedham.lyrictutor.generated.resources.annotation_missing_key
import com.lukeneedham.lyrictutor.generated.resources.annotation_open_settings
import com.lukeneedham.lyrictutor.generated.resources.annotation_progress
import com.lukeneedham.lyrictutor.generated.resources.annotation_rate_limited
import com.lukeneedham.lyrictutor.generated.resources.annotation_retry
import com.lukeneedham.lyrictutor.ui.components.AppButton
import com.lukeneedham.lyrictutor.ui.components.AppText
import com.lukeneedham.lyrictutor.ui.components.Spinner
import com.lukeneedham.lyrictutor.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** How the translating is going: a progress bar while it runs, and what went wrong, with the way to fix it, when it did not. Nothing for [AnnotationStatus.Idle]. */
@Composable
internal fun AnnotationNotice(
    status: AnnotationStatus,
    detail: String?,
    doneLines: Int,
    totalLines: Int,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    val message = when (status) {
        AnnotationStatus.Idle -> return
        AnnotationStatus.Loading -> stringResource(Res.string.annotation_progress, doneLines, totalLines)
        AnnotationStatus.MissingKey -> stringResource(Res.string.annotation_missing_key)
        AnnotationStatus.InvalidKey -> stringResource(Res.string.annotation_invalid_key)
        AnnotationStatus.RateLimited -> stringResource(Res.string.annotation_rate_limited)
        AnnotationStatus.DailyLimit -> stringResource(Res.string.annotation_daily_limit)
        AnnotationStatus.Failed -> stringResource(Res.string.annotation_failed)
    }
    val isLoading = status == AnnotationStatus.Loading
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isLoading) theme.surface else theme.error.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isLoading) Spinner(diameter = 20.dp)
            AppText(
                text = message,
                color = if (isLoading) theme.onSurface else theme.error,
                size = 15.sp,
                weight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
        if (isLoading) {
            val fraction by animateFloatAsState(
                targetValue = if (totalLines > 0) (doneLines.toFloat() / totalLines).coerceIn(0f, 1f) else 0f,
                label = "progress",
            )
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(theme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(theme.primary))
            }
        }
        if (detail != null) AppText(text = detail, color = theme.onSurfaceVariant, size = 13.sp)
        when (status) {
            AnnotationStatus.MissingKey, AnnotationStatus.InvalidKey, AnnotationStatus.DailyLimit ->
                AppButton(text = stringResource(Res.string.annotation_open_settings), onClick = onOpenSettings)
            AnnotationStatus.RateLimited, AnnotationStatus.Failed ->
                AppButton(text = stringResource(Res.string.annotation_retry), onClick = onRetry)
            else -> Unit
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnnotationNotice(
                status = AnnotationStatus.Loading,
                detail = null,
                doneLines = 12,
                totalLines = 40,
                onRetry = {},
                onOpenSettings = {},
            )
            AnnotationNotice(
                status = AnnotationStatus.MissingKey,
                detail = null,
                doneLines = 0,
                totalLines = 40,
                onRetry = {},
                onOpenSettings = {},
            )
        }
    }
}
