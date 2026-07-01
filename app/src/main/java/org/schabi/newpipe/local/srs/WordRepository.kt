package org.schabi.newpipe.local.srs

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.database.srs.WordCardDAO
import org.schabi.newpipe.database.srs.WordCardEntity
import java.time.OffsetDateTime

class WordRepository(private val dao: WordCardDAO) {

    fun getDueCards(): Flowable<List<WordCardEntity>> {
        return dao.getDueCards(OffsetDateTime.now())
            .subscribeOn(Schedulers.io())
    }

    fun getDueCount(): Flowable<Int> {
        return dao.getDueCount(OffsetDateTime.now())
            .subscribeOn(Schedulers.io())
    }

    fun getCardsByLanguage(lang: String): Flowable<List<WordCardEntity>> {
        return dao.getCardsByLanguage(lang)
            .subscribeOn(Schedulers.io())
    }

    fun addCard(
        word: String,
        sentence: String,
        translation: String?,
        videoTitle: String?,
        videoUrl: String?,
        channelName: String?,
        timestampMs: Long,
        languageTag: String = "en"
    ): Long {
        val card = WordCardEntity(
            word = word,
            sentence = sentence,
            translation = translation,
            videoTitle = videoTitle,
            videoUrl = videoUrl,
            channelName = channelName,
            timestampMs = timestampMs,
            nextReview = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            languageTag = languageTag
        )
        return dao.insert(card)
    }

    fun reviewCard(card: WordCardEntity, quality: Int) {
        val result = SM2Algorithm.calculate(
            interval = card.interval,
            easeFactor = card.easeFactor,
            repetitions = card.repetitions,
            quality = quality
        )
        val updatedCard = card.copy(
            interval = result.interval,
            easeFactor = result.easeFactor,
            repetitions = result.repetitions,
            nextReview = result.nextReview,
            lastReviewed = OffsetDateTime.now()
        )
        dao.update(updatedCard)
    }

    fun updateCard(card: WordCardEntity) {
        dao.update(card)
    }

    fun deleteCard(card: WordCardEntity) {
        dao.delete(card)
    }

    fun deleteAll() {
        dao.deleteAll()
    }
}
