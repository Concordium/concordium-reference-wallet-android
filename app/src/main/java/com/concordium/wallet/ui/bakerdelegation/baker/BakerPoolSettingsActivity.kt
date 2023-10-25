package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakingCommissionRange
import com.concordium.wallet.data.model.FinalizationCommissionRange
import com.concordium.wallet.data.model.TransactionCommissionRange
import com.concordium.wallet.databinding.ActivityBakerSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
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

    private fun ActivityBakerSettingsBinding.setRewardRates(rewardRange: FinalizationCommissionRange) {
        if (rewardRange.min != rewardRange.max) {
            rewardGroup.visibility = View.VISIBLE
            rewardMin.text = getMinRangeText(rewardRange.min)
            rewardMax.text = getMaxRangeText(rewardRange.max)
            rewardSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(rewardRange.max)
                min = getSliderRangeValue(rewardRange.min)
                progress = getSliderDefaultProgressValue(rewardRange.max)
                rewardValue.text = getPercentageStringFromProgress(progress)

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        rewardValue.text = getPercentageStringFromProgress(progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            rewardGroup.visibility = View.GONE
            rewardValue.text = getPercentageString(rewardRange.max)
        }
    }

    private fun ActivityBakerSettingsBinding.setBakingRates(bakingRange: BakingCommissionRange) {
        if (bakingRange.min != bakingRange.max) {
            bakingGroup.visibility = View.VISIBLE
            bakingMin.text = getMinRangeText(bakingRange.min)
            bakingMax.text = getMaxRangeText(bakingRange.max)
            bakingSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(bakingRange.max)
                min = getSliderRangeValue(bakingRange.min)
                progress = getSliderDefaultProgressValue(bakingRange.max)
                bakingValue.text = getPercentageStringFromProgress(progress)

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        bakingValue.text = getPercentageStringFromProgress(progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            bakingGroup.visibility = View.GONE
            bakingValue.text = getPercentageString(bakingRange.max)
        }
    }

    private fun ActivityBakerSettingsBinding.setTransactionRates(
        transactionRange: TransactionCommissionRange
    ) {
        if (transactionRange.min != transactionRange.max) {
            transactionFeeGroup.visibility = View.VISIBLE
            transactionFeeMin.text = getMinRangeText(transactionRange.min)
            transactionFeeMax.text = getMaxRangeText(transactionRange.max)
            transactionFeeSlider.apply {
                isEnabled = true
                max = getSliderRangeValue(transactionRange.max)
                min = getSliderRangeValue(transactionRange.min)
                progress = getSliderDefaultProgressValue(transactionRange.max)
                transactionFeeValue.text = getPercentageStringFromProgress(progress)

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?, progress: Int, fromUser: Boolean
                    ) {
                        transactionFeeValue.text = getPercentageStringFromProgress(progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        } else {
            transactionFeeGroup.visibility = View.GONE
            transactionFeeValue.text = getPercentageString(transactionRange.max)
        }
    }

    private fun getSliderRangeValue(value: Double): Int =
        (value * RATE_RANGE_FRACTION_MULTIPLIER).toInt()

    private fun getSliderDefaultProgressValue(max: Double) =
        (max * RATE_RANGE_FRACTION_MULTIPLIER).roundToInt()

    private fun getPercentageStringFromProgress(progress: Int) =
        "${valueAsPercentageString(progress.toDouble() / 1000)}%"

    private fun getPercentageString(value: Double) = "${valueAsPercentageString(value * 100)}%"


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

    private fun onContinueClicked() {
        val chainParams = viewModel.bakerDelegationData.chainParameters
        val transactionRange = chainParams?.transactionCommissionRange
        val bakingRange = chainParams?.bakingCommissionRange
        val rewardRange = chainParams?.finalizationCommissionRange

        viewModel.setSelectedCommissionRates(
            transactionRate = if (transactionRange != null && transactionRange.min != transactionRange.max) {
                binding.transactionFeeSlider.progress.toDouble() / 100
            } else {
                transactionRange?.max
            },
            bakingRate = if (bakingRange != null && bakingRange.min != bakingRange.max) {
                binding.bakingSlider.progress.toDouble() / 100
            } else {
                bakingRange?.max
            },
            rewardRate = if (rewardRange != null && rewardRange.min != rewardRange.max) {
                binding.rewardSlider.progress.toDouble() / 100
            } else {
                rewardRange?.max
            },
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
