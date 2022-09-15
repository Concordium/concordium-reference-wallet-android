package com.concordium.wallet.ui.passphrase.recover

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.TextView
import androidx.fragment.app.Fragment
import cash.z.ecc.android.bip39.Mnemonics
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseRecoverInputBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.util.KeyboardUtil
import java.util.*
import kotlin.concurrent.schedule

class PassPhraseRecoverInputFragment : Fragment() {
    private var _binding: FragmentPassPhraseRecoverInputBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: PassPhraseRecoverViewModel

    private lateinit var arrayAdapter: WordsPickedRecoverListAdapter
    private var snapTimer: Timer? = null
    private var listViewHasFocus = true
    private var listViewScrollState = SCROLL_STATE_IDLE

    companion object {
        private val WORD_COUNT = Mnemonics.WordCount.COUNT_24.count

        @JvmStatic
        fun newInstance(viewModel: PassPhraseRecoverViewModel) = PassPhraseRecoverInputFragment().apply {
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseRecoverInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _viewModel.loadAllWords()
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        (activity as BaseActivity).setActionBarTitle(R.string.pass_phrase_recover_input_title)
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    override fun onPause() {
        super.onPause()
        KeyboardUtil.hideKeyboard(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeSnapTimer()
        _binding = null
    }

    private fun initObservers() {
        _viewModel.validate.observe(viewLifecycleOwner) { success ->
            if (!success)
                binding.tvError.visibility = View.VISIBLE
            else
                binding.tvError.visibility = View.INVISIBLE
        }
    }

    private fun initViews() {
        initButtons()

        setSuggestionsVisible(false)

        for (i in 0 until WordsPickedBaseListAdapter.OFFSET) {
            _viewModel.wordsPicked[i] = WordsPickedBaseListAdapter.BLANK
        }

        for (i in _viewModel.wordsPicked.size - 1 downTo _viewModel.wordsPicked.size - WordsPickedBaseListAdapter.OFFSET - 1) {
            _viewModel.wordsPicked[i] = WordsPickedBaseListAdapter.BLANK
        }

        arrayAdapter = WordsPickedRecoverListAdapter(requireContext(), _viewModel.wordsPicked)

        arrayAdapter.setWordPickedClickListener { position ->
            arrayAdapter.currentPosition = position
            moveToCurrent()
        }

        arrayAdapter.setOnTextChangeListener { text ->
            lookUp(text)
        }

        arrayAdapter.also { binding.listView.adapter = it }

        initListViewScroll()
    }

    private fun initButtons() {
        binding.btnClearAll.setOnClickListener {
            _viewModel.clearWordsPicked()
            initViews()
            arrayAdapter.notifyDataSetChanged()
        }
        binding.btnClearBelow.setOnClickListener {
            for (i in arrayAdapter.currentPosition + 1 until _viewModel.wordsPicked.size - 3) {
                _viewModel.wordsPicked[i] = null
            }
            arrayAdapter.notifyDataSetChanged()
        }
        binding.tvSuggest1.setOnClickListener {
            insertSuggestion(binding.tvSuggest1)
        }
        binding.tvSuggest2.setOnClickListener {
            insertSuggestion(binding.tvSuggest2)
        }
        binding.tvSuggest3.setOnClickListener {
            insertSuggestion(binding.tvSuggest3)
        }
        binding.tvSuggest4.setOnClickListener {
            insertSuggestion(binding.tvSuggest4)
        }
    }

    private fun lookUp(text: String) {
        if (listViewScrollState != SCROLL_STATE_IDLE)
            return

        setSuggestionsVisible(false)
        if (text.length <= 1)
            return
        val filtered = _viewModel.allWords.filter { it.startsWith(text) }
        if (filtered.isNotEmpty()) {
            binding.tvSuggest1.text = filtered[0]
            binding.tvSuggest1.visibility = View.VISIBLE
            if (filtered.size > 1) {
                binding.tvSuggest2.text = filtered[1]
                binding.tvSuggest2.visibility = View.VISIBLE
            }
            if (filtered.size > 2) {
                binding.tvSuggest3.text = filtered[2]
                binding.tvSuggest3.visibility = View.VISIBLE
            }
            if (filtered.size > 3) {
                binding.tvSuggest4.text = filtered[3]
                binding.tvSuggest4.visibility = View.VISIBLE
            }
        }
    }

    private fun setSuggestionsVisible(isVisible: Boolean) {
        binding.tvSuggest1.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        binding.tvSuggest2.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        binding.tvSuggest3.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        binding.tvSuggest4.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun initListViewScroll() {
        binding.listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                listViewScrollState = scrollState
                if (listViewScrollState == SCROLL_STATE_TOUCH_SCROLL)
                    listViewHasFocus = true
            }
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                val changeTo = firstVisibleItem + 2
                if (changeTo != arrayAdapter.currentPosition && listViewHasFocus) {
                    if (arrayAdapter.currentPosition != changeTo) {
                        arrayAdapter.currentPosition = changeTo
                        startSnapTimer()
                        arrayAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun removeSnapTimer() {
        snapTimer?.cancel()
        snapTimer?.purge()
        snapTimer = null
    }

    private fun startSnapTimer() {
        if (snapTimer == null) {
            snapTimer = Timer()
            snapTimer?.schedule(50) {
                activity?.runOnUiThread {
                    if (listViewScrollState == SCROLL_STATE_IDLE && listViewHasFocus) {
                        binding.listView.smoothScrollToPositionFromTop(arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET, 0, 100)
                        setSuggestionsVisible(false)
                        removeSnapTimer()
                    } else {
                        removeSnapTimer()
                        startSnapTimer()
                    }
                }
            }
        }
    }

    private fun insertSuggestion(tvSuggestion: TextView) {
        if (tvSuggestion.visibility != View.VISIBLE)
            return

        if (arrayAdapter.currentPosition < WORD_COUNT + WordsPickedBaseListAdapter.OFFSET) {
            _viewModel.wordsPicked[arrayAdapter.currentPosition] = tvSuggestion.text.toString()
            setSuggestionsVisible(false)
            moveDown()
            _viewModel.validateInputCode()
        }
    }

    private fun moveDown() {
        if (arrayAdapter.currentPosition <= WORD_COUNT)
            arrayAdapter.currentPosition++
        moveToCurrent()
    }

    private fun moveToCurrent() {
        listViewHasFocus = false
        binding.listView.smoothScrollToPositionFromTop(arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET, 0, 50)
        Timer().schedule(100) {
            activity?.runOnUiThread {
                setSuggestionsVisible(false)
                arrayAdapter.notifyDataSetChanged()
            }
        }
    }
}
