package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.cis2.TokensFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.cis2.lookfornew.LookForNewTokensFragment
import com.concordium.wallet.ui.cis2.lookfornew.TokenSelectedDestination
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectActivity
import com.concordium.wallet.uicore.afterMeasured
import com.concordium.wallet.util.TokenUtil
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger
import javax.crypto.Cipher

class AccountDetailsActivity : BaseActivity(), EarnDelegate by EarnDelegateImpl() {
    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var viewModelAccountDetails: AccountDetailsViewModel
    private lateinit var viewModelTokens: TokensViewModel
    private var accountAddress = ""
    private var lookForNewTokensFragment: LookForNewTokensFragment? = null
    private var selectedTabIndex: Int = 0

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
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.account_details_title
        )
        initializeViewModelAccountDetails()
        viewModelAccountDetails.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initializeViewModelTokens()
        val isShielded = intent.extras!!.getBoolean(EXTRA_SHIELDED)
        val continueToShieldIntro = intent.extras!!.getBoolean(EXTRA_CONTINUE_TO_SHIELD_INTRO)
        accountAddress = viewModelAccountDetails.account.address
        viewModelAccountDetails.initialize(viewModelAccountDetails.account, isShielded)
        initViews()
        if (continueToShieldIntro) {
            gotoAccountSettings(true)
        }
        supportFragmentManager.beginTransaction().replace(
            R.id.tokens_fragment,
            TokensFragment.newInstance(
                viewModelTokens,
                viewModelAccountDetails.account.address,
                true
            ),
            null
        ).commit()
        binding.balances.afterMeasured {
            binding.balances.minimumHeight = binding.balances.height
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelAccountDetails.loadAccount(accountAddress)
        viewModelAccountDetails.populateTransferList()
        viewModelAccountDetails.initiateFrequentUpdater()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelAccountDetails.stopFrequentUpdater()
    }

    private fun initializeViewModelTokens() {
        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]

        viewModelTokens.tokenData.account = viewModelAccountDetails.account

        viewModelTokens.waiting.observe(this) { waiting ->
            waiting?.let {
                showWaitingTokens(waiting)
            }
        }

        viewModelTokens.chooseToken.observe(this) { token ->
            showTokenDetailsDialog(token)
        }

        viewModelTokens.updateWithSelectedTokensDone.observe(this) { anyChanges ->
            runOnUiThread {
                lookForNewTokensFragment?.dismiss()
                lookForNewTokensFragment = null
                if (anyChanges) {
                    when (viewModelTokens.addTokenDestination.value) {
                        TokenSelectedDestination.TOKEN -> {
                            viewModelTokens.addTokenDestination.value =
                                TokenSelectedDestination.NoChange
                            onTabFungible()
                        }

                        TokenSelectedDestination.NFT -> {
                            viewModelTokens.addTokenDestination.value =
                                TokenSelectedDestination.NoChange
                            onTabCollectibles()
                        }

                        else -> if (selectedTabIndex == 0) {
                            onTabFungible()
                        } else {
                            onTabCollectibles()
                        }
                    }
                    Toast.makeText(this, R.string.cis_tokens_updated, Toast.LENGTH_SHORT).show()
                } else {
                    if (selectedTabIndex == 0) {
                        onTabFungible()
                    } else {
                        onTabCollectibles()
                    }
                    Toast.makeText(this, R.string.cis_tokens_not_updated, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializeViewModelAccountDetails() {
        viewModelAccountDetails = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountDetailsViewModel::class.java]

        viewModelAccountDetails.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaitingTransactions(waiting)
            }
        }
        viewModelAccountDetails.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModelAccountDetails.finishLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                finish()
            }
        })
        viewModelAccountDetails.totalBalanceLiveData.observe(this) { totalBalance ->
            if (viewModelAccountDetails.isShielded && totalBalance.second) {
                showAuthentication(null,
                    object : AuthenticationCallback {
                        override fun getCipherForBiometrics(): Cipher? {
                            return viewModelAccountDetails.getCipherForBiometrics()
                        }

                        override fun onCorrectPassword(password: String) {
                            viewModelAccountDetails.continueWithPassword(password)
                        }

                        override fun onCipher(cipher: Cipher) {
                            viewModelAccountDetails.checkLogin(cipher)
                        }

                        override fun onCancelled() {
                            finish()
                        }
                    })
            } else {
                showTotalBalance(totalBalance.first)
            }
        }

        viewModelAccountDetails.selectedTransactionForDecrytionLiveData.observe(this) { transaction ->
            showAuthentication(null,
                object : AuthenticationCallback {
                    override fun getCipherForBiometrics(): Cipher? {
                        return viewModelAccountDetails.getCipherForBiometrics()
                    }

                    override fun onCorrectPassword(password: String) {
                        viewModelAccountDetails.continueWithPassword(password, true, transaction)
                    }

                    override fun onCipher(cipher: Cipher) {
                        viewModelAccountDetails.checkLogin(cipher, true, transaction)
                    }

                    override fun onCancelled() {
                        finish()
                    }
                })
        }

        viewModelAccountDetails.transferListLiveData.observe(this) {
            viewModelAccountDetails.checkForEncryptedAmounts()
        }

        viewModelAccountDetails.showPadLockLiveData.observe(this) {
            invalidateOptionsMenu()
        }

        viewModelAccountDetails.shieldingEnabledLiveData.observe(this) {
            //Show non-shielded options
            viewModelAccountDetails.isShielded = false
            initViews()
            //...then hide shielding options
            updateShieldEnabledUI()
        }

        viewModelAccountDetails.accountUpdatedLiveData.observe(this) {
            initTopContent()
            updateShieldEnabledUI()
        }
    }

    private fun initViews() {
        showWaitingTransactions(false)
        initTabs()
        if (!viewModelAccountDetails.isShielded)
            initTabsTokens()
    }

    private fun initTabsTokens() {
        binding.tabFungible.setOnClickListener {
            onTabFungible()
        }
        binding.tabCollectibles.setOnClickListener {
            onTabCollectibles()
        }
        binding.tabAddNew.setOnClickListener {
            showFindTokensDialog()
        }
    }

    private fun onTabFungible() {
        selectedTabIndex = 0
        binding.markerFungible.visibility = View.VISIBLE
        binding.markerCollectibles.visibility = View.GONE
        binding.tabFungibleText.setTypeface(binding.tabFungibleText.typeface, Typeface.BOLD)
        binding.tabCollectiblesText.setTypeface(
            binding.tabCollectiblesText.typeface,
            Typeface.NORMAL
        )
        supportFragmentManager.beginTransaction().replace(
            R.id.tokens_fragment,
            TokensFragment.newInstance(
                viewModelTokens,
                viewModelAccountDetails.account.address,
                true
            ),
            null
        ).commit()
    }

    private fun onTabCollectibles() {
        selectedTabIndex = 1
        binding.markerFungible.visibility = View.GONE
        binding.markerCollectibles.visibility = View.VISIBLE
        binding.tabFungibleText.setTypeface(binding.tabFungibleText.typeface, Typeface.NORMAL)
        binding.tabCollectiblesText.setTypeface(
            binding.tabCollectiblesText.typeface,
            Typeface.BOLD
        )
        supportFragmentManager.beginTransaction().replace(
            R.id.tokens_fragment,
            TokensFragment.newInstance(
                viewModelTokens,
                viewModelAccountDetails.account.address,
                false
            ),
            null
        ).commit()
    }

    private fun initTopContent() {
        setActionBarTitle(
            getString(
                if (viewModelAccountDetails.isShielded) R.string.account_details_title_shielded_balance else R.string.account_details_title_regular_balance,
                viewModelAccountDetails.account.getAccountName()
            )
        )
        when (viewModelAccountDetails.account.transactionStatus) {
            TransactionStatus.ABSENT -> setErrorMode()
            TransactionStatus.FINALIZED -> setFinalizedMode()
            TransactionStatus.COMMITTED -> setPendingMode()
            TransactionStatus.RECEIVED -> setPendingMode()
            else -> {}
        }
        binding.accountRetryButton.setOnClickListener {
            setResult(RESULT_RETRY_ACCOUNT_CREATION)
            finish()
        }
        binding.accountRemoveButton.setOnClickListener {
            viewModelAccountDetails.deleteAccountAndFinish()
        }
        binding.toggleBalance.setOnClickListener {
            viewModelAccountDetails.isShielded = false
            initViews()
        }
        binding.toggleShielded.setOnClickListener {
            viewModelAccountDetails.isShielded = true
            initViews()
            showTransactionsView()
        }
        binding.accountTotalDetailsDisposalText.text =
            if (viewModelAccountDetails.isShielded) resources.getString(
                R.string.account_shielded_total_details_disposal,
                viewModelAccountDetails.account.name
            ) else resources.getString(R.string.account_total_details_disposal)
    }

    private fun showFindTokensDialog() {
        lookForNewTokensFragment = LookForNewTokensFragment.newInstance(viewModelTokens)
        lookForNewTokensFragment?.show(supportFragmentManager, "")
    }

    private fun showTokenDetailsDialog(token: Token) {
        val intent = Intent(this, TokenDetailsActivity::class.java)
        intent.putExtra(TokenDetailsActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(TokenDetailsActivity.TOKEN, token)
        showTokenDetails.launch(intent)
    }

    private val showTokenDetails =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getBooleanExtra(TokenDetailsActivity.DELETED, false)?.let { isDeleted ->
                    if (isDeleted) {
                        viewModelTokens.updateWithSelectedTokensDone.postValue(true)
                    }
                }
            }
        }

    private fun updateShieldEnabledUI() {
        binding.toggleContainer.visibility =
            if (viewModelAccountDetails.shieldingEnabledLiveData.value == true) View.VISIBLE else View.GONE
        binding.shieldedIcon.visibility =
            if (viewModelAccountDetails.shieldingEnabledLiveData.value == true && viewModelAccountDetails.isShielded) View.VISIBLE else View.GONE
        binding.markerBalance.visibility =
            if (viewModelAccountDetails.isShielded) View.GONE else View.VISIBLE
        binding.markerShielded.visibility =
            if (viewModelAccountDetails.isShielded) View.VISIBLE else View.GONE
        updateButtonsSlider()
    }

    private fun setFinalizedMode() {
        binding.buttonsSlider.setEnableButtons(!viewModelAccountDetails.account.readOnly)
        binding.accountDetailsLayout.visibility = View.VISIBLE
        binding.accountsOverviewTotalDetailsBakerContainer.visibility = View.GONE
        binding.accountsOverviewTotalDetailsStakedContainer.visibility = View.GONE
        if (viewModelAccountDetails.isShielded) {
            binding.accountsOverviewTotalDetailsDisposalContainer.visibility = View.GONE
        } else {
            binding.accountsOverviewTotalDetailsDisposalContainer.visibility = View.VISIBLE
            if (viewModelAccountDetails.account.isBaking()) {
                binding.accountsOverviewTotalDetailsBakerContainer.visibility = View.VISIBLE
                binding.accountsOverviewTotalTitleBaker.text = getString(
                    R.string.account_details_stake_with_baker,
                    viewModelAccountDetails.account.accountBaker?.bakerId?.toString() ?: ""
                )
                binding.accountsOverviewTotalDetailsBaker.text =
                    CurrencyUtil.formatGTU(
                        viewModelAccountDetails.account.accountBaker?.stakedAmount
                            ?: BigInteger.ZERO,
                        true
                    )
            } else if (viewModelAccountDetails.account.isDelegating()) {
                binding.accountsOverviewTotalDetailsStakedContainer.visibility = View.VISIBLE
                if (viewModelAccountDetails.account.accountDelegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_L_POOL)
                    binding.accountsOverviewTotalTitleStaked.text =
                        getString(R.string.account_details_delegation_with_passive_pool)
                else
                    binding.accountsOverviewTotalTitleStaked.text = getString(
                        R.string.account_details_delegation_with_baker_pool,
                        viewModelAccountDetails.account.accountDelegation?.delegationTarget?.bakerId
                            ?: ""
                    )
                binding.accountsOverviewTotalDetailsStaked.text =
                    CurrencyUtil.formatGTU(
                        viewModelAccountDetails.account.accountDelegation?.stakedAmount
                            ?: BigInteger.ZERO,
                        true
                    )
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
        val adapter = AccountDetailsPagerAdapter(this, viewModelAccountDetails.account, this)
        binding.accountDetailsPager.adapter = adapter
    }

    private fun showWaitingTransactions(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun showWaitingTokens(waiting: Boolean) {
        binding.includeProgressTokens.progressLayout.visibility =
            if (waiting) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return true
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.balanceTextview.text = CurrencyUtil.formatGTU(totalBalance, true)
        binding.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(
            viewModelAccountDetails.account.getAtDisposalWithoutStakedOrScheduled(totalBalance),
            true
        )
    }

    private fun onSendClicked() {
        val intent = Intent(this, SendTokenActivity::class.java)
        intent.putExtra(SendTokenActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(
            SendTokenActivity.TOKEN,
            TokenUtil.getCCDToken(viewModelAccountDetails.account)
        )
        startActivity(intent)
    }

    private fun onAddressClicked() {
        val intent = Intent(this, AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
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
                    intent.putExtra(WalletConnectActivity.ACCOUNT, viewModelAccountDetails.account)
                    startActivity(intent)
                }
            }
        }

    private fun updateButtonsSlider() {
        if (viewModelAccountDetails.isShielded) {
            binding.buttonsSlider.visibility = View.GONE
            // Completely hide and disable shielding actions.
            return
        }
        binding.buttonsSlider.visibility = View.VISIBLE
        binding.buttonsShielded.visibility = View.GONE
        binding.buttonsSlider.removeAllButtons()
        binding.buttonsSlider.addButton(R.drawable.ic_tokens, "2", setToWhite = false) {
            binding.buttonsSlider.setMarkerOn("2")
            showTokensView()
        }
        binding.buttonsSlider.addButton(R.drawable.ic_send, "3") {
            onSendClicked()
        }
        binding.buttonsSlider.addButton(R.drawable.ic_list, "4") {
            binding.buttonsSlider.setMarkerOn("4")
            showTransactionsView()
        }
        binding.buttonsSlider.addButton(R.drawable.ic_recipient_address_qr, "5") {
            onAddressClicked()
        }
        if ((viewModelAccountDetails.shieldingEnabledLiveData.value == true && !viewModelAccountDetails.isShielded) || viewModelAccountDetails.shieldingEnabledLiveData.value == false) {
            binding.buttonsSlider.addButton(R.drawable.ic_earn, "6") {
                gotoEarn(
                    this,
                    viewModelAccountDetails.account,
                    viewModelAccountDetails.hasPendingDelegationTransactions,
                    viewModelAccountDetails.hasPendingBakingTransactions
                )
            }
        }
        binding.buttonsSlider.addButton(R.drawable.ic_scan, "7") {
            scan()
        }
        binding.buttonsSlider.addButton(R.drawable.ic_settings, "10") {
            gotoAccountSettings(false)
        }
        binding.buttonsSlider.commitButtons()
        if (binding.accountDetailsPager.visibility == View.VISIBLE)
            binding.buttonsSlider.setMarkerOn("4")
        if (binding.tokens.visibility == View.VISIBLE)
            binding.buttonsSlider.setMarkerOn("2")
    }

    private fun gotoAccountSettings(continueToShieldIntro: Boolean) {
        val intent = Intent(this, AccountSettingsActivity::class.java)
        intent.putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(AccountSettingsActivity.EXTRA_SHIELDED, viewModelAccountDetails.isShielded)
        if (continueToShieldIntro)
            intent.putExtra(
                AccountSettingsActivity.EXTRA_CONTINUE_TO_SHIELD_INTRO,
                viewModelAccountDetails.isShielded
            )
        startActivity(intent)
    }

    private fun showTransactionsView() {
        binding.accountDetailsPager.visibility = View.VISIBLE
        binding.tokens.visibility = View.GONE
    }

    private fun showTokensView() {
        binding.accountDetailsPager.visibility = View.GONE
        binding.tokens.visibility = View.VISIBLE
    }
}
