package com.concordium.wallet.onboarding.ui.terms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.onboarding.data.OnboardingRepository
import com.concordium.wallet.onboarding.ui.terms.model.TermsAndConditionsItem
import com.walletconnect.util.Empty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TermsAndConditionsViewModel(
    application: Application,
    val onboardingRepository: OnboardingRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<TermsAndConditionsState>(TermsAndConditionsState.Loading)
    val state: StateFlow<TermsAndConditionsState> = _state

    init {
        viewModelScope.launch {
            val lastAcceptedVersion =
                onboardingRepository.getLocalAcceptedTermsAndConditionsVersion()
            onboardingRepository.getRemoteAcceptedTermsAndConditionsVersion().onSuccess { result ->
                _state.update {
                    when {
                        lastAcceptedVersion.isNullOrEmpty() ->
                            TermsAndConditionsState.InitialTerms(result)

                        result.version != lastAcceptedVersion ->
                            TermsAndConditionsState.UpdateTerms(result)

                        else -> TermsAndConditionsState.NavigateUpdateForward
                    }
                }
            }.onFailure { error ->
                _state.update { TermsAndConditionsState.Error(error) }
            }
        }
    }

    fun onTermsAccepted() {
        viewModelScope.launch {
            with(_state.value) {
                val termsAccepted = when (this) {
                    is TermsAndConditionsState.InitialTerms -> {
                        _state.update { TermsAndConditionsState.NavigateOnboardingForward }
                        terms.version
                    }

                    is TermsAndConditionsState.UpdateTerms -> {
                        _state.update { TermsAndConditionsState.NavigateUpdateForward }
                        newestTerms.version
                    }

                    else -> String.Empty
                }

                onboardingRepository.saveAcceptedTermsAndConditionsVersion(termsAccepted)
            }
        }
    }
}

sealed class TermsAndConditionsState {
    object Loading : TermsAndConditionsState()
    data class InitialTerms(val terms: TermsAndConditionsItem) : TermsAndConditionsState()
    data class UpdateTerms(val newestTerms: TermsAndConditionsItem) : TermsAndConditionsState()

    data class Error(val originalException: Throwable) : TermsAndConditionsState()

    object NavigateOnboardingForward : TermsAndConditionsState()
    object NavigateUpdateForward : TermsAndConditionsState()
}
