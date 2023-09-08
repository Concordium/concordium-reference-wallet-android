package com.concordium.wallet.onboarding.ui.di

import com.concordium.wallet.data.repository.AuthenticationRepository
import com.concordium.wallet.ui.passphrase.recover.ExportPassPhraseViewModel
import com.concordium.wallet.ui.passphrase.recover.PassPhraseRecoverViewModel
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object PassPhraseModule : ModuleLoader() {
    override val modules: List<Module> =
        listOf(
            viewModelModule,
            repositoryModule
        )
}

private val viewModelModule = module {

    viewModel {
        PassPhraseViewModel(
            application = get(),
            authenticationRepository = get()
        )
    }

    viewModel {
        PassPhraseRecoverViewModel(
            application = get(),
            authenticationRepository = get()
        )
    }

    viewModel {
        ExportPassPhraseViewModel(
            application = get(),
            authenticationRepository = get()
        )
    }
}

private val repositoryModule = module {
    single { AuthenticationRepository(sharedPreferences = get()) }
}