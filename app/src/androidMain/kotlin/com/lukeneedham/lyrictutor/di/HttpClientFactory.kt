package com.lukeneedham.lyrictutor.di

import com.lukeneedham.common.network.NetworkLogInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/** The one HTTP client of the app: Ktor on OkHttp, with every call recorded for the debug kit network page. */
object HttpClientFactory {
    // The default of the OkHttp engine is ten seconds for each of these, too short for Gemini; its call sets its own limits
    private const val REQUEST_TIMEOUT_MILLIS = 30_000L
    private const val CONNECT_TIMEOUT_MILLIS = 15_000L
    private const val SOCKET_TIMEOUT_MILLIS = 30_000L

    fun create(): HttpClient =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
            engine { addInterceptor(NetworkLogInterceptor()) }
        }
}
