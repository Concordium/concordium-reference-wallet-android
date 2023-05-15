package com.concordium.wallet.core.tracking

import com.concordium.wallet.AppConfig.appVersion
import com.concordium.wallet.AppConfig.net
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

class AppTracker(private val tracker: Tracker) {

    companion object {
        const val APP_VERSION_VARIABLE_INDEX = 1
        const val NETWORK_VERSION_VARIABLE_INDEX = 2
    }

    fun trackVersionAndNetwork(){
        TrackHelper.track().screen("/mainactivity").title("Home").dimension(APP_VERSION_VARIABLE_INDEX, appVersion).with(tracker)
        TrackHelper.track().screen("/mainactivity").title("Home").dimension(NETWORK_VERSION_VARIABLE_INDEX, net).with(tracker)
    }
}