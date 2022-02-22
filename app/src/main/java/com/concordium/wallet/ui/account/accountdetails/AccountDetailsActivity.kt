package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.accountdetails.transfers.AdapterItem
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.util.ImageUtil
import kotlinx.android.synthetic.main.activity_account_details.*
import kotlinx.android.synthetic.main.activity_account_details.accounts_overview_total_details_staked
import kotlinx.android.synthetic.main.activity_account_details.accounts_overview_total_details_disposal

import kotlinx.android.synthetic.main.activity_account_details.root_layout
import kotlinx.android.synthetic.main.progress.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.crypto.Cipher


class AccountDetailsActivity :
    BaseActivity(R.layout.activity_account_details, R.string.account_details_title) {

    private var mMenuDialog: AlertDialog? = null

    private lateinit var viewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
        const val REQUESTCODE_ENABLE_SHIELDING = 1241

    }


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        initViews()
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
            updateShieldEnabledUI()
        })

    }


    private fun initViews() {
        setActionBarTitle(getString(if(viewModel.isShielded) R.string.account_details_title_shielded_balance else R.string.account_details_title_regular_balance, viewModel.account.getAccountName()))
        showWaiting(false)
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

        initTabs()

        updateShieldEnabledUI()

        if(viewModel.hasTransactionsToDecrypt
            && !App.appCore.session.isShieldedWarningDismissed(viewModel.account.address)
            && !App.appCore.session.isShieldingEnabled(viewModel.account.address)){

            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.account_details_shielded_warning_title))
            builder.setMessage(getString(R.string.account_details_shielded_warning_text))
            builder.setNegativeButton(getString(R.string.account_details_shielded_warning_enable), object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which:Int) {
                    startShieldedIntroFlow()
                }
            })
            builder.setPositiveButton(getString(R.string.account_details_shielded_warning_dismiss), object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which:Int) {
                    App.appCore.session.setShieldedWarningDismissed(viewModel.account.address, true)
                }
            })
            builder.setCancelable(true)
            builder.create().show()
        }

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

        if(viewModel.isShielded){
            //accounts_overview_total_details_baker_container.visibility = View.GONE
            accounts_overview_total_details_staked_container.visibility = View.GONE
            accounts_overview_total_details_disposal_container.visibility = View.GONE
            send_imageview.setImageResource(R.drawable.ic_icon_send_shielded)
            shield_imageview.setImageResource(R.drawable.ic_unshield)
        }
        else{
            //accounts_overview_total_details_baker_container.visibility = View.VISIBLE
            accounts_overview_total_details_staked_container.visibility = View.VISIBLE
            accounts_overview_total_details_disposal_container.visibility = View.VISIBLE
            send_imageview.setImageResource(R.drawable.ic_send)
            shield_imageview.setImageResource(R.drawable.ic_shielded_icon)

            if(viewModel.account.isBaker()){
                //accounts_overview_total_details_baker_container.visibility = View.VISIBLE
                accounts_overview_total_details_baker_id.text = viewModel.account.bakerId.toString()
                accounts_overview_total_details_staked_container.visibility = View.VISIBLE
            }
            else{
                //accounts_overview_total_details_baker_container.visibility = View.GONE
                accounts_overview_total_details_staked_container.visibility = View.GONE
            }
            accounts_overview_total_details_staked.text = CurrencyUtil.formatGTU(viewModel.account.totalStaked, true)
        }

        if(!viewModel.account.readOnly){
            send_textview.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.text_black
                )
            )
            shield_textview.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.text_black
                )
            )
            ImageUtil.changeImageViewTintColor(
                send_imageview,
                R.color.theme_black
            )
            ImageUtil.changeImageViewTintColor(
                shield_imageview,
                R.color.theme_black
            )
        }
        address_textview.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.text_black
            )
        )
        ImageUtil.changeImageViewTintColor(
            address_imageview,
            R.color.theme_black
        )
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
                cvShowShielded.visibility = if(viewModel.shieldingEnabledLiveData.value == true) View.GONE else View.VISIBLE
                cvShowShielded.setOnClickListener {
                    mMenuDialog?.dismiss()
                    startShieldedIntroFlow()
                }
                cvShowShieldedTV.text = getString(R.string.account_details_menu_show_shielded, viewModel.account.name)

                val cvHideShielded = menuView.findViewById(R.id.menu_hide_shielded_container) as CardView
                val cvHideShieldedTV = menuView.findViewById(R.id.menu_hide_shielded) as TextView
                cvHideShielded.visibility = if(viewModel.shieldingEnabledLiveData.value == true) View.VISIBLE else View.GONE
                cvHideShielded.setOnClickListener {
                    mMenuDialog?.dismiss()
                    viewModel.disableShielded()
                }
                cvHideShieldedTV.text = getString(R.string.account_details_menu_hide_shielded, viewModel.account.name)

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
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item)
        intent.putExtra(AccountDetailsActivity.EXTRA_SHIELDED, isShielded)
        startActivity(intent)
    }

    private fun gotoTransferFilters(item: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountTransactionsFiltersActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item)
        startActivity(intent)
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

