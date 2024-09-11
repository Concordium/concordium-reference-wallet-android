package com.concordium.wallet.ui.passphrase.recover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportSeedViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val showSeedMutableStateFlow: MutableStateFlow<SeedState> = MutableStateFlow(SeedState.Hidden)
    val seedState: Flow<SeedState> = showSeedMutableStateFlow.asStateFlow()

    // The initial value is a random seed to be shown under a blur.
    private val _seedFlow: MutableStateFlow<String> = MutableStateFlow(
        "f74c4127f238a391891dd23c74cd5283f3c727b282caeba754f815dc876d8b84d3339c6892943575f0f919c99ed4b7d858df2aea68112292da4eae98e2e69410"
    )
    val seed: Flow<String> = _seedFlow.asStateFlow()
    val seedString: String = _seedFlow.value

    fun onShowSeedClicked(password: String) {
        viewModelScope.launch {
            val seed = try {
                AuthPreferences(getApplication()).getSeedPhrase(password)
            } catch (e: Exception) {
                Log.e("seed_decrypt_failed", e)
                return@launch
            }
            _seedFlow.emit(seed)
            showSeedMutableStateFlow.emit(SeedState.Revealed)
        }
    }
}

sealed interface SeedState {
    object Hidden : SeedState
    object Revealed : SeedState
}