package org.schabi.newpipe.local.srs

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AnkiConnectClient(private val baseUrl: String = "http://localhost:8765") {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Creates a deck in Anki.
     */
    fun createDeck(deckName: String): Boolean {
        val requestJson = JSONObject().apply {
            put("action", "createDeck")
            put("version", 6)
            put(
                "params",
                JSONObject().apply {
                    put("deck", deckName)
                }
            )
        }
        val request = Request.Builder()
            .url(baseUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates a note in Anki.
     */
    fun addNote(
        word: String,
        translation: String,
        sentence: String,
        videoTitle: String,
        deckName: String = "Tubular",
        tags: List<String> = listOf("tubular", "youtube")
    ): Long? {
        createDeck(deckName) // Ensure deck exists

        val requestJson = JSONObject().apply {
            put("action", "addNote")
            put("version", 6)
            put(
                "params",
                JSONObject().apply {
                    put(
                        "note",
                        JSONObject().apply {
                            put("deckName", deckName)
                            put("modelName", "Basic")
                            put(
                                "fields",
                                JSONObject().apply {
                                    put("Front", word)
                                    put("Back", "$translation\n\nContext: $sentence\n\nSource: $videoTitle")
                                }
                            )
                            put("tags", JSONArray(tags))
                        }
                    )
                }
            )
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val resultJson = JSONObject(bodyString)
                val error = resultJson.optString("error", "")
                if (error.isNotEmpty() && error != "null") return null
                resultJson.optLong("result", -1L)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Test connection to AnkiConnect.
     */
    fun testConnection(): Boolean {
        val requestJson = JSONObject().apply {
            put("action", "version")
            put("version", 6)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
