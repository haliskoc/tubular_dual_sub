package org.schabi.newpipe.local.srs

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TranslationService(private val apiKey: String) {

    private val client = OkHttpClient()

    /**
     * Translates text to a target language using the DeepL Free API.
     */
    fun translate(text: String, targetLang: String): String? {
        if (apiKey.trim().isEmpty()) return null
        
        val url = "https://api-free.deepl.com/v2/translate"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "DeepL-Auth-Key $apiKey")
            .post(
                FormBody.Builder()
                    .add("text", text)
                    .add("target_lang", targetLang.uppercase())
                    .build()
            )
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val json = JSONObject(bodyString)
                val translations = json.getJSONArray("translations")
                if (translations.length() > 0) {
                    translations.getJSONObject(0).getString("text")
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
