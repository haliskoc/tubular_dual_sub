package org.schabi.newpipe.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.NewPipeDatabase
import org.schabi.newpipe.R
import org.schabi.newpipe.database.srs.WordCardEntity
import org.schabi.newpipe.local.srs.AnkiConnectClient
import org.schabi.newpipe.local.srs.ApkgExporter
import org.schabi.newpipe.local.srs.WordRepository
import org.schabi.newpipe.player.ui.SrsReviewDialogFragment
import java.io.IOException

class SrsSettingsFragment : BasePreferenceFragment() {

    private val disposables = CompositeDisposable()
    private lateinit var repository: WordRepository
    private var allCards: List<WordCardEntity> = emptyList()

    companion object {
        private const val REQUEST_CODE_SAVE_FILE = 5858
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResourceRegistry()

        val database = NewPipeDatabase.getInstance(requireContext())
        repository = WordRepository(database.wordCardDAO())

        val disposable = repository.getDueCount()
            .subscribeOn(Schedulers.io())
            .subscribe({}, {})
        disposables.add(disposable)

        findPreference<Preference>("srs_start_review")?.setOnPreferenceClickListener {
            val dialog = SrsReviewDialogFragment.newInstance(null)
            dialog.show(parentFragmentManager, "SrsReviewDialog")
            true
        }

        findPreference<Preference>("srs_export_apkg")?.setOnPreferenceClickListener {
            val dbDisposable = repository.getDueCards()
                .subscribeOn(Schedulers.io())
                .firstOrError()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cards ->
                    if (cards.isEmpty()) {
                        Toast.makeText(context, "No cards to export!", Toast.LENGTH_SHORT).show()
                    } else {
                        allCards = cards
                        startExportFilePicker()
                    }
                }, {
                    Toast.makeText(context, R.string.srs_export_failed, Toast.LENGTH_SHORT).show()
                })
            disposables.add(dbDisposable)
            true
        }

        findPreference<Preference>("srs_sync_ankiconnect")?.setOnPreferenceClickListener {
            val syncDisposable = repository.getDueCards()
                .subscribeOn(Schedulers.io())
                .firstOrError()
                .observeOn(Schedulers.io())
                .subscribe({ cards ->
                    if (cards.isEmpty()) {
                        AndroidSchedulers.mainThread().scheduleDirect {
                            Toast.makeText(context, "No cards to sync!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val client = AnkiConnectClient()
                        if (!client.testConnection()) {
                            AndroidSchedulers.mainThread().scheduleDirect {
                                Toast.makeText(context, R.string.srs_sync_failed, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            var successCount = 0
                            for (card in cards) {
                                val translation = card.translation ?: "No translation"
                                val noteId = client.addNote(
                                    word = card.word,
                                    translation = translation,
                                    sentence = card.sentence,
                                    videoTitle = card.videoTitle ?: "YouTube Video"
                                )
                                if (noteId != null) {
                                    successCount++
                                    repository.updateCard(card.copy(isSyncedToAnki = true, ankiNoteId = noteId))
                                }
                            }
                            AndroidSchedulers.mainThread().scheduleDirect {
                                Toast.makeText(context, getString(R.string.srs_sync_success) + " ($successCount cards)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }, {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        Toast.makeText(context, R.string.srs_sync_failed, Toast.LENGTH_SHORT).show()
                    }
                })
            disposables.add(syncDisposable)
            true
        }

        findPreference<Preference>("srs_delete_all")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.srs_delete_all_confirm_title)
                .setMessage(R.string.srs_delete_all_confirm_message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    Schedulers.io().scheduleDirect {
                        repository.deleteAll()
                    }
                    Toast.makeText(context, "All cards deleted!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
            true
        }
    }

    private fun startExportFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/tab-separated-values"
            putExtra(Intent.EXTRA_TITLE, "tubular_anki_deck.txt")
        }
        startActivityForResult(intent, REQUEST_CODE_SAVE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SAVE_FILE && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                    ApkgExporter.exportToTsv(allCards, os)
                    Toast.makeText(context, R.string.srs_export_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, R.string.srs_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
