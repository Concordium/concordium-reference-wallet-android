package com.concordium.wallet.onboarding.ui.terms

import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentTermsAndConditionsBinding
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.concordium.wallet.ui.intro.introstart.IntroStartActivity
import com.google.android.material.snackbar.Snackbar
import com.walletconnect.util.Empty
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.regex.Pattern

internal const val TERMS_AND_CONDITIONS_WEB_LINK =
    "https://developer.concordium.software/en/mainnet/net/resources/terms-and-conditions.html"

class TermsAndConditionsFragment :
    BaseBindingFragment<FragmentTermsAndConditionsBinding>() {
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
            }

            Linkify.addLinks(
                approveTermsText,
                Pattern.compile(getString(R.string.terms_and_conditions_approve_pattern)),
                String.Empty,
                null
            ) { _, _ -> TERMS_AND_CONDITIONS_WEB_LINK }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is TermsAndConditionsState.Error ->
                            showErrorMessage()

                        TermsAndConditionsState.Loading ->
                            viewDataBinding.loadingContainer.visibility = View.VISIBLE

                        is TermsAndConditionsState.InitialTerms -> updateInitialTermsBinding()
                        is TermsAndConditionsState.UpdateTerms -> updateUpdateTermsBinding()
                        TermsAndConditionsState.NavigateUpdateForward,
                        TermsAndConditionsState.NavigateOnboardingForward -> gotoStart()
                    }
                }
            }
        }
    }

    private fun updateInitialTermsBinding() {
        viewDataBinding.apply {
            loadingContainer.visibility = View.GONE
            viewDataBinding.approveTermsSwitch.isEnabled = true

            padlockShieldImg.setImageResource(R.drawable.ic_padlock_shield)
            titleText.text = getString(R.string.terms_and_conditions_title)
            descriptionText.text = getString(R.string.terms_and_conditions_description)
        }
    }

    private fun updateUpdateTermsBinding() {
        viewDataBinding.apply {
            loadingContainer.visibility = View.GONE
            viewDataBinding.approveTermsSwitch.isEnabled = true

            padlockShieldImg.setImageResource(R.drawable.ic_terms_and_conditions)
            titleText.text = getString(R.string.terms_and_conditions_title_update)
            descriptionText.text = getString(R.string.terms_and_conditions_description_update)
        }
    }

    private fun showErrorMessage() {
        Snackbar.make(
            viewDataBinding.rootContainer,
            getString(R.string.unexpected_error),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun gotoStart() {
        App.appCore.session.setTermsHashed(App.appContext.getString(R.string.terms_text).hashCode())
        requireActivity().finish()
        val intent = if (App.appCore.session.hasSetupPassword) Intent(
            requireContext(),
            AuthLoginActivity::class.java
        ) else Intent(requireContext(), IntroStartActivity::class.java)
        startActivity(intent)
    }
}
