package com.lukeneedham.lyrictutor.ui

/** How the translating of the lyrics is going. The annotations that did arrive are shown whatever the status. */
enum class AnnotationStatus { Idle, Loading, MissingKey, InvalidKey, RateLimited, DailyLimit, Failed }
