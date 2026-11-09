package com.lukeneedham.lyrictutor.data.annotation

/** The models a key can use, to choose one in Settings instead of typing its code. */
interface ModelCatalog {
    /** The model codes, best first. [useCodingPlan]: the key is of a coding subscription, which has its own endpoint (not every service has one). Fails with an [AnnotationException]: [AnnotationException.MissingKey] for a blank key. */
    suspend fun list(apiKey: String, useCodingPlan: Boolean = false): Result<List<String>>
}
