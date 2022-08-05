package com.concordium.wallet.ui.passphrase.setup

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseInputBinding
import com.concordium.wallet.ui.passphrase.common.WordsPickedBaseListAdapter
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel.Companion.PASS_PHRASE_DATA
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel.Companion.WORD_COUNT
import java.util.*
import kotlin.concurrent.schedule
import kotlin.random.Random

class PassPhraseInputFragment : PassPhraseBaseFragment() {
    private var _binding: FragmentPassPhraseInputBinding? = null
    private val binding get() = _binding!!
    private lateinit var arrayAdapter: WordsPickedListAdapter
    private var allSuggestions = mutableMapOf<Int, Array<CharArray?>>()
    private var isEditing = false
    private var snapTimer: Timer? = null
    private var listViewHasFocus = false
    private var listViewScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE

    companion object {
        @JvmStatic
        fun newInstance(viewModel: PassPhraseViewModel) = PassPhraseInputFragment().apply {
            arguments = Bundle().apply {
                putSerializable(PASS_PHRASE_DATA, viewModel)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSuggestions()
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeSnapTimer()
        _binding = null
    }

    private fun initViews() {
        for (i in 0 until WordsPickedBaseListAdapter.OFFSET) {
            viewModel.wordsPicked[i] = WordsPickedBaseListAdapter.BLANK
        }

        for (i in viewModel.wordsPicked.size - 1 downTo viewModel.wordsPicked.size - WordsPickedBaseListAdapter.OFFSET - 1) {
            viewModel.wordsPicked[i] = WordsPickedBaseListAdapter.BLANK
        }

        arrayAdapter = WordsPickedListAdapter(requireContext(), viewModel.wordsPicked)
        arrayAdapter.setWordPickedClickListener() { position ->
            val before = arrayAdapter.currentPosition
            var durationFactor = 1
            if (position < before) durationFactor = if (before - position == 1) 2 else 3

            arrayAdapter.currentPosition = position
            populateFourSuggestions(position)
            setColorsSuggestions()

            listViewHasFocus = false
            binding.listView.smoothScrollToPositionFromTop(position - 2, 0, 100 * durationFactor)
            Timer().schedule(30 * durationFactor.toLong()) {
                activity?.runOnUiThread {
                    arrayAdapter.notifyDataSetChanged()
                }
            }
            Timer().schedule(150 * durationFactor.toLong()) {
                activity?.runOnUiThread {
                    listViewHasFocus = true
                }
            }
        }
        arrayAdapter.also { binding.listView.adapter = it }

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

        setupTouchListeners()
        populateFourSuggestions(arrayAdapter.currentPosition)
        initListViewScroll()
    }

    private fun initObservers() {
        viewModel.validate.observe(viewLifecycleOwner) { success ->
            if (!success)
                binding.tvError.visibility = View.VISIBLE
            else
                binding.tvError.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListeners() {
        binding.tvSuggest1.setOnTouchListener { _, _ ->
            listViewHasFocus = false
            false
        }
        binding.tvSuggest2.setOnTouchListener { _, _ ->
            listViewHasFocus = false
            false
        }
        binding.tvSuggest3.setOnTouchListener { _, _ ->
            listViewHasFocus = false
            false
        }
        binding.tvSuggest4.setOnTouchListener { _, _ ->
            listViewHasFocus = false
            false
        }
        binding.listView.setOnTouchListener { _, _ ->
            listViewHasFocus = true
            false
        }
    }

    private fun initListViewScroll() {
        binding.listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                listViewScrollState = scrollState
            }
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                val changeTo = firstVisibleItem + 2
                if (changeTo != arrayAdapter.currentPosition && listViewHasFocus) {
                    if (arrayAdapter.currentPosition != changeTo) {
                        arrayAdapter.currentPosition = changeTo
                        populateFourSuggestions(arrayAdapter.currentPosition)
                        setColorsSuggestions()
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
                    if (listViewScrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        binding.listView.smoothScrollToPositionFromTop(arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET, 0, 100)
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
        if (arrayAdapter.currentPosition < WORD_COUNT + WordsPickedBaseListAdapter.OFFSET) {

            isEditing = viewModel.wordsPicked[arrayAdapter.currentPosition] != null
            viewModel.wordsPicked[arrayAdapter.currentPosition] = tvSuggestion.text.toString()

            if (isEditing) {
                populateFourSuggestions(arrayAdapter.currentPosition)
                setColorsSuggestions()
                arrayAdapter.notifyDataSetChanged()
                viewModel.validateInputCode()
                return
            }

            setEnableSuggestions(false)
            moveDown()
            populateFourSuggestions(arrayAdapter.currentPosition)
            setColorsSuggestions()
            setEnableSuggestions(true)
            arrayAdapter.notifyDataSetChanged()
            viewModel.validateInputCode()
        }
    }

    private fun moveDown() {
        if (!isEditing) {
            if (arrayAdapter.currentPosition <= WORD_COUNT)
                arrayAdapter.currentPosition++
            binding.listView.smoothScrollToPositionFromTop(arrayAdapter.currentPosition - WordsPickedBaseListAdapter.OFFSET, 0)
        }
    }

    private fun setEnableSuggestions(enabled: Boolean) {
        binding.tvSuggest1.isEnabled = enabled
        binding.tvSuggest2.isEnabled = enabled
        binding.tvSuggest3.isEnabled = enabled
        binding.tvSuggest4.isEnabled = enabled
    }

    private fun setColorsSuggestions() {
        setColorsSuggestion(binding.tvSuggest1)
        setColorsSuggestion(binding.tvSuggest2)
        setColorsSuggestion(binding.tvSuggest3)
        setColorsSuggestion(binding.tvSuggest4)
    }

    private fun setColorsSuggestion(tvSuggestion: TextView) {
        if (tvSuggestion.text != null && tvSuggestion.text.toString() == viewModel.wordsPicked[arrayAdapter.currentPosition]) {
            tvSuggestion.isSelected = true
            tvSuggestion.setTextColor(ContextCompat.getColor(requireContext(), R.color.theme_white))
        } else {
            tvSuggestion.isSelected = false
            tvSuggestion.setTextColor(ContextCompat.getColor(requireContext(), R.color.theme_black))
        }
    }

    private fun populateFourSuggestions(position: Int) {
        allSuggestions[position - WordsPickedBaseListAdapter.OFFSET]?.let { fourSuggestions ->
            binding.tvSuggest1.text = String(fourSuggestions[0]!!)
            binding.tvSuggest2.text = String(fourSuggestions[1]!!)
            binding.tvSuggest3.text = String(fourSuggestions[2]!!)
            binding.tvSuggest4.text = String(fourSuggestions[3]!!)
        }
    }

    private fun loadSuggestions() {
        for (i in 0 until WORD_COUNT) {
            val place = Random(System.nanoTime()).nextInt(0, 4)
            val suggestion = arrayOfNulls<CharArray>(4)
            suggestion[place] = viewModel.mnemonicCodeToConfirm[i]
            var threeRandomsIndex = 0
            val threeRandoms = getThreeRandom(i)
            for (j in 0..3) {
                if (j != place)
                    suggestion[j] = viewModel.mnemonicCodeToConfirm[threeRandoms[threeRandomsIndex++]]
            }
            allSuggestions[i] = suggestion
        }
    }

    private fun getThreeRandom(butNot: Int): IntArray {
        val size = 3
        val s = HashSet<Int>(size)
        while (s.size < size) {
            val random = Random(System.nanoTime()).nextInt(0,24)
            if (random != butNot)
                s += random
        }
        return s.toIntArray()
    }
}
