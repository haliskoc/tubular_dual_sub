package org.schabi.newpipe.player.subtitle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SubtitleRepository {

    private val cache: MutableMap<String, List<SubtitleCue>> = linkedMapOf()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getCues(
        url: String,
        mimeType: String?,
        languageTag: String
    ): List<SubtitleCue> {
        cache[languageTag]?.let { return it }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw SubtitleLoadException("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body?.string()
                ?: throw SubtitleLoadException("Empty response body")

            val cues = SubtitleParser.parse(body, mimeType)

            if (cues.isEmpty()) {
                throw SubtitleLoadException("Parsed subtitle list is empty")
            }

            cache[languageTag] = cues
            cues
        }
    }

    fun getCuesSync(url: String, mimeType: String?, languageTag: String): List<SubtitleCue> {
        cache[languageTag]?.let { return it }

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw SubtitleLoadException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body?.string()
            ?: throw SubtitleLoadException("Empty response body")

        val cues = SubtitleParser.parse(body, mimeType)

        if (cues.isEmpty()) {
            throw SubtitleLoadException("Parsed subtitle list is empty")
        }

        cache[languageTag] = cues
        return cues
    }

    fun clear() {
        cache.clear()
    }

    fun hasCache(languageTag: String): Boolean = cache.containsKey(languageTag)
}

class SubtitleLoadException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
