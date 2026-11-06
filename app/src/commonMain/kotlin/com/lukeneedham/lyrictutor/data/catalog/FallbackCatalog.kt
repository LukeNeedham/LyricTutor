package com.lukeneedham.lyrictutor.data.catalog

/** Asks each catalog in turn and uses the first match. */
class FallbackCatalog(private val catalogs: List<SongCatalog>) : SongCatalog {
    override suspend fun find(title: String, artist: String, durationSeconds: Int?): CatalogMatch? {
        for (catalog in catalogs) {
            catalog.find(title, artist, durationSeconds)?.let { return it }
        }
        return null
    }
}
