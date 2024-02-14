package com.concordium.wallet.ui.bakerdelegation.common

import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class StakeAmountInputValidatorTest {

    @Test
    fun noErrorIfNotChangingStakeUnderMinimum() {
        // If the old stake is under the current minimum, but not being changed,
        // no error should be returned.
        val validator = StakeAmountInputValidator(
            minimumValue = "15000000000".toBigInteger(),
            maximumValue = "20000000000".toBigInteger(),
            oldStakedAmount = "10000000000".toBigInteger(),
            balance = "10000000000".toBigInteger(),
            atDisposal = BigInteger.ZERO,
            currentPool = null,
            poolLimit = null,
            previouslyStakedInPool = null,
            isInCoolDown = null,
            oldPoolId = null,
            newPoolId = null,
        )
        val validationResult = validator.validate("10000000000".toBigInteger(), null)

        Assert.assertEquals(StakeAmountInputValidator.StakeError.OK, validationResult)
    }

    @Test
    fun noErrorIfAmountIsGreaterThanLong() {
        val validator = StakeAmountInputValidator(
            minimumValue = "0".toBigInteger(),
            maximumValue = "53151357135135000001".toBigInteger(),
            oldStakedAmount = null,
            balance = "53151357135135000001".toBigInteger(),
            atDisposal = "53151357135135000001".toBigInteger(),
            currentPool = null,
            poolLimit = null,
            previouslyStakedInPool = null,
            isInCoolDown = null,
            oldPoolId = null,
            newPoolId = null,
        )
        val bigAmount = "53151357135135000000".toBigInteger()

        val validationResult = validator.validate(bigAmount, null)

        Assert.assertEquals(StakeAmountInputValidator.StakeError.OK, validationResult)
    }
}
