package org.schabi.newpipe.player.subtitle

import org.schabi.newpipe.extractor.stream.SubtitlesStream

object SecondaryCaptionHelper {

    val TRANSLATION_TARGETS = linkedMapOf(
        "tr" to "Turkish",
        "de" to "German",
        "fr" to "French",
        "es" to "Spanish",
        "ru" to "Russian",
        "ar" to "Arabic",
        "pt" to "Portuguese",
        "it" to "Italian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh-Hans" to "Chinese (Simplified)"
    )

    fun findStreamForLanguage(
        streams: List<SubtitlesStream>,
        languageTag: String
    ): SubtitlesStream? {
        if (streams.isEmpty()) return null

        streams.find { it.languageTag == languageTag }?.let { return it }

        streams.find { it.languageTag.contains(languageTag) }?.let { return it }

        streams.find {
            it.displayLanguageName.lowercase().contains(languageTag.lowercase())
        }?.let { return it }

        return null
    }

    fun buildTranslatedUrl(
        sourceStream: SubtitlesStream,
        targetLanguageTag: String
    ): String {
        val baseUrl = sourceStream.content.toString()
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl${separator}tlang=$targetLanguageTag"
    }
}
