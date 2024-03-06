package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakingCommissionRange
import com.concordium.wallet.data.model.TransactionCommissionRange
import com.concordium.wallet.databinding.ActivityBakerSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.hideKeyboard
import java.text.DecimalFormat
import java.text.ParseException
import kotlin.math.roundToInt


class BakerPoolSettingsActivity : BaseDelegationBakerActivity() {
    private lateinit var binding: ActivityBakerSettingsBinding

    /**
     * Formats values in range [[0.0; 1.0]].
     */
    private val percentNumberFormat = (DecimalFormat.getNumberInstance() as DecimalFormat).apply {
        multiplier = 100
        minimumFractionDigits = PERCENT_FORMAT_DECIMALS
        maximumFractionDigits = PERCENT_FORMAT_DECIMALS
    }

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

    private fun ActivityBakerSettingsBinding.setTransactionRates(
        transactionRange: TransactionCommissionRange
    ) {
        var isDynamicChange = false
        transactionFeeValue.decimals = PERCENT_FORMAT_DECIMALS
        if (transactionRange.min != transactionRange.max) {
            transactionFeeGroup.visibility = View.VISIBLE
            transactionFeeMin.text = getMinRangeText(transactionRange.min)
            transactionFeeMax.text = getMaxRangeText(transactionRange.max)

            transactionFeeValue.afterTextChanged { textAmount ->
                if (!transactionFeeValue.hasFocus()) {
                    return@afterTextChanged
                }

                try {
                    isDynamicChange = true

                    val parsedAmount: Double = textAmount
                        .takeIf(String::isNotBlank)
                        ?.let(percentNumberFormat::parse)
                        ?.toDouble()
                        ?: 0.0
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
                } catch (e: ParseException) {
                    isDynamicChange = false
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
                transactionFeeValue.setText(getPercentageFromProgress(progress))

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        if (isDynamicChange.not() && getPercentageFromProgress(progress) != transactionFeeValue.text.toString()) {
                            transactionFeeValue.clearFocus()
                            hideKeyboard(rootLayout)
                            transactionFeeValue.setText(getPercentageFromProgress(progress))
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
            transactionFeeValue.setText(percentNumberFormat.format(transactionRange.max))
        }
    }

    private fun ActivityBakerSettingsBinding.setBakingRates(
        bakingRange: BakingCommissionRange
    ) {
        var isDynamicChange = false
        bakingValue.decimals = PERCENT_FORMAT_DECIMALS
        if (bakingRange.min != bakingRange.max) {
            bakingGroup.visibility = View.VISIBLE
            bakingMin.text = getMinRangeText(bakingRange.min)
            bakingMax.text = getMaxRangeText(bakingRange.max)

            bakingValue.afterTextChanged { textAmount ->
                if (!bakingValue.hasFocus()) {
                    return@afterTextChanged
                }

                try {
                    isDynamicChange = true

                    val parsedAmount: Double = textAmount
                        .takeIf(String::isNotBlank)
                        ?.let(percentNumberFormat::parse)
                        ?.toDouble()
                        ?: 0.0
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
                } catch (e: ParseException) {
                    isDynamicChange = false
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
                bakingValue.setText(getPercentageFromProgress(progress))

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        if (isDynamicChange.not() && getPercentageFromProgress(progress) != bakingValue.text.toString()) {
                            bakingValue.clearFocus()
                            hideKeyboard(rootLayout)
                            bakingValue.setText(getPercentageFromProgress(progress))
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
            bakingValue.setText(percentNumberFormat.format(bakingRange.max))
        }
    }

    private fun onContinueClicked() {
        val chainParams = viewModel.bakerDelegationData.chainParameters
        val transactionRange = chainParams?.transactionCommissionRange ?: return showErrorMessage(
            getString(R.string.app_error_lib)
        )
        val bakingRange = chainParams.bakingCommissionRange

        val selectedTransactionRate: Double =
            binding.transactionFeeValue.text
                .toString()
                .takeIf(String::isNotBlank)
                ?.let(percentNumberFormat::parse)
                ?.toDouble()
                ?: 0.0
        val selectedBakingRate: Double =
            binding.bakingValue.text
                .toString()
                .takeIf(String::isNotBlank)
                ?.let(percentNumberFormat::parse)
                ?.toDouble()
                ?: 0.0

        if (selectedTransactionRate !in transactionRange.min..transactionRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_transaction_not_in_range))
            return
        }
        if (selectedBakingRate !in bakingRange.min..bakingRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_baking_not_in_range))
            return
        }

        viewModel.setSelectedCommissionRates(
            transactionRate = selectedTransactionRate,
            bakingRate = selectedBakingRate,
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

    private fun getPercentageFromProgress(progress: Int) =
        percentNumberFormat.format(progress.toDouble() / RATE_RANGE_FRACTION_MULTIPLIER)

    private fun getMinRangeText(value: Double) =
        getString(R.string.baking_pool_range_min, percentNumberFormat.format(value))

    private fun getMaxRangeText(value: Double) =
        getString(R.string.baking_pool_range_max, percentNumberFormat.format(value))

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

    private companion object {
        private const val RATE_RANGE_FRACTION_MULTIPLIER = 100000
        private const val PERCENT_FORMAT_DECIMALS = 3
    }
}
