package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRegistrationIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationStatusActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationCreateIntroFlowActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import kotlinx.android.synthetic.main.activity_account_details.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class AccountDetailsActivity :
    BaseActivity(R.layout.activity_account_details, R.string.account_details_title) {

    private var mMenuDialog: AlertDialog? = null

    private lateinit var viewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_CONTINUE_TO_SHIELD_INTRO = "EXTRA_CONTINUE_TO_SHIELD_INTRO"
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
        const val REQUESTCODE_ENABLE_SHIELDING = 1241
    }

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        val continueToShieldIntro = intent.extras!!.getBoolean(EXTRA_CONTINUE_TO_SHIELD_INTRO)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        initViews()

        if(continueToShieldIntro){
            startShieldedIntroFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.populateTransferList()
        viewModel.initiateFrequentUpdater()
    }


    override fun onPause() {
        super.onPause()
        viewModel.stopFrequentUpdater()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUESTCODE_ENABLE_SHIELDING) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getBooleanExtra(ShieldingIntroActivity.EXTRA_RESULT_SHIELDING_ENABLED, false)?.let { enabled ->
                    if(enabled){
                        viewModel.enableShielded()
                        //Decouple from main thread allowing UI to update
                        GlobalScope.launch(Dispatchers.Main){
                            delay(1)
                            viewModel.isShielded = true
                            initViews()
                            updateShieldEnabledUI()
                        }
                    }
                }
            }
        }
    }
    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AccountDetailsViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.finishLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                finish()
            }
        })
        viewModel.totalBalanceLiveData.observe(this, Observer<Pair<Long,Boolean>> { totalBalance ->
            if(viewModel.isShielded && totalBalance.second){
                showAuthentication(null, viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
                    override fun getCipherForBiometrics() : Cipher?{
                        return viewModel.getCipherForBiometrics()
                    }
                    override fun onCorrectPassword(password: String) {
                        viewModel.continueWithPassword(password)
                    }
                    override fun onCipher(cipher: Cipher) {
                        viewModel.checkLogin(cipher)
                    }
                    override fun onCancelled() {
                        finish()
                    }
                })
            }
            else{
                showTotalBalance(totalBalance.first)
            }
        })

        viewModel.selectedTransactionForDecrytionLiveData.observe(this, Observer<Transaction> { transaction ->
            showAuthentication(null, viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
                override fun getCipherForBiometrics() : Cipher?{
                    return viewModel.getCipherForBiometrics()
                }
                override fun onCorrectPassword(password: String) {
                    viewModel.continueWithPassword(password, true, transaction)
                }
                override fun onCipher(cipher: Cipher) {
                    viewModel.checkLogin(cipher, true, transaction)
                }
                override fun onCancelled() {
                    finish()
                }
            })
        })

        viewModel.transferListLiveData.observe(this, Observer { transferList ->
            viewModel.checkForUndecryptedAmounts()
        })

        viewModel.showPadLockLiveData.observe(this, Observer { showPadlock ->
            invalidateOptionsMenu()
        })

        viewModel.shieldingEnabledLiveData.observe(this, Observer { showShielded ->
            //Show non-shielded options
            viewModel.isShielded = false
            initViews()
            //...then hide shielding options
            updateShieldEnabledUI()
        })

        viewModel.accountUpdatedLiveData.observe(this, Observer {
            initTopContent()
        })
    }

    private fun initViews() {
        showWaiting(false)

        initTopContent()


        initTabs()

        updateShieldEnabledUI()

    }

    private fun initTopContent() {
        setActionBarTitle(getString(if(viewModel.isShielded) R.string.account_details_title_shielded_balance else R.string.account_details_title_regular_balance, viewModel.account.getAccountName()))
        when (viewModel.account.transactionStatus) {
            TransactionStatus.ABSENT -> {
                setErrorMode()
            }
            TransactionStatus.FINALIZED -> {
                setFinalizedMode()
            }
            TransactionStatus.COMMITTED -> setPendingMode()
            TransactionStatus.RECEIVED -> setPendingMode()
            else -> {
            }
        }
        account_retry_button.setOnClickListener {
            setResult(RESULT_RETRY_ACCOUNT_CREATION)
            finish()
        }
        account_remove_button.setOnClickListener {
            viewModel.deleteAccountAndFinish()
        }
        send_funds_layout.setOnClickListener {
            onSendFundsClicked()
        }
        address_layout.setOnClickListener {
            onAddressClicked()
        }
        shield_funds_layout.setOnClickListener {
            onShieldFundsClicked()
        }
        toggle_balance.setOnClickListener {
            viewModel.isShielded = false
            initViews()
        }
        toggle_shielded.setOnClickListener {
            viewModel.isShielded = true
            initViews()
        }

        shield_textview.text = if(viewModel.isShielded) resources.getText(R.string.account_details_unshield) else resources.getText(R.string.account_details_shield)

        account_total_details_disposal_text.text = if(viewModel.isShielded) resources.getString(R.string.account_shielded_total_details_disposal, viewModel.account.name) else resources.getString(R.string.account_total_details_disposal)

    }

    private fun updateShieldEnabledUI() {
        shield_funds_layout.visibility = if(viewModel.shieldingEnabledLiveData.value == true) View.VISIBLE else View.GONE
        toggle_container.visibility = if(viewModel.shieldingEnabledLiveData.value == true) View.VISIBLE else View.GONE
        toggle_balance.isSelected = !viewModel.isShielded
        toggle_shielded.isSelected = viewModel.isShielded
        shielded_icon.visibility = if(viewModel.shieldingEnabledLiveData.value == true && viewModel.isShielded) View.VISIBLE else View.GONE
    }


    private fun setFinalizedMode() {
        send_funds_layout.isEnabled = true && !viewModel.account.readOnly
        shield_funds_layout.isEnabled = true && !viewModel.account.readOnly
        address_layout.isEnabled = true
        account_details_layout.visibility = View.VISIBLE
        readonly_desc.visibility = if(viewModel.account.readOnly) View.VISIBLE else View.GONE

        accounts_overview_total_details_baker_container.visibility = View.GONE
        accounts_overview_total_details_staked_container.visibility = View.GONE

        if (viewModel.isShielded) {
            accounts_overview_total_details_disposal_container.visibility = View.GONE
            send_imageview.setImageResource(R.drawable.ic_icon_send_shielded)
            shield_imageview.setImageResource(R.drawable.ic_unshield)
        }
        else {
            accounts_overview_total_details_disposal_container.visibility = View.VISIBLE
            send_imageview.setImageResource(R.drawable.ic_send)
            shield_imageview.setImageResource(R.drawable.ic_shielded_icon)
            if (viewModel.account.isBaking()) {
                accounts_overview_total_details_baker_container.visibility = View.VISIBLE
                accounts_overview_total_title_baker.text = getString(R.string.account_details_stake_with_baker, viewModel.account.accountBaker?.bakerId?.toString() ?: "")
                accounts_overview_total_details_baker.text = CurrencyUtil.formatGTU(viewModel.account.accountBaker?.stakedAmount ?: "0", true)
            } else if (viewModel.account.isDelegating()) {
                accounts_overview_total_details_staked_container.visibility = View.VISIBLE
                if (viewModel.account.accountDelegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_L_POOL)
                    accounts_overview_total_title_staked.text = getString(R.string.account_details_delegation_with_passive_pool)
                else
                    accounts_overview_total_title_staked.text = getString(R.string.account_details_delegation_with_baker_pool, viewModel.account.accountDelegation?.delegationTarget?.bakerId ?: "")
                accounts_overview_total_details_staked.text = CurrencyUtil.formatGTU(viewModel.account.accountDelegation?.stakedAmount ?: "", true)
            }
        }
    }

    private fun setErrorMode() {
        setPendingMode()
        account_retry_button.visibility = View.VISIBLE
        account_remove_button.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        send_funds_layout.isEnabled = false
        shield_funds_layout.isEnabled = false
        address_layout.isEnabled = false
    }

    private fun initTabs() {
        val adapter = AccountDetailsPagerAdapter(supportFragmentManager, viewModel.account, this)
        account_details_pager.adapter = adapter
        account_details_tablayout.setupWithViewPager(account_details_pager)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.release_schedule, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.item_menu -> {
                item.icon = resources.getDrawable(R.drawable.burger_closed_to_open_anim, null)
                (item.icon as Animatable).start()
                val builder = AlertDialog.Builder(this)
                builder.setOnDismissListener {
                    item.icon = resources.getDrawable(R.drawable.burger_open_to_closed_anim, null)
                    (item.icon as Animatable).start()
                }

                //Release schedule
                val menuView = View.inflate(this, R.layout.burger_menu_content, null)

                val tvReleaseSchedule = menuView.findViewById(R.id.menu_item_release) as TextView
                tvReleaseSchedule.visibility = if(viewModel.isShielded) View.GONE else View.VISIBLE
                tvReleaseSchedule.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoAccountReleaseSchedule(viewModel.account, viewModel.isShielded)
                }

                //Transfer filters settings
                val tvTransferFilter = menuView.findViewById(R.id.menu_item_filter) as TextView
                tvTransferFilter.visibility = if(viewModel.isShielded) View.GONE else View.VISIBLE
                tvTransferFilter.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoTransferFilters(viewModel.account, viewModel.isShielded)
                }

                val cvShowShielded = menuView.findViewById(R.id.menu_show_shielded_container) as CardView
                val cvShowShieldedTV = menuView.findViewById(R.id.menu_show_shielded) as TextView
                cvShowShielded.visibility = if(viewModel.shieldingEnabledLiveData.value == true || viewModel.account.readOnly == true) View.GONE else View.VISIBLE
                cvShowShielded.setOnClickListener {
                    mMenuDialog?.dismiss()
                    startShieldedIntroFlow()
                }
                cvShowShieldedTV.text = getString(R.string.account_details_menu_show_shielded, viewModel.account.name)

                val cvHideShielded = menuView.findViewById(R.id.menu_hide_shielded_container) as CardView
                val cvHideShieldedTV = menuView.findViewById(R.id.menu_hide_shielded) as TextView
                cvHideShielded.visibility = if(viewModel.shieldingEnabledLiveData.value == true && viewModel.account.readOnly != true) View.VISIBLE else View.GONE
                cvHideShielded.setOnClickListener {
                    mMenuDialog?.dismiss()
                    viewModel.disableShielded()
                }
                cvHideShieldedTV.text = getString(R.string.account_details_menu_hide_shielded, viewModel.account.name)

                //Delegation
                val tvDelegation = menuView.findViewById(R.id.menu_item_delegation) as TextView
                tvDelegation.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoDelegation(viewModel.account)
                }

                //Baking
                val tvBaking = menuView.findViewById(R.id.menu_item_baking) as TextView
                tvBaking.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoBaking(viewModel.account)
                }

                //Decrypt option
                val cvDecrypt = menuView.findViewById(R.id.menu_decrypt_container) as CardView
                cvDecrypt.visibility = if(viewModel.isShielded && viewModel.hasTransactionsToDecrypt) View.VISIBLE else View.GONE
                cvDecrypt.setOnClickListener {
                    mMenuDialog?.dismiss()
                    showAuthentication(null, viewModel.shouldUseBiometrics(), viewModel.usePasscode(), object : AuthenticationCallback{
                        override fun getCipherForBiometrics() : Cipher?{
                            return viewModel.getCipherForBiometrics()
                        }
                        override fun onCorrectPassword(password: String) {
                            viewModel.continueWithPassword(password, true)
                        }
                        override fun onCipher(cipher: Cipher) {
                            viewModel.checkLogin(cipher, true)
                        }
                        override fun onCancelled() {
                            finish()
                        }
                    })
                }



                builder.setCustomTitle(menuView)
                mMenuDialog = builder.show()

                mMenuDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
           }
        }
        return true
    }

    private fun startShieldedIntroFlow() {
        val intent = Intent(this, ShieldingIntroActivity::class.java)
        startActivityForResult(intent, REQUESTCODE_ENABLE_SHIELDING)
    }

    private fun gotoAccountReleaseSchedule(item: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT, item)
        intent.putExtra(EXTRA_SHIELDED, isShielded)
        startActivity(intent)
    }

    private fun gotoTransferFilters(item: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountTransactionsFiltersActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT, item)
        startActivity(intent)
    }

    private fun gotoDelegation(account: Account) {
        val intent = if (account.accountDelegation != null || viewModel.hasPendingTransactions) {
            Intent(this, DelegationStatusActivity::class.java)
            intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, DelegationData(account, isTransactionInProgress = viewModel.hasPendingTransactions, type = UPDATE_DELEGATION))
        } else {
            Intent(this, DelegationCreateIntroFlowActivity::class.java)
            intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
            intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, DelegationData(account, type = REGISTER_DELEGATION))
        }
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoBaking(account: Account) {
        val intent = if (account.isBaking()) {
            Intent(this, BakerStatusActivity::class.java)
        }
        else {
            Intent(this, BakerRegistrationIntroFlow::class.java)
            intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        }
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, DelegationData(account, type = REGISTER_BAKER))
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun showTotalBalance(totalBalance: Long) {
        balance_textview.text = CurrencyUtil.formatGTU(totalBalance)
        accounts_overview_total_details_disposal.text = CurrencyUtil.formatGTU(totalBalance - viewModel.account.getAtDisposalSubstraction(), true)
    }

    private fun onSendFundsClicked() {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, viewModel.isShielded)
        intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun onShieldFundsClicked() {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, viewModel.isShielded)
        intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, viewModel.account)
        intent.putExtra(SendFundsActivity.EXTRA_RECIPIENT, Recipient(viewModel.account.id, viewModel.account.name, viewModel.account.address))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun onAddressClicked() {
        val intent = Intent(this, AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }
    //endregion
}

