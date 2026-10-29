package com.lukeneedham.lyrictutor.data.track

/** Reads the `og:` and `music:` meta tags of `open.spotify.com/track/<id>`. Pure, so it is tested on a saved page. */
object SpotifyPageParser {
    private val metaTag = Regex("<meta\\s[^>]*>", RegexOption.IGNORE_CASE)
    private val attribute = Regex("([a-zA-Z:_-]+)\\s*=\\s*\"([^\"]*)\"")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")

    /** Fails with [TrackResolveException.Unreadable] when there is no title or no artist. */
    fun parse(trackId: String, html: String): Result<TrackInfo> {
        val tags = readTags(html)

        // "Cuco, The Marías · Si Me Voy (with The Marías) · Song · 2023"
        val description = tags["og:description"]?.split(" · ").orEmpty()

        val title = tags["og:title"]?.takeIf { it.isNotBlank() } ?: description.getOrNull(1)
        val artists = (tags["music:musician_description"] ?: description.firstOrNull())
            ?.split(", ")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        if (title.isNullOrBlank()) return Result.failure(TrackResolveException.Unreadable("No title in the track page"))
        if (artists.isEmpty()) return Result.failure(TrackResolveException.Unreadable("No artist in the track page"))

        return Result.success(
            TrackInfo(
                trackId = trackId,
                title = title,
                searchTitle = TitleCleaner.clean(title),
                artists = artists,
                coverUrl = tags["og:image"],
                durationSeconds = tags["music:duration"]?.toIntOrNull(),
                releaseDate = tags["music:release_date"],
            ),
        )
    }

    /** The first value of each property: the page repeats some (the allowed countries), which are ignored. */
    private fun readTags(html: String): Map<String, String> {
        val tags = LinkedHashMap<String, String>()
        for (tag in metaTag.findAll(html)) {
            val attributes = attribute.findAll(tag.value).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
            val key = attributes["property"] ?: attributes["name"] ?: continue
            val content = attributes["content"] ?: continue
            if (key !in tags) tags[key] = decodeEntities(content)
        }
        return tags
    }

    private fun decodeEntities(text: String): String =
        text.replace(numericEntity) { match ->
            val radix = if (match.groupValues[1].isEmpty()) 10 else 16
            match.groupValues[2].toIntOrNull(radix)?.takeIf { it in 1..0xFFFF }?.let { Char(it).toString() } ?: match.value
        }
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
}
