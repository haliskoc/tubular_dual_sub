package org.schabi.newpipe.player.subtitle

data class SubtitleCue(
    @JvmField val startMs: Long,
    @JvmField val endMs: Long,
    @JvmField val text: String
) : Comparable<SubtitleCue> {

    override fun compareTo(other: SubtitleCue): Int = startMs.compareTo(other.startMs)

    companion object {
        val BY_START_MS = Comparator<SubtitleCue> { a, b ->
            a.startMs.compareTo(b.startMs)
        }
    }
}
