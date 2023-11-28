package com.concordium.wallet.ui.bakerdelegation.baker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakingCommissionRange
import com.concordium.wallet.data.model.TransactionCommissionRange
import com.concordium.wallet.databinding.ActivityBakerSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.util.addSuffix
import com.concordium.wallet.util.dropAfterDecimalPlaces
import com.concordium.wallet.util.getTextWithoutSuffix
import com.concordium.wallet.util.hideKeyboard
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt


class BakerPoolSettingsActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.baker_registration_title
        )
        viewModel.bakerDelegationData.oldCommissionRates =
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates

        viewModel.loadChainParameters()
        initViews()
        initObservables()
    }

    private fun initObservables() {
        viewModel.chainParametersLoadedLiveData.observe(this) {
            setCommissionRates()
        }
    }

    private fun setCommissionRates() {
        val chainParams = viewModel.bakerDelegationData.chainParameters ?: return
        binding.apply {
            setTransactionRates(chainParams.transactionCommissionRange)

            setBakingRates(chainParams.bakingCommissionRange)
        }
    }

    private val RATE_RANGE_FRACTION_MULTIPLIER = 100000
    private val COMMISION_RATE_SUFFIX = "%"

    @SuppressLint("SetTextI18n")
    private fun ActivityBakerSettingsBinding.setTransactionRates(
        transactionRange: TransactionCommissionRange
    ) {
        var isDynamicChange = false
        if (transactionRange.min != transactionRange.max) {
            transactionFeeGroup.visibility = View.VISIBLE
            transactionFeeMin.text = getMinRangeText(transactionRange.min)
            transactionFeeMax.text = getMaxRangeText(transactionRange.max)

            transactionFeeValue.addSuffix(COMMISION_RATE_SUFFIX)
            transactionFeeValue.doOnTextChanged { text, start, before, count ->
                if (transactionFeeValue.hasFocus()) {
                    val textAmount = text?.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                    when {
                        textAmount.isNullOrEmpty() -> Unit
                        else -> {
                            try {
                                isDynamicChange = true

                                val parsedAmount = textAmount.toDouble() / 100
                                val previousProgress = transactionFeeSlider.progress
                                transactionFeeSlider.progress =
                                    if (parsedAmount in transactionRange.min..transactionRange.max) {
                                        getSliderRangeValue(parsedAmount)
                                    } else if (parsedAmount > transactionRange.max) {
                                        getSliderRangeValue(transactionRange.max)
                                    } else {
                                        getSliderRangeValue(transactionRange.min)
                                    }
                                if (previousProgress == transactionFeeSlider.progress) {
                                    isDynamicChange = false
                                }

                            } catch (e: NumberFormatException) {
                                isDynamicChange = false
                                transactionFeeValue.setText(getPercentageString(transactionRange.max))
                                Snackbar.make(
                                    transactionFeeValue,
                                    getString(R.string.baking_commission_rate_error_parse),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

            transactionFeeSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(transactionRange.max)
                min = getSliderRangeValue(transactionRange.min)
                progress = getSliderDefaultProgressValue(
                    transactionRange.max,
                    viewModel.bakerDelegationData.oldCommissionRates?.transactionCommission
                        ?: viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates?.transactionCommission
                )
                transactionFeeValue.setText(getPercentageStringFromProgress(progress))

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        if (isDynamicChange.not() && getPercentageStringFromProgress(progress) != transactionFeeValue.text.toString()) {
                            transactionFeeValue.clearFocus()
                            hideKeyboard(rootLayout)
                            transactionFeeValue.setText(getPercentageStringFromProgress(progress))
                        }
                        isDynamicChange = false
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            transactionFeeGroup.visibility = View.GONE
            transactionFeeValue.isEnabled = false
            transactionFeeValue.setText(getPercentageString(transactionRange.max))
        }
    }

    private fun ActivityBakerSettingsBinding.setBakingRates(
        bakingRange: BakingCommissionRange
    ) {
        var isDynamicChange = false
        if (bakingRange.min != bakingRange.max) {
            bakingGroup.visibility = View.VISIBLE
            bakingMin.text = getMinRangeText(bakingRange.min)
            bakingMax.text = getMaxRangeText(bakingRange.max)

            bakingValue.addSuffix(COMMISION_RATE_SUFFIX)
            bakingValue.doOnTextChanged { text, start, before, count ->
                if (bakingValue.hasFocus()) {
                    val textAmount = text?.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                    when {
                        textAmount.isNullOrEmpty() -> Unit
                        else -> {
                            try {
                                isDynamicChange = true

                                val parsedAmount = textAmount.toDouble() / 100
                                val previousProgress = bakingSlider.progress
                                bakingSlider.progress =
                                    if (parsedAmount in bakingRange.min..bakingRange.max) {
                                        getSliderRangeValue(parsedAmount)
                                    } else if (parsedAmount > bakingRange.max) {
                                        getSliderRangeValue(bakingRange.max)
                                    } else {
                                        getSliderRangeValue(bakingRange.min)
                                    }
                                if (previousProgress == bakingSlider.progress) {
                                    isDynamicChange = false
                                }

                            } catch (e: NumberFormatException) {
                                isDynamicChange = false
                                bakingValue.setText(getPercentageString(bakingRange.max))
                                Snackbar.make(
                                    bakingValue,
                                    getString(R.string.baking_commission_rate_error_parse),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

            bakingSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(bakingRange.max)
                min = getSliderRangeValue(bakingRange.min)
                progress = getSliderDefaultProgressValue(
                    bakingRange.max,
                    viewModel.bakerDelegationData.oldCommissionRates?.bakingCommission
                        ?: viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates?.bakingCommission
                )
                bakingValue.setText(getPercentageStringFromProgress(progress))


                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        if (isDynamicChange.not() && getPercentageStringFromProgress(progress) != bakingValue.text.toString()) {
                            bakingValue.clearFocus()
                            hideKeyboard(rootLayout)
                            bakingValue.setText(getPercentageStringFromProgress(progress))
                        }
                        isDynamicChange = false
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            bakingGroup.visibility = View.GONE
            bakingValue.isEnabled = false
            bakingValue.setText(getPercentageString(bakingRange.max))
        }
    }

    private fun onContinueClicked() {
        val chainParams = viewModel.bakerDelegationData.chainParameters
        val transactionRange = chainParams?.transactionCommissionRange ?: return showErrorMessage(
            getString(R.string.app_error_lib)
        )
        val bakingRange = chainParams.bakingCommissionRange

        var selectedTransactionRate =
            binding.transactionFeeValue.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                .replace(",", ".").toDouble()

        selectedTransactionRate = when (selectedTransactionRate) {
            transactionRange.max * 100 -> transactionRange.max
            transactionRange.min * 100 -> transactionRange.min
            else -> {
                ("." + binding.transactionFeeValue.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                    .replace(",", "")
                    .replace(".", "")).toDouble()
            }
        }

        var selectedBakingRate =
            binding.bakingValue.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                .replace(",", ".").toDouble()

        selectedBakingRate = when (selectedBakingRate) {
            bakingRange.max * 100 -> bakingRange.max
            bakingRange.min * 100 -> bakingRange.min
            else -> {
                ("." + binding.bakingValue.getTextWithoutSuffix(COMMISION_RATE_SUFFIX)
                    .replace(",", "")
                    .replace(".", "")).toDouble()
            }
        }


        if (selectedTransactionRate !in transactionRange.min..transactionRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_transaction_not_in_range))
            return
        }
        if (selectedBakingRate !in bakingRange.min..bakingRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_baking_not_in_range))
            return
        }

        viewModel.setSelectedCommissionRates(
            transactionRate = selectedTransactionRate.dropAfterDecimalPlaces(5),
            bakingRate = selectedBakingRate.dropAfterDecimalPlaces(5),
        )

        val intent = Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun getSliderRangeValue(value: Double): Int =
        (value * RATE_RANGE_FRACTION_MULTIPLIER).toInt()

    private fun getSliderDefaultProgressValue(max: Double, current: Double?) =
        if (current != null) (current * RATE_RANGE_FRACTION_MULTIPLIER).roundToInt() else (max * RATE_RANGE_FRACTION_MULTIPLIER).roundToInt()

    private fun getPercentageStringFromProgress(progress: Int) =
        "${valueAsPercentageString(progress.toDouble() / 1000)} %"

    private fun getPercentageString(value: Double) = "${valueAsPercentageString(value * 100)} %"


    private fun valueAsPercentageString(value: Double) = String.format("%.3f", value)

    private fun getMinRangeText(value: Double) = "${
        resources.getString(
            R.string.baking_pool_range_min, String.format("%.3f", value * 100)
        )
    }%"


    private fun getMaxRangeText(value: Double) = "${
        resources.getString(
            R.string.baking_pool_range_max, String.format("%.3f", value * 100)
        )
    }%"

    override fun initViews() {
        binding.bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun errorLiveData(value: Int) {
    }
}
