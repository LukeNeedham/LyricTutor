package com.lukeneedham.lyrictutor.data.art

import androidx.compose.ui.graphics.Color

/** The two colours of an album cover: what the page of its song is painted with. */
data class ArtColors(
    /** The most prominent colour of the cover. */
    val primary: Color,
    /** The next most prominent colour that is clearly different from [primary]. */
    val secondary: Color,
)
