package com.lukeneedham.lyrictutor.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.lukeneedham.common.logger.Logger
import com.lukeneedham.lyrictutor.data.art.ArtColorExtractor
import com.lukeneedham.lyrictutor.data.art.ArtColors
import com.lukeneedham.lyrictutor.data.art.DominantColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the cover with Coil (the image is usually in its cache already, as the page shows it) and reads the colours of a small copy. */
class CoilArtColorExtractor(private val context: Context) : ArtColorExtractor {

    override suspend fun extract(coverUrl: String): ArtColors? = withContext(Dispatchers.Default) {
        try {
            // A small copy is enough to find the colours, and a software bitmap is needed to read its pixels
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .size(SAMPLE_SIZE, SAMPLE_SIZE)
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(context).execute(request)
            val image = (result as? SuccessResult)?.image ?: return@withContext null
            val bitmap = image.toBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            DominantColors.extract(pixels)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Could not read the colours of the cover", e)
            null
        }
    }

    private companion object {
        const val SAMPLE_SIZE = 96
    }
}
