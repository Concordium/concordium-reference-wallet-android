package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.PendingChange
import com.concordium.wallet.data.util.CurrencyUtilImpl
import com.concordium.wallet.databinding.DelegationBakerStatusContentItemBinding
import com.concordium.wallet.databinding.DelegationbakerStatusBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class StatusActivity(private val titleId: Int) : BaseActivity() {
    protected lateinit var binding: DelegationbakerStatusBinding
    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DelegationbakerStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, titleId)

        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as BakerDelegationData)
        initView()
    }

    override fun onStart() {
        super.onStart()
        // subscribe to messages from AccountDetailsViewModel
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        // unsubscribe from messages from AccountDetailsViewModel
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(bakerDelegationData: BakerDelegationData) {
        // receive message from AccountDetailsViewModel
        viewModel.bakerDelegationData = bakerDelegationData
        initView()
    }

    protected open fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DelegationBakerViewModel::class.java]
    }

    fun setContentTitle(res: Int){
        binding.statusTitle.text = getString(res)
    }

    fun addContent(titleRes: Int, text: String) {
        addContent(getString(titleRes), text)
    }

    fun addContent(title: String, text: String, titleTextColor: Int? = null) {
        binding.statusEmpty.visibility = View.GONE
        binding.statusListContainer.visibility = View.VISIBLE

        val delegationBakerStatusBinding = DelegationBakerStatusContentItemBinding.inflate(layoutInflater)

        if (title.isNotEmpty())
        {
            delegationBakerStatusBinding.statusItemTitle.text = title
            titleTextColor?.let {
                delegationBakerStatusBinding.statusItemTitle.setTextColor(getColor(it))
            }
        }
        else
            delegationBakerStatusBinding.statusItemTitle.visibility = View.GONE

        if (text.isNotEmpty())
            delegationBakerStatusBinding.statusItemContent.text = text
        else
            delegationBakerStatusBinding.statusItemContent.visibility = View.GONE

        binding.statusListContainer.addView(delegationBakerStatusBinding.root)
    }

    fun setEmptyState(text: String){
        binding.statusEmpty.text = text
        binding.statusEmpty.visibility = View.VISIBLE
        binding.statusListContainer.visibility = View.GONE
    }

    fun clearState(){
        binding.statusEmpty.text = ""
        binding.statusListContainer.removeAllViews()
        binding.statusButtonTop.isEnabled = true
        binding.statusButtonBottom.isEnabled = true
    }

    protected fun addPendingChange(pendingChange: PendingChange, dateStringId: Int, takeEffectOnStringId: Int, removeStakeStringId: Int, reduceStakeStringId: Int) {
        pendingChange.estimatedChangeTime?.let { estimatedChangeTime ->
            val prefix = estimatedChangeTime.toDate()?.formatTo("yyyy-MM-dd")
            val postfix = estimatedChangeTime.toDate()?.formatTo("HH:mm")
            val dateStr = getString(dateStringId, prefix, postfix)
            addContent(getString(takeEffectOnStringId) + "\n" + dateStr, "")
            if (pendingChange.change == "RemoveStake") {
                binding.statusButtonTop.isEnabled = false
                addContent(getString(removeStakeStringId), "")
            } else if (pendingChange.change == "ReduceStake") {
                pendingChange.newStake?.let { newStake ->
                    addContent(getString(reduceStakeStringId), CurrencyUtilImpl.formatGTU(newStake, true))
                }
            }
        }
    }

    protected fun addWaitingForTransaction(contentTitleStringId: Int, emptyStateStringId: Int) {
        binding.statusIcon.setImageResource(R.drawable.ic_logo_icon_pending)
        binding.statusButtonTop.isEnabled = false
        binding.statusButtonBottom.isEnabled = false
        setContentTitle(contentTitleStringId)
        setEmptyState(getString(emptyStateStringId))
    }

    abstract fun initView()

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }
}
