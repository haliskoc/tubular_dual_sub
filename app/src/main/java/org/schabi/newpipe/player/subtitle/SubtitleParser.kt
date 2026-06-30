package org.schabi.newpipe.player.subtitle

import java.io.BufferedReader
import java.io.StringReader
import org.jsoup.Jsoup
import org.jsoup.parser.Parser as JsoupParser

object SubtitleParser {

    enum class Format { VTT, TTML, UNKNOWN }

    fun parse(body: String, mimeType: String?): List<SubtitleCue> {
        return when (detectFormat(mimeType)) {
            Format.VTT -> parseVtt(BufferedReader(StringReader(body)))

            Format.TTML -> parseTtml(BufferedReader(StringReader(body)))

            Format.UNKNOWN -> {
                val cues = try {
                    parseVtt(BufferedReader(StringReader(body)))
                } catch (_: Exception) {
                    emptyList()
                }
                if (cues.isEmpty()) parseTtml(BufferedReader(StringReader(body))) else cues
            }
        }
    }

    private fun detectFormat(mimeType: String?): Format = when {
        mimeType == null -> Format.UNKNOWN
        mimeType.contains("ttml", true) -> Format.TTML
        mimeType.contains("vtt", true) || mimeType.contains("text/") -> Format.VTT
        else -> Format.UNKNOWN
    }

    private fun parseVtt(reader: BufferedReader): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var state = 0
        var startMs: Long = 0
        var endMs: Long = 0
        val textBuilder = StringBuilder()

        reader.forEachLine { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("WEBVTT") || trimmed.isEmpty() ||
                    trimmed.startsWith("NOTE") || trimmed.startsWith("STYLE") -> {
                    if (state >= 2 && textBuilder.isNotEmpty()) {
                        cues.add(SubtitleCue(startMs, endMs, textBuilder.toString().trim()))
                        textBuilder.clear()
                    }
                    state = 0
                }

                state == 0 && !trimmed.contains("-->") -> { /* skip ID line */ }

                trimmed.contains("-->") -> {
                    val parts = trimmed.split(Regex("\\s*-->\\s*"), limit = 2)
                    if (parts.size == 2) {
                        startMs = TimestampParser.parse(parts[0])
                        endMs = TimestampParser.parse(parts[1].split(" ")[0])
                        state = 1
                        textBuilder.clear()
                    }
                }

                state == 1 -> {
                    textBuilder.append(Jsoup.parse(trimmed).text())
                    state = 2
                }

                state == 2 && trimmed.isNotEmpty() -> {
                    textBuilder.append("\n").append(Jsoup.parse(trimmed).text())
                }
            }
        }

        if (state >= 2 && textBuilder.isNotEmpty()) {
            cues.add(SubtitleCue(startMs, endMs, textBuilder.toString().trim()))
        }

        return cues.toList().sorted()
    }

    private fun parseTtml(reader: BufferedReader): List<SubtitleCue> {
        val xml = reader.readText()
        val doc = Jsoup.parse(xml, "", JsoupParser.xmlParser())
        val cues = mutableListOf<SubtitleCue>()

        for (p in doc.select("p[begin][end]")) {
            val begin = p.attr("begin")
            val end = p.attr("end")
            if (begin.isNotEmpty() && end.isNotEmpty()) {
                val text = p.text().trim()
                if (text.isNotEmpty()) {
                    cues.add(
                        SubtitleCue(
                            TimestampParser.parse(begin),
                            TimestampParser.parse(end),
                            text
                        )
                    )
                }
            }
        }

        return cues.toList().sorted()
    }
}
