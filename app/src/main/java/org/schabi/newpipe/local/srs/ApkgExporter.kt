package org.schabi.newpipe.local.srs

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import org.schabi.newpipe.database.srs.WordCardEntity

object ApkgExporter {

    /**
     * Exports a list of WordCardEntity to a tab-separated TSV stream that can be imported directly into Anki.
     */
    fun exportToTsv(cards: List<WordCardEntity>, outputStream: OutputStream) {
        outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            for (card in cards) {
                val translation = card.translation ?: ""
                val context = card.sentence
                val source = "${card.videoTitle ?: ""} (${card.channelName ?: ""})"

                val front = card.word
                val back = "$translation<br><br><b>Context:</b> $context<br><b>Source:</b> $source"

                // Escape tabs and newlines for Anki import compatibility
                val escapedFront = front.replace("\t", " ").replace("\n", "<br>")
                val escapedBack = back.replace("\t", " ").replace("\n", "<br>")

                writer.write("$escapedFront\t$escapedBack\n")
            }
        }
    }
}
