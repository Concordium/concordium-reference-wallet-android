package com.concordium.wallet.util

import java.util.Date
import java.util.Random

object RandomUtil {

    fun randomString(length: Int): String {
        val charString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = Random(Date().time)

        val sb = StringBuilder(length)
        for (i in 0 until length) {
            val randomNumber = random.nextInt(charString.length)
            sb.append(charString[randomNumber])
        }
        return sb.toString()
    }
}