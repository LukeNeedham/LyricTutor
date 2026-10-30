package com.lukeneedham.lyrictutor.data

import com.lukeneedham.lyrictutor.data.wiktionary.WiktionaryPageCache
class RoomWiktionaryPageCache(private val dao: WiktionaryPageDao) : WiktionaryPageCache {
    override suspend fun get(title: String): String? = dao.get(title)?.body

    override suspend fun put(title: String, body: String) {
        dao.insert(WiktionaryPageEntity(title = title, body = body, createdAt = System.currentTimeMillis()))
    }
}
