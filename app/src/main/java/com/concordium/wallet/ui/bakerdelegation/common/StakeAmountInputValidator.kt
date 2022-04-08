package com.concordium.wallet.ui.bakerdelegation.common

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil

class StakeAmountInputValidator(
    private val minimumValue: String?,
    private val maximumValue: String?,
    private val atDisposal: String?,
    private val currentPool: String?,
    private val poolLimit: String?,
    private val previouslyStakedInPool: String?
) {
    enum class StakeError {
        OK, NOT_ENOUGH_FUND, MINIMUM, MAXIMUM, POOL_LIMIT_REACHED, UNKNOWN
    }

    fun validate(amount: String?): StakeError {

        if (amount == null) return StakeError.MINIMUM

        var check = checkAmount(amount)
        if (check != StakeError.OK) return check

        check = checkMaximum(amount)
        if (check != StakeError.OK) return check

        check = checkMinimum(amount)
        if (check != StakeError.OK) return check

        check = checkPoolLimit(amount)
        if (check != StakeError.OK) return check

        check = checkAtDisposal(amount)
        if (check != StakeError.OK) return check

        return StakeError.OK
    }

    fun getErrorText(context: Context, stakeError: StakeError): String {
        return when (stakeError) {
            StakeError.NOT_ENOUGH_FUND -> context.getString(R.string.delegation_register_delegation_not_enough_funds)
            StakeError.MINIMUM -> context.getString(R.string.delegation_register_delegation_minimum, CurrencyUtil.formatGTU(minimumValue ?: "0", false))
            StakeError.MAXIMUM -> context.getString(R.string.delegation_register_delegation_maximum, CurrencyUtil.formatGTU(maximumValue ?: "0", false))
            StakeError.POOL_LIMIT_REACHED -> context.getString(R.string.delegation_register_delegation_pool_limit_will_be_breached)
            StakeError.UNKNOWN -> context.getString(R.string.app_error_general)
            else -> ""
        }
    }

    private fun checkAmount(amount: String): StakeError {
        if (amount.toLongOrNull() == null) StakeError.UNKNOWN
        return StakeError.OK
    }

    private fun checkMaximum(amount: String): StakeError {
        if (maximumValue?.toLongOrNull() == null) return StakeError.OK
        if (amount.toLong() > maximumValue.toLong()) return StakeError.MAXIMUM
        return StakeError.OK
    }

    private fun checkMinimum(amount: String): StakeError {
        if (minimumValue?.toLongOrNull() == null) return StakeError.UNKNOWN
        if (amount.toLong() < minimumValue.toLong()) return StakeError.MINIMUM
        return StakeError.OK
    }

    private fun checkAtDisposal(amount: String): StakeError {
        if (atDisposal?.toLongOrNull() == null) return StakeError.UNKNOWN
        if (amount.toLong() > atDisposal.toLong()) return StakeError.NOT_ENOUGH_FUND
        return StakeError.OK
    }

    private fun checkPoolLimit(amount: String): StakeError {
        if (currentPool?.toLongOrNull() != null && poolLimit?.toLongOrNull() != null) {
            if (amount.toLong() + currentPool.toLong() - (previouslyStakedInPool?.toLong() ?: 0) > poolLimit.toLong()) {
                return StakeError.POOL_LIMIT_REACHED
            }
        }
        return StakeError.OK
    }
}
