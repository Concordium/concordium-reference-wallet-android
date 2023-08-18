package com.concordium.wallet.ui.bakerdelegation.common

import org.junit.Assert
import org.junit.Test

class StakeAmountInputValidatorTest {

    @Test
    fun notEnoughFundIfAmountIsGreaterThanLong() {
        val validator = StakeAmountInputValidator(
            null, "12345678912345", null, null, null, null, null, null, null, null
        )
        val tooBigAmount = "53151357135135000000"

        val validationResult = validator.validate(tooBigAmount, null)

        Assert.assertEquals(StakeAmountInputValidator.StakeError.NOT_ENOUGH_FUND, validationResult)
    }
}
