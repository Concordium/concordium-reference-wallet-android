package com.concordium.wallet.util

object PerformanceUtil {

    private var time = System.currentTimeMillis()
    private var prevTime = time
    private var timeDelta: Long = 0

    val deltaTime: Long
        get() {
            prevTime = time
            time = System.currentTimeMillis()
            timeDelta = time - prevTime
            return timeDelta
        }

    fun showDeltaTime(prefix: String = "") {
        Log.d("$prefix - Millis since last call: $deltaTime")
    }

}
