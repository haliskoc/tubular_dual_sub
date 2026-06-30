package org.schabi.newpipe.player.subtitle

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.SubtitleView
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DualSubtitleSyncEngine(
    private val exoPlayer: ExoPlayer,
    private val secondarySubtitleView: SubtitleView
) {
    companion object {
        private const val BASE_INTERVAL_MS = 100L
        private const val MIN_INTERVAL_MS = 33L
    }

    private var scope: CoroutineScope? = null
    private var cues: List<SubtitleCue> = emptyList()
    private var lastShownIndex: Int = -1
    private var speedMultiplier: Double = 1.0

    val isActive: Boolean get() = scope != null && scope?.isActive == true

    fun start(subtitleCues: List<SubtitleCue>) {
        stop()
        cues = subtitleCues
        lastShownIndex = -1

        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope?.launch { pollLoop() }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        cues = emptyList()
        lastShownIndex = -1

        secondarySubtitleView.setCues(emptyList())
    }

    fun onSeekProcessed() {
        if (!isActive || cues.isEmpty()) return

        val positionMs = exoPlayer.currentPosition
        val index = findCueIndex(positionMs)
        updateView(index)
    }

    fun onPlaybackSpeedChanged(speed: Float) {
        speedMultiplier = speed.toDouble()
    }

    private suspend fun pollLoop() {
        while (scope != null && scope!!.isActive && cues.isNotEmpty()) {
            poll()
            delay(calculateInterval())
        }
    }

    private fun poll() {
        if (cues.isEmpty()) return

        val positionMs = exoPlayer.currentPosition
        if (positionMs < 0) return

        val index = findCueIndex(positionMs)
        updateView(index)
    }

    private fun findCueIndex(positionMs: Long): Int {
        if (cues.isEmpty()) return -1

        var result = cues.binarySearchBy(positionMs) { it.startMs }

        if (result < 0) {
            result = -(result + 1) - 1
        }

        if (result < 0 || result >= cues.size) return -1

        val cue = cues[result]

        if (positionMs >= cue.startMs && positionMs < cue.endMs) {
            return result
        }

        val overlapping = mutableListOf<Int>()
        overlapping.add(result)

        var prev = result - 1
        while (prev >= 0 && cues[prev].endMs > positionMs) {
            overlapping.add(prev)
            prev--
        }

        var next = result + 1
        while (next < cues.size && cues[next].startMs <= positionMs) {
            overlapping.add(next)
            next++
        }

        return when {
            overlapping.isEmpty() -> -1
            overlapping.size == 1 -> overlapping[0]
            else -> overlapping.maxByOrNull { cues[it].endMs - positionMs } ?: overlapping[0]
        }
    }

    private fun updateView(index: Int) {
        if (index == lastShownIndex) return

        if (index < 0 || index >= cues.size) {
            secondarySubtitleView.setCues(emptyList())
            lastShownIndex = -1
        } else {
            secondarySubtitleView.setCues(cues[index].toExoCues())
            lastShownIndex = index
        }
    }

    private fun calculateInterval(): Long {
        return max(MIN_INTERVAL_MS, (BASE_INTERVAL_MS / speedMultiplier).toLong())
    }
}

fun SubtitleCue.toExoCues(): List<Cue> = listOf(
    Cue.Builder()
        .setText(text)
        .setStartTimeUs(startMs * 1000)
        .setEndTimeUs(endMs * 1000)
        .setLine(0.73f, Cue.LINE_TYPE_FRACTION)
        .setPosition(0.5f, Cue.ANCHOR_TYPE_MIDDLE)
        .setTextAlignment(Cue.TEXT_ALIGNMENT_CENTER)
        .setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
        .setSize(0.9f)
        .build()
)
