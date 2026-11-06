package com.lukeneedham.lyrictutor.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TrackExtrasFinderTest {
    private class FakeCatalog(private val match: CatalogMatch?) : SongCatalog {
        override suspend fun find(title: String, artist: String, durationSeconds: Int?) = match
    }

    private class FakeLinks(private val links: StreamingLinks?) : StreamingLinkFinder {
        var asked: String? = null
        override suspend fun find(pageUrl: String): StreamingLinks? {
            asked = pageUrl
            return links
        }
    }

    @Test
    fun findsTheLinksFromThePageOfTheMatch() = runTest {
        val links = FakeLinks(StreamingLinks("https://open.spotify.com/track/x", "https://www.youtube.com/watch?v=y"))
        val extras = TrackExtrasFinder(FakeCatalog(CatalogMatch("cover", "https://music.apple.com/song")), links).find("t", "a", null)

        assertEquals(TrackExtras("cover", "https://open.spotify.com/track/x", "https://www.youtube.com/watch?v=y"), extras)
        assertEquals("https://music.apple.com/song", links.asked)
    }

    @Test
    fun keepsTheCoverWhenNoLinksAreFound() = runTest {
        val extras = TrackExtrasFinder(FakeCatalog(CatalogMatch("cover", "page")), FakeLinks(null)).find("t", "a", null)
        assertEquals(TrackExtras("cover", null, null), extras)
    }

    @Test
    fun givesNothingWhenTheCatalogHasNoMatch() = runTest {
        val links = FakeLinks(null)
        assertEquals(TrackExtras(null, null, null), TrackExtrasFinder(FakeCatalog(null), links).find("t", "a", null))
        assertEquals(null, links.asked)
    }
}
