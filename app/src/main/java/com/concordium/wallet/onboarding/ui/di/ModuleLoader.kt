package com.concordium.wallet.onboarding.ui.di

import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

abstract class ModuleLoader {
    abstract val modules: List<Module>

    fun load() {
        loadKoinModules(modules)
    }

    fun unload() {
        unloadKoinModules(modules)
    }
}
