package com.concordium.wallet.ui.bakerdelegation.common

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import java.math.BigInteger

class StakeAmountInputValidator(
    private val minimumValue: BigInteger?,
    private val maximumValue: BigInteger?,
    private val oldStakedAmount: BigInteger?,
    private val balance: BigInteger?,
    private val atDisposal: BigInteger?,
    private val currentPool: BigInteger?,
    private val poolLimit: BigInteger?,
    private val previouslyStakedInPool: BigInteger?,
    private val isInCoolDown: Boolean?,
    private val oldPoolId: Long?,
    private val newPoolId: String?
) {
    enum class StakeError {
        OK, NOT_ENOUGH_FUND, MINIMUM, MAXIMUM, POOL_LIMIT_REACHED, POOL_LIMIT_REACHED_COOLDOWN, UNKNOWN
    }

    fun validate(amount: BigInteger?, estimatedFee: BigInteger?): StakeError {
        if (amount == null) return StakeError.MINIMUM

        var check = checkMaximum(amount)
        if (check != StakeError.OK) return check

        check = checkMinimum(amount)
        if (check != StakeError.OK) return check

        if (isInCoolDown == true) {
            check = checkPoolLimitCoolDown(amount)
            if (check != StakeError.OK) return check
        } else {
            check = checkPoolLimit(amount)
            if (check != StakeError.OK) return check
        }

        check = checkBalance(amount, estimatedFee)
        if (check != StakeError.OK) return check

        return StakeError.OK
    }

    fun getErrorText(context: Context, stakeError: StakeError): String {
        return when (stakeError) {
            StakeError.NOT_ENOUGH_FUND -> context.getString(R.string.delegation_register_delegation_not_enough_funds)
            StakeError.MINIMUM -> context.getString(
                R.string.delegation_register_delegation_minimum,
                CurrencyUtil.formatGTU(minimumValue ?: BigInteger.ZERO, false)
            )

            StakeError.MAXIMUM -> context.getString(
                R.string.delegation_register_delegation_maximum,
                CurrencyUtil.formatGTU(maximumValue ?: BigInteger.ZERO, false)
            )

            StakeError.POOL_LIMIT_REACHED -> context.getString(R.string.delegation_register_delegation_pool_limit_will_be_breached)
            StakeError.POOL_LIMIT_REACHED_COOLDOWN -> context.getString(R.string.delegation_amount_too_large_while_in_cooldown)
            StakeError.UNKNOWN -> context.getString(R.string.app_error_general)
            else -> ""
        }
    }

    private fun checkMaximum(amount: BigInteger): StakeError {
        // Only check for maximum if there is one and the amount is to be changed.
        if (maximumValue == null || oldStakedAmount != null && oldStakedAmount == amount)
            return StakeError.OK
        if (amount > maximumValue) return StakeError.MAXIMUM
        return StakeError.OK
    }

    private fun checkMinimum(amount: BigInteger): StakeError {
        // Only check for minimum if the amount is to be changed.
        if (oldStakedAmount != null && oldStakedAmount == amount) return StakeError.OK
        if (minimumValue == null) return StakeError.UNKNOWN
        if (amount < minimumValue) return StakeError.MINIMUM
        return StakeError.OK
    }

    private fun checkBalance(amount: BigInteger, estimatedFee: BigInteger?): StakeError = when {
        balance == null || atDisposal == null -> StakeError.UNKNOWN

        balance < amount + (estimatedFee ?: BigInteger.ZERO) ->
            StakeError.NOT_ENOUGH_FUND

        (estimatedFee ?: BigInteger.ZERO) > atDisposal -> StakeError.NOT_ENOUGH_FUND

        else -> StakeError.OK
    }

    private fun checkPoolLimit(amount: BigInteger): StakeError {
        if (currentPool != null && poolLimit != null) {
            val prev =
                // Only use previouslyStakedInPool if pool is the same.
                if (oldPoolId == newPoolId?.toLongOrNull())
                    previouslyStakedInPool ?: BigInteger.ZERO
                else
                    BigInteger.ZERO

            if ((amount + currentPool - prev) > poolLimit) {
                return StakeError.POOL_LIMIT_REACHED
            }
        }
        return StakeError.OK
    }

    private fun checkPoolLimitCoolDown(amount: BigInteger): StakeError {
        // check if pool has changed from either Passive to a baker pool or from one baker pool to another
        if (isInCoolDown == true) {
            if ((oldPoolId == null && !newPoolId.isNullOrEmpty())
                || (oldPoolId != null && newPoolId != null && oldPoolId.toString() != newPoolId)
            ) {
                if (currentPool != null && poolLimit != null) {
                    val prev =
                        // Only use previouslyStakedInPool if pool is the same.
                        if (oldPoolId == newPoolId.toLongOrNull())
                            previouslyStakedInPool ?: BigInteger.ZERO
                        else
                            BigInteger.ZERO

                    if ((amount + currentPool - prev) > poolLimit) {
                        return StakeError.POOL_LIMIT_REACHED_COOLDOWN
                    }
                }
            }
        }
        return StakeError.OK
    }
}
