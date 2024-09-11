package com.concordium.wallet.ui.passphrase.recover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.util.SPACE
import com.concordium.wallet.data.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExportPassPhraseViewModel(
    application: Application,
    private val authenticationRepository: AuthenticationRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ExportSeedPhraseState>(ExportSeedPhraseState.Loading)
    val state: StateFlow<ExportSeedPhraseState> = _state

    private val _seedPhrase: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val seedPhraseString: String
        get() = _seedPhrase.value.joinToString(String.SPACE)

    private val showSeedMutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State.Hidden)
    val seedState: Flow<State> = showSeedMutableStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            authenticationRepository.getSeedPhase()
                .onSuccess { phrase ->
                    _state.update {
                        ExportSeedPhraseState.Success(phrase.split(String.SPACE))
                    }
                    _seedPhrase.update { phrase.split(String.SPACE) }
                }
                .onFailure {
                    _state.update {
                        ExportSeedPhraseState.Error(
                            IllegalStateException("No seed phrase found")
                        )
                    }
                }
        }
    }

    fun onShowSeedClicked() {
        viewModelScope.launch {
            showSeedMutableStateFlow.emit(State.Revealed)
        }
    }
}

sealed class ExportSeedPhraseState {
    object Loading : ExportSeedPhraseState()
    data class Error(val originalException: Throwable) : ExportSeedPhraseState()
    data class Success(val seedPhrase: List<String>) : ExportSeedPhraseState()
}

sealed interface State {
    object Hidden : State
    object Revealed : State
}
