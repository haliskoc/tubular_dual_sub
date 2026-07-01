package org.schabi.newpipe.player.transcript

data class TranscriptItem(
    val startMs: Long,
    val endMs: Long,
    val text: String
) {
    val formattedTimestamp: String
        get() {
            val totalSec = startMs / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }
}
