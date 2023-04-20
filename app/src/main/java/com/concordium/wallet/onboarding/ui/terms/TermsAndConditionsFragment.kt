package com.concordium.wallet.onboarding.ui.terms

import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentTermsAndConditionsBinding
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.regex.Pattern

internal const val TERMS_AND_CONDITIONS_LINK =
    "http://wallet-proxy.mainnet.concordium.software/v0/termsAndConditionsVersion"

class TermsAndConditionsFragment :
    BaseBindingFragment<FragmentTermsAndConditionsBinding>() { //todo extract to common base TermsAndConditionsFragment and TermsAndConditionsUpdateFragment

    private val viewModel by activityViewModel<TermsAndConditionsViewModel>()
    override fun getLayoutResId() = R.layout.fragment_terms_and_conditions

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()

        viewDataBinding.apply {
            approveTermsSwitch.setOnCheckedChangeListener { _, isChecked ->
                confirmButton.isEnabled = isChecked
            }
            confirmButton.setOnClickListener {
                viewModel.onTermsAccepted()
                //todo navigate forward
            }
            Linkify.addLinks(
                approveTermsText,
                Pattern.compile(getString(R.string.terms_and_conditions_approve_pattern)),
                TERMS_AND_CONDITIONS_LINK
            )
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is TermsAndConditionsState.UpdateTerms, is TermsAndConditionsState.Error ->
                            showErrorMessage()

                        TermsAndConditionsState.Loading ->
                            viewDataBinding.loadingContainer.visibility = View.VISIBLE

                        is TermsAndConditionsState.InitialTerms -> {
                            viewDataBinding.apply {
                                loadingContainer.visibility = View.GONE
                                viewDataBinding.approveTermsSwitch.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showErrorMessage() {
        Snackbar.make(
            viewDataBinding.rootContainer,
            getString(R.string.unexpected_error),
            Snackbar.LENGTH_LONG
        ).show()
    }
}