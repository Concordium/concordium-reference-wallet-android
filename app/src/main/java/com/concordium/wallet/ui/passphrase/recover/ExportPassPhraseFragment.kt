package com.concordium.wallet.ui.passphrase.recover

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportPassPhraseRevealBinding
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
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
        viewDataBinding.ivSeedBlur.setOnClickListener { showSeed() }
    }

    private fun setupViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ExportSeedPhraseState.Error -> {
                        viewDataBinding.llSeedPhrase.visibility = View.GONE
                        viewDataBinding.clSeed.visibility = View.VISIBLE

                    }

                    is ExportSeedPhraseState.Success -> {
                        viewDataBinding.clSeed.visibility = View.GONE
                        viewDataBinding.llSeedPhrase.visibility = View.VISIBLE
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
        viewDataBinding.showSeedButton.setOnClickListener { showSeed() }

        viewDataBinding.copySeedButton.setOnClickListener {
            viewDataBinding.copySeedButton.text = getString(R.string.copied)
            viewDataBinding.copySeedButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_check),
                null,
            )
        }

        viewDataBinding.copySeedPhraseButton.setOnClickListener {
            viewDataBinding.copySeedPhraseButton.text = getString(R.string.copied)
            viewDataBinding.copySeedPhraseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_check),
                null,
            )
        }
    }

    private fun showSeed() {
        showAuthentication(requireActivity() as AppCompatActivity, { password ->
            crossFade(
                viewDataBinding.ivSeedBlur,
                viewDataBinding.clSeedLayout
            )
            viewModel.onShowSeedClicked()
        })
    }

    private fun showSeedPhrase() {
        showAuthentication(requireActivity() as AppCompatActivity, { password ->
            crossFade(
                viewDataBinding.llTapToReveal,
                viewDataBinding.gvReveal
            )
            viewModel.onShowSeedClicked()
        })
    }

    private fun crossFade(startView: View, targetView: View) {
        val shortAnimationDuration = 500L
        viewDataBinding.apply {
            targetView.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration)
                    .setListener(null)
            }
            startView.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startView.visibility = View.GONE
                    }
                })
        }
    }

    private fun observeButtonsState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seedState.collect { state ->
                    viewDataBinding.showSeedButton.isVisible = state is State.Hidden
                    viewDataBinding.copySeedButton.isVisible = state is State.Revealed
                    viewDataBinding.copySeedPhraseButton.isVisible = state is State.Revealed
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
