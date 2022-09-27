package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.databinding.BurgerMenuContentBinding
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectActivity
import com.concordium.wallet.uicore.setEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class AccountDetailsActivity : BaseActivity(), EarnDelegate by EarnDelegateImpl() {
    private var mMenuDialog: AlertDialog? = null

    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var viewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_CONTINUE_TO_SHIELD_INTRO = "EXTRA_CONTINUE_TO_SHIELD_INTRO"
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.account_details_title)

        val account = intent.extras!!.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        val continueToShieldIntro = intent.extras!!.getBoolean(EXTRA_CONTINUE_TO_SHIELD_INTRO)
        initializeViewModel()
        viewModel.initialize(account, isShielded)
        initViews()

        if (continueToShieldIntro) {
            startShieldedIntroFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.populateTransferList()
        viewModel.initiateFrequentUpdater()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopFrequentUpdater()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountDetailsViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
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
        viewModel.totalBalanceLiveData.observe(this) { totalBalance ->
            if (viewModel.isShielded && totalBalance.second) {
                showAuthentication(null,
                    object : AuthenticationCallback {
                        override fun getCipherForBiometrics(): Cipher? {
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
            } else {
                showTotalBalance(totalBalance.first)
            }
        }

        viewModel.selectedTransactionForDecrytionLiveData.observe(this) { transaction ->
            showAuthentication(null,
                object : AuthenticationCallback {
                    override fun getCipherForBiometrics(): Cipher? {
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
        }

        viewModel.transferListLiveData.observe(this) {
            viewModel.checkForEncryptedAmounts()
        }

        viewModel.showPadLockLiveData.observe(this) {
            invalidateOptionsMenu()
        }

        viewModel.shieldingEnabledLiveData.observe(this) {
            //Show non-shielded options
            viewModel.isShielded = false
            initViews()
            //...then hide shielding options
            updateShieldEnabledUI()
        }

        viewModel.accountUpdatedLiveData.observe(this) {
            initTopContent()
        }
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
        binding.accountRetryButton.setOnClickListener {
            setResult(RESULT_RETRY_ACCOUNT_CREATION)
            finish()
        }
        binding.accountRemoveButton.setOnClickListener {
            viewModel.deleteAccountAndFinish()
        }
        binding.toggleBalance.setOnClickListener {
            viewModel.isShielded = false
            initViews()
        }
        binding.toggleShielded.setOnClickListener {
            viewModel.isShielded = true
            initViews()
        }
        binding.accountTotalDetailsDisposalText.text = if(viewModel.isShielded) resources.getString(R.string.account_shielded_total_details_disposal, viewModel.account.name) else resources.getString(R.string.account_total_details_disposal)
    }

    private fun updateShieldEnabledUI() {
        binding.toggleContainer.visibility = if(viewModel.shieldingEnabledLiveData.value == true) View.VISIBLE else View.GONE
        binding.toggleBalance.isSelected = !viewModel.isShielded
        binding.toggleShielded.isSelected = viewModel.isShielded
        binding.shieldedIcon.visibility = if(viewModel.shieldingEnabledLiveData.value == true && viewModel.isShielded) View.VISIBLE else View.GONE
        updateButtonsSlider()
    }

    private fun setFinalizedMode() {
        binding.buttonsSlider.setEnableButtons(!viewModel.account.readOnly)
        binding.accountDetailsLayout.visibility = View.VISIBLE
        binding.readonlyDesc.visibility = if(viewModel.account.readOnly) View.VISIBLE else View.GONE
        binding.accountsOverviewTotalDetailsBakerContainer.visibility = View.GONE
        binding.accountsOverviewTotalDetailsStakedContainer.visibility = View.GONE
        if (viewModel.isShielded) {
            binding.accountsOverviewTotalDetailsDisposalContainer.visibility = View.GONE
        }
        else {
            binding.accountsOverviewTotalDetailsDisposalContainer.visibility = View.VISIBLE
            if (viewModel.account.isBaking()) {
                binding.accountsOverviewTotalDetailsBakerContainer.visibility = View.VISIBLE
                binding.accountsOverviewTotalTitleBaker.text = getString(R.string.account_details_stake_with_baker, viewModel.account.accountBaker?.bakerId?.toString() ?: "")
                binding.accountsOverviewTotalDetailsBaker.text = CurrencyUtil.formatGTU(viewModel.account.accountBaker?.stakedAmount ?: "0", true)
            } else if (viewModel.account.isDelegating()) {
                binding.accountsOverviewTotalDetailsStakedContainer.visibility = View.VISIBLE
                if (viewModel.account.accountDelegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_L_POOL)
                    binding.accountsOverviewTotalTitleStaked.text = getString(R.string.account_details_delegation_with_passive_pool)
                else
                    binding.accountsOverviewTotalTitleStaked.text = getString(R.string.account_details_delegation_with_baker_pool, viewModel.account.accountDelegation?.delegationTarget?.bakerId ?: "")
                binding.accountsOverviewTotalDetailsStaked.text = CurrencyUtil.formatGTU(viewModel.account.accountDelegation?.stakedAmount ?: "", true)
            }
        }
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.accountRetryButton.visibility = View.VISIBLE
        binding.accountRemoveButton.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        binding.buttonsSlider.setEnableButtons(false)
    }

    private fun initTabs() {
        val adapter = AccountDetailsPagerAdapter(supportFragmentManager, viewModel.account, this)
        binding.accountDetailsPager.adapter = adapter
        binding.accountDetailsTablayout.setupWithViewPager(binding.accountDetailsPager)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
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
                item.icon = AppCompatResources.getDrawable(this, R.drawable.burger_closed_to_open_anim)
                (item.icon as Animatable).start()
                val builder = AlertDialog.Builder(this)
                builder.setOnDismissListener {
                    item.icon = AppCompatResources.getDrawable(this, R.drawable.burger_open_to_closed_anim)
                    (item.icon as Animatable).start()
                }

                val menuView = BurgerMenuContentBinding.inflate(layoutInflater)

                //Release schedule
                menuView.menuItemRelease.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
                menuView.menuItemRelease.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoAccountReleaseSchedule(viewModel.account, viewModel.isShielded)
                }

                //Transfer filters settings
                menuView.menuItemFilter.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
                menuView.menuItemFilter.setOnClickListener {
                    mMenuDialog?.dismiss()
                    gotoTransferFilters(viewModel.account)
                }

                menuView.menuShowShieldedContainer.visibility = if (viewModel.shieldingEnabledLiveData.value == true || viewModel.account.readOnly) View.GONE else View.VISIBLE
                menuView.menuShowShieldedContainer.setOnClickListener {
                    mMenuDialog?.dismiss()
                    startShieldedIntroFlow()
                }
                menuView.menuShowShielded.text = getString(R.string.account_details_menu_show_shielded, viewModel.account.name)

                menuView.menuHideShieldedContainer.visibility = if (viewModel.shieldingEnabledLiveData.value == true && !viewModel.account.readOnly) View.VISIBLE else View.GONE
                menuView.menuHideShieldedContainer.setOnClickListener {
                    mMenuDialog?.dismiss()
                    viewModel.disableShielded()
                }
                menuView.menuHideShielded.text = getString(R.string.account_details_menu_hide_shielded, viewModel.account.name)

                //Decrypt option
                menuView.menuDecryptContainer.visibility = if(viewModel.isShielded && viewModel.hasTransactionsToDecrypt) View.VISIBLE else View.GONE
                menuView.menuDecryptContainer.setOnClickListener {
                    mMenuDialog?.dismiss()
                    showAuthentication(null, object : AuthenticationCallback {
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

                // Change account name
                menuView.menuChangeAccountNameContainer.setOnClickListener {
                    mMenuDialog?.dismiss()
                    showChangeNameDialog()
                }

                builder.setCustomTitle(menuView.root)
                mMenuDialog = builder.show()

                mMenuDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
           }
        }
        return true
    }

    private fun showChangeNameDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.account_details_change_name_popup_title))
        builder.setMessage(getString(R.string.account_details_change_name_popup_subtitle))
        val input = AppCompatEditText(this)
        input.hint = viewModel.account.name
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setEditText(this, input)
        builder.setPositiveButton(getString(R.string.account_details_change_name_popup_save)) { _, _ ->
            viewModel.changeAccountName(input.text.toString())
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private val getResultEnableShielding =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getBooleanExtra(ShieldingIntroActivity.EXTRA_RESULT_SHIELDING_ENABLED, false)?.let { enabled ->
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

    private fun startShieldedIntroFlow() {
        val intent = Intent(this, ShieldingIntroActivity::class.java)
        getResultEnableShielding.launch(intent)
    }

    private fun gotoAccountReleaseSchedule(item: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT, item)
        intent.putExtra(EXTRA_SHIELDED, isShielded)
        startActivity(intent)
    }

    private fun gotoTransferFilters(item: Account) {
        val intent = Intent(this, AccountTransactionsFiltersActivity::class.java)
        intent.putExtra(EXTRA_ACCOUNT, item)
        startActivity(intent)
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showTotalBalance(totalBalance: Long) {
        binding.balanceTextview.text = CurrencyUtil.formatGTU(totalBalance)
        binding.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(viewModel.account.getAtDisposalWithoutStakedOrScheduled(totalBalance), true)
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

    private fun scan() {
        val intent = Intent(this, ScanQRActivity::class.java)
        intent.putExtra(ScanQRActivity.QR_MODE, ScanQRActivity.QR_MODE_WALLET_CONNECT)
        getResultScanQr.launch(intent)
    }

    private val getResultScanQr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { wcUri ->
                    val intent = Intent(this, WalletConnectActivity::class.java)
                    intent.putExtra(WalletConnectActivity.FROM_DEEP_LINK, false)
                    intent.putExtra(WalletConnectActivity.WC_URI, wcUri)
                    startActivity(intent)
                }
            }
        }

    private fun updateButtonsSlider() {
        binding.buttonsSlider.removeAllButtons()
        if (viewModel.isShielded) {
            binding.buttonsSlider.addButton(R.drawable.ic_icon_send_shielded) {
                onSendFundsClicked()
            }
        } else {
            binding.buttonsSlider.addButton(R.drawable.ic_send) {
                onSendFundsClicked()
            }
        }
        binding.buttonsSlider.addButton(R.drawable.ic_recipient_address_qr) {
            onAddressClicked()
        }
        if ((viewModel.shieldingEnabledLiveData.value == true && !viewModel.isShielded) || viewModel.shieldingEnabledLiveData.value == false) {
            binding.buttonsSlider.addButton(R.drawable.ic_earn) {
                gotoEarn(this, viewModel.account, viewModel.hasPendingDelegationTransactions, viewModel.hasPendingBakingTransactions)
            }
        }
        binding.buttonsSlider.addButton(R.drawable.ic_scan) {
            scan()
        }
        if (viewModel.shieldingEnabledLiveData.value == true) {
            if (viewModel.isShielded) {
                binding.buttonsSlider.addButton(R.drawable.ic_unshield) {
                    onShieldFundsClicked()
                }
            } else {
                binding.buttonsSlider.addButton(R.drawable.ic_shielded_icon) {
                    onShieldFundsClicked()
                }
            }
        }
        binding.buttonsSlider.commitButtons()
    }
}
