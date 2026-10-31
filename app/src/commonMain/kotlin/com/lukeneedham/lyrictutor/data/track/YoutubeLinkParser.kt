package com.lukeneedham.lyrictutor.data.track

/** Finds a YouTube video id in what the user pasted or shared. Pure, so it is tested without a network. */
object YoutubeLinkParser {
    private const val ID = "[A-Za-z0-9_-]{11}"
    private val patterns = listOf(
        Regex("(?:youtube\\.com|youtube-nocookie\\.com)/watch\\?(?:[^\\s#]*&)?v=($ID)"),
        Regex("youtu\\.be/($ID)"),
        Regex("youtube\\.com/(?:shorts|embed|live|v)/($ID)"),
    )

    /** The video id in [text] (a link, or share text that contains one), or null. */
    fun parseVideoId(text: String): String? = patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1) }

    /** The link that opens a video in the YouTube app (or in the browser). */
    fun videoUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
}
