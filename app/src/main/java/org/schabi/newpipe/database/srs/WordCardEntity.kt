package org.schabi.newpipe.database.srs

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(tableName = "word_cards")
data class WordCardEntity(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "sentence")
    val sentence: String,

    @ColumnInfo(name = "translation")
    val translation: String?,

    @ColumnInfo(name = "videoTitle")
    val videoTitle: String?,

    @ColumnInfo(name = "videoUrl")
    val videoUrl: String?,

    @ColumnInfo(name = "channelName")
    val channelName: String?,

    @ColumnInfo(name = "timestampMs")
    val timestampMs: Long,

    @ColumnInfo(name = "easeFactor")
    val easeFactor: Float = 2.5f,

    @ColumnInfo(name = "interval")
    val interval: Int = 0,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    @ColumnInfo(name = "nextReview")
    val nextReview: OffsetDateTime,

    @ColumnInfo(name = "lastReviewed")
    val lastReviewed: OffsetDateTime? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: OffsetDateTime,

    @ColumnInfo(name = "languageTag")
    val languageTag: String = "en",

    @ColumnInfo(name = "isSyncedToAnki")
    val isSyncedToAnki: Boolean = false,

    @ColumnInfo(name = "ankiNoteId")
    val ankiNoteId: Long? = null
)
