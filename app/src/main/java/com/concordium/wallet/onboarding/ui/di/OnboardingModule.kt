package com.concordium.wallet.onboarding.ui.di

import com.concordium.wallet.onboarding.data.OnboardingRepository
import com.concordium.wallet.onboarding.ui.terms.TermsAndConditionsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object OnboardingModule : ModuleLoader() {
    override val modules: List<Module> =
        listOf(
            viewModelModule,
            repositoryModule
        )
}

private val viewModelModule = module {
    viewModel {
        TermsAndConditionsViewModel(
            application = get(),
            onboardingRepository = get()
        )
    }
}

private val repositoryModule = module {
    single { OnboardingRepository(sharedPreferences = get(), onboardingService = get()) }
}
