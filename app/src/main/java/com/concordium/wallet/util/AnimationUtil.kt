package com.concordium.wallet.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

object AnimationUtil {

    fun crossFade(startView: View, targetView: View) {
        val shortAnimationDuration = 500L
        targetView.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)
        }
        startView.animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startView.visibility = View.GONE
                }
            })
    }
}