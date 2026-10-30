package com.lukeneedham.lyrictutor.di

import com.lukeneedham.common.DebugKit

object DebugFlags {
    /** Whether the debug tools are on: only in debug builds. */
    val isEnabled: Boolean get() = DebugKit.enabled
}
