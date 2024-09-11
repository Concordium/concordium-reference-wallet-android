package com.concordium.wallet.ui.passphrase.recover

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportPassPhraseRevealBinding
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.AnimationUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExportPassPhraseFragment : BaseBindingFragment<FragmentExportPassPhraseRevealBinding>(),
    AuthDelegate by AuthDelegateImpl() {

    private val viewModel: ExportPassPhraseViewModel by activityViewModel()
    override fun getLayoutResId() = R.layout.fragment_export_pass_phrase_reveal

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupButtons()
        observeButtonsState()

        viewDataBinding.llTapToReveal.setOnClickListener { showSeedPhrase() }
    }

    private fun setupViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ExportSeedPhraseState.Error -> Snackbar.make(
                        viewDataBinding.root,
                        R.string.export_seed_phrase_error,
                        Snackbar.LENGTH_LONG
                    ).show()

                    is ExportSeedPhraseState.Success -> {
                        viewDataBinding.gvReveal.adapter = WordsAdapter(
                            requireContext(),
                            state.seedPhrase
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun setupButtons() {
        viewDataBinding.copySeedPhraseButton.setOnClickListener {
            copyToClipboard(viewModel.seedPhraseString)

            viewDataBinding.copySeedPhraseButton.text = getString(R.string.copied)
            viewDataBinding.copySeedPhraseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_check),
                null,
            )
        }
    }

    private fun copyToClipboard(copyString: String) {
        val clipboardManager: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(
            getString(R.string.pass_phrase_title),
            copyString,
        )
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun showSeedPhrase() {
        showAuthentication(
            activity = requireActivity() as AppCompatActivity,
            authenticated = {
                viewModel.onShowSeedClicked()
                AnimationUtil.crossFade(
                    viewDataBinding.llTapToReveal,
                    viewDataBinding.gvReveal
                )
            }
        )
    }

    private fun observeButtonsState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seedState.collect { state ->
                    when (state) {
                        is State.Revealed -> viewDataBinding.copySeedPhraseButton.visibility = View.VISIBLE
                        else -> viewDataBinding.copySeedPhraseButton.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private class WordsAdapter constructor(
        private val context: Context,
        private val items: List<String>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any? = null

        override fun getItemId(position: Int): Long = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context)
                    .inflate(R.layout.item_passphrase_word, parent, false)
                holder = ViewHolder()
                holder.contentView = view
                holder.tvPosition = view.findViewById(R.id.tvPosition)
                holder.tvTitle = view.findViewById(R.id.tvTitle)
                view.tag = holder
            } else {
                view = convertView
                holder = convertView.tag as ViewHolder
            }

            holder.tvPosition.text = "${position + 1}."
            holder.tvTitle.text = items[position]

            return view
        }

        inner class ViewHolder {
            lateinit var contentView: View
            lateinit var tvPosition: TextView
            lateinit var tvTitle: TextView
        }
    }
}
