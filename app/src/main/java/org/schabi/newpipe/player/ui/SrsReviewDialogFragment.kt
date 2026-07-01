package org.schabi.newpipe.player.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import org.schabi.newpipe.databinding.SrsReviewDialogBinding

class SrsReviewDialogFragment : DialogFragment() {

    interface Callback {
        fun onSeekTo(positionMs: Long)
    }

    private var callback: Callback? = null
    private val viewModel: SrsReviewViewModel by viewModels()
    private var _binding: SrsReviewDialogBinding? = null
    private val binding get() = _binding!!

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = SrsReviewDialogBinding.inflate(layoutInflater)

        viewModel.dueCards.observe(this) { cards ->
            if (cards.isNullOrEmpty()) {
                binding.textViewWord.text = "No due cards! 🎉"
                binding.buttonShowAnswer.visibility = View.GONE
                binding.layoutAnswerDetails.visibility = View.GONE
                binding.layoutRatingButtons.visibility = View.GONE
                binding.textViewProgress.text = "0/0"
            } else {
                binding.buttonShowAnswer.visibility = View.VISIBLE
            }
        }

        viewModel.currentCard.observe(this) { card ->
            if (card != null) {
                val total = viewModel.dueCards.value?.size ?: 0
                val current = viewModel.getCurrentIndex() + 1
                binding.textViewProgress.text = "$current / $total"

                binding.textViewWord.text = card.word
                binding.textViewTranslation.text = card.translation ?: "No translation available"
                binding.textViewSentence.text = card.sentence
                binding.textViewVideoLink.text = "🎬 Play: ${card.videoTitle ?: "Video"}"

                binding.layoutAnswerDetails.visibility = View.GONE
                binding.layoutRatingButtons.visibility = View.GONE
                binding.buttonShowAnswer.visibility = View.VISIBLE

                if (callback != null && card.timestampMs > 0) {
                    binding.textViewVideoLink.visibility = View.VISIBLE
                    binding.textViewVideoLink.setOnClickListener {
                        callback?.onSeekTo(card.timestampMs)
                    }
                } else {
                    binding.textViewVideoLink.visibility = View.GONE
                }
            } else {
                val total = viewModel.dueCards.value?.size ?: 0
                if (total > 0) {
                    binding.textViewWord.text = "Review completed! 🌟"
                    binding.textViewProgress.text = "$total / $total"
                }
                binding.buttonShowAnswer.visibility = View.GONE
                binding.layoutAnswerDetails.visibility = View.GONE
                binding.layoutRatingButtons.visibility = View.GONE
            }
        }

        binding.buttonShowAnswer.setOnClickListener {
            binding.buttonShowAnswer.visibility = View.GONE
            binding.layoutAnswerDetails.visibility = View.VISIBLE
            binding.layoutRatingButtons.visibility = View.VISIBLE
        }

        binding.buttonAgain.setOnClickListener { viewModel.review(0) }
        binding.buttonHard.setOnClickListener { viewModel.review(2) }
        binding.buttonGood.setOnClickListener { viewModel.review(3) }
        binding.buttonEasy.setOnClickListener { viewModel.review(4) }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(callback: Callback? = null): SrsReviewDialogFragment {
            val fragment = SrsReviewDialogFragment()
            fragment.callback = callback
            return fragment
        }
    }
}
