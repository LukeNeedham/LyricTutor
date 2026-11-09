package com.lukeneedham.lyrictutor.data.annotation

import com.lukeneedham.lyrictutor.data.settings.AiProvider

/** The [ModelCatalog] of each AI service. */
class ModelCatalogs(private val catalogs: Map<AiProvider, ModelCatalog>) {
    fun of(provider: AiProvider): ModelCatalog = catalogs.getValue(provider)
}
