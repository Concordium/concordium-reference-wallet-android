package com.concordium.wallet.ui.passphrase.recover

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportSeedBinding
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.AnimationUtil
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExportSeedFragment : BaseBindingFragment<FragmentExportSeedBinding>(),
    AuthDelegate by AuthDelegateImpl() {

    private val viewModel: ExportSeedViewModel by activityViewModel()

    override fun getLayoutResId() = R.layout.fragment_export_seed

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSeed()
        setupButtons()
        observeButtonsState()

        viewDataBinding.clSeedBlur.setOnClickListener { showSeed() }
    }

    private fun initSeed() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seed.collect { seed ->
                    viewDataBinding.tvSeed.text = seed
                }
            }
        }
    }

    private fun setupButtons() {
        viewDataBinding.showSeedButton.setOnClickListener { showSeed() }

        viewDataBinding.copySeedButton.setOnClickListener {
            copyToClipboard(viewModel.seedString)

            viewDataBinding.copySeedButton.text = getString(R.string.copied)
            viewDataBinding.copySeedButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
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

    private fun showSeed() {
        showAuthentication(
            activity = requireActivity() as AppCompatActivity,
            authenticated = { password ->
                password?.let {viewModel.onShowSeedClicked(password) }
                AnimationUtil.crossFade(
                    viewDataBinding.clSeedBlur,
                    viewDataBinding.clSeedLayout
                )
            }
        )
    }

    private fun observeButtonsState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seedState.collect { state ->
                    viewDataBinding.showSeedButton.isVisible = state is SeedState.Hidden
                    viewDataBinding.copySeedButton.isVisible = state is SeedState.Revealed
                }
            }
        }
    }
}