package org.schabi.newpipe.player.subtitle

object TimestampParser {

    private val CLOCK_REGEX = Regex("""(\d{2,})?:?(\d{2}):(\d{2})[.,:;](\d{2,3})""")

    private val SECONDS_REGEX = Regex("""^(\d+\.?\d*)\s*s?${'$'}""")

    fun parse(raw: String): Long {
        val trimmed = raw.trim()
        return when {
            trimmed.contains(":") -> parseClockFormat(trimmed)
            trimmed.matches(SECONDS_REGEX) -> parseSecondsFormat(trimmed)
            else -> trimmed.toLongOrNull() ?: 0L
        }
    }

    private fun parseClockFormat(text: String): Long {
        val parts = text.split(":", ";", limit = 4)
        val lastPart = parts.last().replace(",", ".")

        return when (parts.size) {
            4 -> {
                val frameMillis = (parts[3].toDouble() * 1000.0 / 30.0).toLong()
                parts[0].toLong() * 3_600_000 +
                    parts[1].toLong() * 60_000 +
                    parts[2].toLong() * 1_000 + frameMillis
            }
            3 -> {
                val subseconds = (lastPart.toDouble() * 1000).toLong()
                parts[0].toLong() * 3_600_000 +
                    parts[1].toLong() * 60_000 + subseconds
            }
            2 -> {
                val subseconds = (lastPart.toDouble() * 1000).toLong()
                parts[0].toLong() * 60_000 + subseconds
            }
            else -> 0L
        }
    }

    private fun parseSecondsFormat(text: String): Long {
        val cleaned = text.removeSuffix("s").trim()
        return (cleaned.toDouble() * 1000).toLong()
    }
}
