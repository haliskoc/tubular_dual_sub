package org.schabi.newpipe.player.subtitle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
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
    ): Result<List<SubtitleCue>> {
        cache[languageTag]?.let { return Result.success(it) }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        SubtitleLoadException("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(
                        SubtitleLoadException("Empty response body")
                    )

                val reader = BufferedReader(StringReader(body))
                val cues = SubtitleParser.parse(reader, mimeType)

                if (cues.isEmpty()) {
                    return@withContext Result.failure(
                        SubtitleLoadException("Parsed subtitle list is empty")
                    )
                }

                cache[languageTag] = cues
                Result.success(cues)
            } catch (e: Exception) {
                Result.failure(SubtitleLoadException("Download/parse failed", e))
            }
        }
    }

    fun getCuesSync(url: String, mimeType: String?, languageTag: String): Result<List<SubtitleCue>> {
        cache[languageTag]?.let { return Result.success(it) }

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return Result.failure(
                    SubtitleLoadException("HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body?.string()
                ?: return Result.failure(SubtitleLoadException("Empty response body"))

            val reader = BufferedReader(StringReader(body))
            val cues = SubtitleParser.parse(reader, mimeType)

            if (cues.isEmpty()) {
                return Result.failure(SubtitleLoadException("Parsed subtitle list is empty"))
            }

            cache[languageTag] = cues
            Result.success(cues)
        } catch (e: Exception) {
            Result.failure(SubtitleLoadException("Download/parse failed", e))
        }
    }

    fun clear() {
        cache.clear()
    }

    fun hasCache(languageTag: String): Boolean = cache.containsKey(languageTag)
}

class SubtitleLoadException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
