package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.PendingChange
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate
import kotlinx.android.synthetic.main.delegationbaker_status.*
import kotlinx.android.synthetic.main.progress.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class StatusActivity(titleId: Int) :
    BaseActivity(R.layout.delegationbaker_status, titleId) {

    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        ).get(DelegationBakerViewModel::class.java)
    }

    fun setContentTitle(res: Int){
        status_title.text = getString(res)
    }

    fun addContent(titleRes: Int, text: String) {
        addContent(getString(titleRes), text)
    }

    fun addContent(title: String, text: String, titleTextColor: Int? = null) {
        status_empty.visibility = View.GONE
        status_list_container.visibility = View.VISIBLE

        val view = LayoutInflater.from(this).inflate(R.layout.delegation_baker_status_content_item, null)

        if (title.isNotEmpty())
        {
            val titleView = view.findViewById<TextView>(R.id.status_item_title)
            titleView.text = title
            titleTextColor?.let {
                titleView.setTextColor(getColor(it))
            }
        }
        else
            view.findViewById<TextView>(R.id.status_item_title).visibility = View.GONE

        if (text.isNotEmpty())
            view.findViewById<TextView>(R.id.status_item_content).text = text
        else
            view.findViewById<TextView>(R.id.status_item_content).visibility = View.GONE

        status_list_container.addView(view)
    }

    fun setEmptyState(text: String){
        status_empty.text = text
        status_empty.visibility = View.VISIBLE
        status_list_container.visibility = View.GONE
    }

    fun clearState(){
        status_empty.text = ""
        status_list_container.removeAllViews()
        status_button_top.isEnabled = true
        status_button_bottom.isEnabled = true
    }

    protected fun addPendingChange(pendingChange: PendingChange, dateStringId: Int, takeEffectOnStringId: Int, removeStakeStringId: Int, reduceStakeStringId: Int) {
        pendingChange.estimatedChangeTime?.let { estimatedChangeTime ->
            val prefix = estimatedChangeTime.toDate()?.formatTo("yyyy-MM-dd")
            val postfix = estimatedChangeTime.toDate()?.formatTo("HH:mm")
            val dateStr = getString(dateStringId, prefix, postfix)
            addContent(getString(takeEffectOnStringId) + "\n" + dateStr, "")
            if (pendingChange.change == "RemoveStake") {
                status_button_top.isEnabled = false
                addContent(getString(removeStakeStringId), "")
            } else if (pendingChange.change == "ReduceStake") {
                pendingChange.newStake?.let { newStake ->
                    addContent(getString(reduceStakeStringId), CurrencyUtil.formatGTU(newStake, true))
                }
            }
        }
    }

    protected fun addWaitingForTransaction(contentTitleStringId: Int, emptyStateStringId: Int) {
        findViewById<ImageView>(R.id.status_icon).setImageResource(R.drawable.ic_logo_icon_pending)
        status_button_top.isEnabled = false
        status_button_bottom.isEnabled = false
        setContentTitle(contentTitleStringId)
        setEmptyState(getString(emptyStateStringId))
    }

    abstract fun initView()

    protected open fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }
}
