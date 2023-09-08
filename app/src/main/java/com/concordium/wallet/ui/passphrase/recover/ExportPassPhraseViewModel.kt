package com.concordium.wallet.ui.passphrase.recover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.util.SPACE
import com.concordium.wallet.data.repository.AuthenticationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExportPassPhraseViewModel(
    application: Application,
    private val authenticationRepository: AuthenticationRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ExportSeedPhraseState>(ExportSeedPhraseState.Loading)
    val state: StateFlow<ExportSeedPhraseState> = _state

    init {
        viewModelScope.launch {
            authenticationRepository.getSeedPhase().onSuccess { result ->
                _state.update {
                    when {
                        result.isNullOrBlank() -> ExportSeedPhraseState.Error(IllegalStateException("No seed phrase found"))
                        else -> ExportSeedPhraseState.Success(result.split(String.SPACE))
                    }
                }
            }
        }
    }
}

sealed class ExportSeedPhraseState {
    object Loading : ExportSeedPhraseState()
    data class Error(val originalException: Throwable) : ExportSeedPhraseState()
    data class Success(val seedPhrase: List<String>) : ExportSeedPhraseState()
}
