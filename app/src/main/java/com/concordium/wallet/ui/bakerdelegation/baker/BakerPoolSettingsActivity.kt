package com.concordium.wallet.ui.bakerdelegation.baker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakingCommissionRange
import com.concordium.wallet.data.model.FinalizationCommissionRange
import com.concordium.wallet.data.model.TransactionCommissionRange
import com.concordium.wallet.databinding.ActivityBakerSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.util.addSuffix
import com.concordium.wallet.util.getTextWithoutSuffix
import com.concordium.wallet.util.hideKeyboard
import com.concordium.wallet.util.truncateDecimals
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

            setRewardRates(chainParams.finalizationCommissionRange)
        }
    }

    private val RATE_RANGE_FRACTION_MULTIPLIER = 100000

    @SuppressLint("SetTextI18n")
    private fun ActivityBakerSettingsBinding.setTransactionRates(
        transactionRange: TransactionCommissionRange
    ) {
        if (transactionRange.min != transactionRange.max) {
            transactionFeeGroup.visibility = View.VISIBLE
            transactionFeeMin.text = getMinRangeText(transactionRange.min)
            transactionFeeMax.text = getMaxRangeText(transactionRange.max)
            transactionFeeValue.addSuffix("%")

            transactionFeeSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(transactionRange.max)
                min = getSliderRangeValue(transactionRange.min)
                progress = getSliderDefaultProgressValue(transactionRange.max, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates?.transactionCommission)
                transactionFeeValue.setText(getPercentageStringFromProgress(progress))

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        transactionFeeValue.setText(getPercentageStringFromProgress(progress))
                        if (transactionFeeValue.hasFocus()) {
                            transactionFeeValue.clearFocus()
                            hideKeyboard(rootLayout)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            transactionFeeGroup.visibility = View.GONE
            transactionFeeValue.setText(getPercentageString(transactionRange.max))
        }
    }

    private fun ActivityBakerSettingsBinding.setBakingRates(bakingRange: BakingCommissionRange) {
        if (bakingRange.min != bakingRange.max) {
            bakingGroup.visibility = View.VISIBLE
            bakingMin.text = getMinRangeText(bakingRange.min)
            bakingMax.text = getMaxRangeText(bakingRange.max)
            bakingValue.addSuffix("%")

            bakingSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(bakingRange.max)
                min = getSliderRangeValue(bakingRange.min)
                progress = getSliderDefaultProgressValue(bakingRange.max, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates?.bakingCommission)
                bakingValue.setText(getPercentageStringFromProgress(progress))


                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        bakingValue.setText(getPercentageStringFromProgress(progress))
                        if (bakingValue.hasFocus()) {
                            bakingValue.clearFocus()
                            hideKeyboard(rootLayout)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            bakingGroup.visibility = View.GONE
            bakingValue.setText(getPercentageString(bakingRange.max))
        }
    }

    private fun ActivityBakerSettingsBinding.setRewardRates(rewardRange: FinalizationCommissionRange) {
        if (rewardRange.min != rewardRange.max) {
            rewardGroup.visibility = View.VISIBLE
            rewardMin.text = getMinRangeText(rewardRange.min)
            rewardMax.text = getMaxRangeText(rewardRange.max)
            rewardValue.addSuffix("%")

            rewardSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(rewardRange.max)
                min = getSliderRangeValue(rewardRange.min)
                progress = getSliderDefaultProgressValue(rewardRange.max, viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.commissionRates?.finalizationCommission)
                rewardValue.setText(getPercentageStringFromProgress(progress))

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        rewardValue.setText(getPercentageStringFromProgress(progress))
                        if (rewardValue.hasFocus()) {
                            rewardValue.clearFocus()
                            hideKeyboard(rootLayout)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            rewardGroup.visibility = View.GONE
            rewardValue.setText(getPercentageString(rewardRange.max))
        }
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

    private fun onContinueClicked() {
        val chainParams = viewModel.bakerDelegationData.chainParameters
        val transactionRange =
            chainParams?.transactionCommissionRange
                ?: return showErrorMessage(getString(R.string.app_error_lib))
        val bakingRange = chainParams.bakingCommissionRange
        val rewardRange = chainParams.finalizationCommissionRange

        val selectedTransactionRate =
            binding.transactionFeeValue.getTextWithoutSuffix("%").toDouble() / 100
        val selectedBakingRate =
            binding.bakingValue.getTextWithoutSuffix("%").toDouble() / 100
        val selectedRewardRate =
            binding.rewardValue.getTextWithoutSuffix("%").toDouble() / 100

        if (selectedTransactionRate !in transactionRange.min..transactionRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_transaction_not_in_range))
            return
        }
        if (selectedBakingRate !in bakingRange.min..bakingRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_baking_not_in_range))
            return
        }
        if (selectedRewardRate !in rewardRange.min..rewardRange.max) {
            showErrorMessage(getString(R.string.baking_commission_rate_error_reward_not_in_range))
            return
        }

        viewModel.setSelectedCommissionRates(
            transactionRate = selectedTransactionRate.truncateDecimals(5),
            bakingRate = selectedBakingRate.truncateDecimals(5),
            rewardRate = selectedRewardRate.truncateDecimals(5),
        )

        val intent = Intent(this, BakerRegistrationOpenActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
