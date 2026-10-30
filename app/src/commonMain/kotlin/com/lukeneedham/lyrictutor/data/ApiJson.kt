package com.lukeneedham.lyrictutor.data

import kotlinx.serialization.json.Json

/**
 * The one lenient `Json` that reads every network answer: an unexpected answer must never crash the app
 * (docs/networking.md in ci-workflows).
 */
internal val apiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
