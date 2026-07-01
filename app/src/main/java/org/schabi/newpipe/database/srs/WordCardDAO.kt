package org.schabi.newpipe.database.srs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.rxjava3.core.Flowable
import java.time.OffsetDateTime

@Dao
interface WordCardDAO {

    @Query("SELECT * FROM word_cards WHERE nextReview <= :now ORDER BY nextReview ASC")
    fun getDueCards(now: OffsetDateTime): Flowable<List<WordCardEntity>>

    @Query("SELECT * FROM word_cards WHERE languageTag = :lang ORDER BY createdAt DESC")
    fun getCardsByLanguage(lang: String): Flowable<List<WordCardEntity>>

    @Query("SELECT * FROM word_cards WHERE word LIKE :query OR sentence LIKE :query")
    fun searchCards(query: String): Flowable<List<WordCardEntity>>

    @Query("SELECT COUNT(*) FROM word_cards WHERE nextReview <= :now")
    fun getDueCount(now: OffsetDateTime): Flowable<Int>

    @Insert
    fun insert(card: WordCardEntity): Long

    @Update
    fun update(card: WordCardEntity)

    @Delete
    fun delete(card: WordCardEntity)

    @Query("DELETE FROM word_cards")
    fun deleteAll()
}
