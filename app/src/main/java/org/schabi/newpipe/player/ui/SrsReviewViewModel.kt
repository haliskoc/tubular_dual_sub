package org.schabi.newpipe.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.schabi.newpipe.NewPipeDatabase
import org.schabi.newpipe.database.srs.WordCardEntity
import org.schabi.newpipe.local.srs.WordRepository

class SrsReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    private val disposables = CompositeDisposable()

    private val _dueCards = MutableLiveData<List<WordCardEntity>>()
    val dueCards: LiveData<List<WordCardEntity>> get() = _dueCards

    private val _currentCard = MutableLiveData<WordCardEntity?>()
    val currentCard: LiveData<WordCardEntity?> get() = _currentCard

    private var currentIndex = 0

    init {
        val database = NewPipeDatabase.getInstance(application)
        repository = WordRepository(database.wordCardDAO())
        loadDueCards()
    }

    fun loadDueCards() {
        val disposable = repository.getDueCards()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ cards ->
                _dueCards.value = cards
                currentIndex = 0
                updateCurrentCard()
            }, {
                _dueCards.value = emptyList()
                _currentCard.value = null
            })
        disposables.add(disposable)
    }

    private fun updateCurrentCard() {
        val cards = _dueCards.value ?: emptyList()
        if (currentIndex >= 0 && currentIndex < cards.size) {
            _currentCard.value = cards[currentIndex]
        } else {
            _currentCard.value = null
        }
    }

    fun review(quality: Int) {
        val card = _currentCard.value ?: return
        
        val disposable = io.reactivex.rxjava3.core.Completable.fromAction {
            repository.reviewCard(card, quality)
        }
        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            currentIndex++
            updateCurrentCard()
        }, {
            // Log error or ignore
        })
        disposables.add(disposable)
    }

    fun getCurrentIndex(): Int = currentIndex

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
