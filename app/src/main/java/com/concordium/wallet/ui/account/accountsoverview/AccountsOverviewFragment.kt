package com.concordium.wallet.ui.account.accountsoverview

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.data.model.BakerStakePendingChange
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.uicore.dialog.CustomDialogFragment

class AccountsOverviewFragment : BaseFragment(), IdentityStatusDelegate by IdentityStatusDelegateImpl() {
    private var _binding: FragmentAccountsOverviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val REQUESTCODE_ACCOUNT_DETAILS = 2000
    }

    interface AccountsOverviewFragmentListener {
        fun identityClicked(identity: Identity)
    }

    private var encryptedWarningDialog: AlertDialog? = null
    private var fragmentListener: AccountsOverviewFragmentListener? = null
    private var forceUpdateDialog: AlertDialog? = null
    private var eventListener: Preferences.Listener? = null

    private lateinit var viewModel: AccountsOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var accountAdapter: AccountAdapter

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        initializeViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsOverviewBinding.inflate(inflater, container, false)
        initializeViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateWarnings()

        context?.let {
            if(App.appCore.session.shouldPromptForBackedUp(it)){
                if(isInLayout && isVisible && !isDetached){
                    CustomDialogFragment.showAppUpdateBackupWarningDialog(it)
                    updateWarnings()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //Make sure when returning the dialog is cleared, else it will popup when returning
        encryptedWarningDialog?.dismiss()
        encryptedWarningDialog = null

        viewModel.updateState()
        viewModel.initiateFrequentUpdater()

        eventListener?.let {
            App.appCore.session.addAccountsBackedUpListener(it)
        }

        if (!App.appCore.appSettingsForceUpdateChecked)
            viewModel.loadAppSettings()

        startCheckForPendingIdentity(activity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let {
            App.appCore.session.removeAccountsBackedUpListener(it)
        }
    }

    override fun onPause() {
        super.onPause()
        stopCheckForPendingIdentity()
        viewModel.stopFrequentUpdater()
        eventListener?.let {
            App.appCore.session.removeAccountsBackedUpListener(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUESTCODE_ACCOUNT_DETAILS) {
            if (resultCode == AccountDetailsActivity.RESULT_RETRY_ACCOUNT_CREATION) {
                gotoCreateAccount()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        viewModel.identityLiveData.value?.let { state ->
            if (state == AccountsOverviewViewModel.State.VALID_IDENTITIES) {
                inflater.inflate(R.menu.add_item_menu, menu)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_item_menu -> gotoCreateAccount()
        }
        return true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            fragmentListener = context as AccountsOverviewFragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement AccountsOverviewFragmentListener")
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountsOverviewViewModel::class.java]
        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MainViewModel::class.java]
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
        viewModel.newFinalizedAccountLiveData.observe(this, Observer<String> { newAccount ->
            newAccount?.let {
                context?.let { CustomDialogFragment.newAccountFinalizedDialog(it, newAccount) }
            }
        })
        viewModel.newFinalizedAccountLiveData.observe(this, Observer<String> { newAccount ->
            newAccount?.let {
                context?.let { CustomDialogFragment.newAccountFinalizedDialog(it, newAccount) }
            }
        })
        viewModel.stateLiveData.observe(this, Observer { state ->
            when (state) {
                AccountsOverviewViewModel.State.NO_IDENTITIES -> showStateNoIdentities()
                AccountsOverviewViewModel.State.NO_ACCOUNTS -> showStateNoAccounts()
                AccountsOverviewViewModel.State.DEFAULT -> showStateDefault()
                else -> {
                }
            }
        })
        viewModel.totalBalanceLiveData.observe(this, Observer<TotalBalancesData> { totalBalance ->
            showTotalBalance(totalBalance.totalBalanceForAllAccounts)
            showDisposalBalance(totalBalance.totalAtDisposalForAllAccounts)
            showStakedBalance(totalBalance.totalStakedForAllAccounts)
        })
        viewModel.accountListLiveData.observe(this, Observer { accountList ->
            accountList?.let {
                accountAdapter.setData(it)
                checkForUnencrypted(it)
                checkForClosingPools(it)
            }
        })
        viewModel.identityLiveData.observe(this, Observer {
            activity?.invalidateOptionsMenu()
        })
        viewModel.identityListLiveData.observe(this, Observer {
            viewModel.updateState()
            viewModel.initiateFrequentUpdater()
        })
        viewModel.pendingIdentityForWarningLiveData.observe(this, Observer {
            updateWarnings()
        })
        viewModel.appSettingsLiveData.observe(this, Observer { appSettings ->
            checkAppSettings(appSettings)
        })
    }

    private fun checkAppSettings(appSettings: AppSettings?) {
        appSettings?.let {
            when (appSettings.status) {
                AppSettings.APP_VERSION_STATUS_WARNING -> it.url?.let { url -> showAppUpdateWarning(url) }
                AppSettings.APP_VERSION_STATUS_NEEDS_UPDATE -> it.url?.let { url -> showAppUpdateNeedsUpdate(url) }
                else -> {}
            }
        }
    }

    private fun showAppUpdateWarning(url: String) {
        if (forceUpdateDialog != null)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.force_update_warning_title)
        builder.setMessage(getString(R.string.force_update_warning_message))
        builder.setPositiveButton(getString(R.string.force_update_warning_update_now)) { _, _ ->
            gotoAppStore(url)
        }
        builder.setNegativeButton(getString(R.string.force_update_warning_backup)) { _, _ -> gotoBackup() }
        builder.setNeutralButton(getString(R.string.force_update_warning_remind_me)) { dialog, _ ->
            App.appCore.appSettingsForceUpdateChecked = true
            dialog.dismiss()
        }
        builder.setCancelable(false)
        forceUpdateDialog = builder.create()
        forceUpdateDialog?.show()
    }

    private fun showAppUpdateNeedsUpdate(url: String) {
        if (forceUpdateDialog != null)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.force_update_needed_title)
        builder.setMessage(getString(R.string.force_update_needed_message))
        builder.setNeutralButton(getString(R.string.force_update_needed_update_now)) { _, _ ->
            gotoAppStore(url)
        }
        builder.setPositiveButton(getString(R.string.force_update_needed_backup)) { _, _ -> gotoBackup() }
        builder.setCancelable(false)
        forceUpdateDialog = builder.create()
        forceUpdateDialog?.show()
    }

    private fun gotoAppStore(url: String) {
        if (url.isNotBlank())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        Process.killProcess(Process.myPid())
    }

    private fun gotoBackup() {
        forceUpdateDialog?.dismiss()
        forceUpdateDialog = null
        startActivity(Intent(context, ExportActivity::class.java))
    }

    private fun checkForClosingPools(accountList: List<AccountWithIdentity>) {
        if (App.appCore.closingPoolsChecked)
            return

        App.appCore.closingPoolsChecked = true // avoid calling this more than once for each app cold launch

        val poolIds = accountList.mapNotNull { accountWithIdentity ->
            accountWithIdentity.account.accountDelegation?.delegationTarget?.bakerId?.toString()
        }.distinct()

        viewModel.poolStatusesLiveData.observe(this, Observer { poolStatuses ->
            if (poolStatuses.isNotEmpty()) {
                var affectedAccounts = ""
                poolStatuses.forEach { poolStatus ->
                    if (poolStatus.second == BakerStakePendingChange.CHANGE_REMOVE_POOL) {
                        val accountNames = accountList.filter { it.account.accountDelegation?.delegationTarget?.bakerId?.toString() == poolStatus.first }.map { it.account.name }
                        accountNames.forEach { accountName ->
                            if (accountName.isNotEmpty())
                                affectedAccounts = affectedAccounts.plus("\n").plus(accountName)
                        }
                    }
                }
                if (affectedAccounts.isNotEmpty()) {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(R.string.accounts_overview_closing_pools_notice_title)
                    builder.setMessage(getString(R.string.accounts_overview_closing_pools_notice_message).plus(affectedAccounts))
                    builder.setPositiveButton(getString(R.string.accounts_overview_closing_pools_notice_ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.create().show()
                }
            }
        })

        viewModel.loadPoolStatuses(poolIds)
    }

    private fun checkForUnencrypted(accountList: List<AccountWithIdentity>) {
        accountList.forEach {

            val hasUnencryptedTransactions = it.account.finalizedEncryptedBalance?.incomingAmounts?.isNotEmpty()
            if((hasUnencryptedTransactions != null && hasUnencryptedTransactions == true)
                && it.account.transactionStatus == TransactionStatus.FINALIZED
                && !App.appCore.session.isShieldedWarningDismissed(it.account.address)
                && !App.appCore.session.isShieldingEnabled(it.account.address)
                && encryptedWarningDialog == null){

                val builder = AlertDialog.Builder(context)
                builder.setTitle(getString(R.string.account_details_shielded_warning_title))
                builder.setMessage(getString(R.string.account_details_shielded_warning_text, it.account.name))
                builder.setNegativeButton(
                    getString(R.string.account_details_shielded_warning_enable, it.account.name),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            startShieldedIntroFlow(it.account)
                            encryptedWarningDialog?.dismiss()
                            encryptedWarningDialog = null
                        }
                    })
                builder.setPositiveButton(
                    getString(R.string.account_details_shielded_warning_dismiss),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            App.appCore.session.setShieldedWarningDismissed(
                                it.account.address,
                                true
                            )
                            encryptedWarningDialog?.dismiss()
                            encryptedWarningDialog = null
                            checkForUnencrypted(accountList) //Check for other accounts with shielded transactions
                        }
                    })
                builder.setCancelable(true)
                encryptedWarningDialog = builder.create()//.show()
                encryptedWarningDialog?.show()
            }
        }
    }

    private fun startShieldedIntroFlow(account: Account) {
        val intent = Intent(activity, AccountDetailsActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
        intent.putExtra(AccountDetailsActivity.EXTRA_SHIELDED, false)
        intent.putExtra(AccountDetailsActivity.EXTRA_CONTINUE_TO_SHIELD_INTRO, true)
        startActivityForResult(intent, REQUESTCODE_ACCOUNT_DETAILS)
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.accounts_overview_title))
        binding.includeProgress.progressLayout.visibility = View.VISIBLE
        binding.noIdentitiesLayout.visibility = View.GONE
        binding.noAccountsTextview.visibility = View.GONE
        binding.createAccountButton.visibility = View.GONE

        binding.createIdentityButton.setOnClickListener {
            gotoCreateIdentity()
        }
        binding.createAccountButton.setOnClickListener {
            gotoCreateAccount()
        }

        binding.missingBackup.setOnClickListener {
            gotoExport()
        }

        eventListener = object : Preferences.Listener {
            override fun onChange() {
                if(isInLayout && isVisible && !isDetached){
                    updateWarnings()
                }
            }
        }

        initializeList()
    }

    private fun updateWarnings(){
        val ident = viewModel.pendingIdentityForWarningLiveData.value
        if (ident != null && !App.appCore.session.isIdentityPendingWarningAcknowledged(ident.id)) {
            binding.missingBackup.visibility = View.GONE
            binding.identityPending.visibility = View.VISIBLE
            viewModel.pendingIdentityForWarningLiveData.value
            binding.identityPendingTv.text = getString(R.string.accounts_overview_identity_pending_warning, ident.name)
            binding.identityPendingClose.setOnClickListener {
                App.appCore.session.setIdentityPendingWarningAcknowledged(ident.id)
                updateWarnings()
            }
            binding.identityPending.setOnClickListener {
                fragmentListener?.identityClicked(ident)
            }
        }
        else{
            binding.missingBackup.visibility = if(App.appCore.session.isAccountsBackedUp()) View.GONE else View.VISIBLE
            binding.identityPending.visibility = View.GONE
        }

    }

    private fun initializeList() {
        accountAdapter = AccountAdapter()
        binding.accountRecyclerview.setHasFixedSize(true)
        binding.accountRecyclerview.adapter = accountAdapter

        accountAdapter.setOnItemClickListener(object : AccountItemView.OnItemClickListener {

            override fun onMoreClicked(item: Account) {
                gotoAccountDetails(item, false)
            }

            override fun onReceiveClicked(item: Account) {
                val intent = Intent(activity, AccountQRCodeActivity::class.java)
                intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, item)
                startActivity(intent)
            }

            override fun onSendClicked(item: Account) {
                val intent = Intent(activity, SendFundsActivity::class.java)
                intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, false)
                intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, item)
                startActivity(intent)
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun gotoCreateIdentity() {
        val intent = Intent(activity, IdentityCreateActivity::class.java)
        startActivity(intent)
    }

    private fun gotoCreateAccount() {
        val intent = Intent(activity, IdentitiesOverviewActivity::class.java)
        intent.putExtra(IdentitiesOverviewActivity.SHOW_FOR_CREATE_ACCOUNT, true)
        startActivity(intent)
    }

    private fun gotoAccountDetails(item: Account, isShielded: Boolean) {
        val intent = Intent(activity, AccountDetailsActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item)
        intent.putExtra(AccountDetailsActivity.EXTRA_SHIELDED, isShielded)
        startActivityForResult(intent, REQUESTCODE_ACCOUNT_DETAILS)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showStateNoIdentities() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsScrollview.visibility = View.VISIBLE
        showNoIdentities(true)
        showNoAccounts(false)
    }

    private fun showStateNoAccounts() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsScrollview.visibility = View.VISIBLE
        showNoIdentities(false)
        showNoAccounts(true)
    }

    private fun showStateDefault() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsScrollview.visibility = View.GONE
        showNoIdentities(false)
        showNoAccounts(false)
    }

    private fun showNoIdentities(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.noIdentitiesLayout.visibility = state
    }

    private fun showNoAccounts(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.noAccountsTextview.visibility = state
        binding.createAccountButton.visibility = state
    }

    private fun showTotalBalance(totalBalance: Long) {
        binding.totalBalanceTextview.text = CurrencyUtil.formatGTU(totalBalance)
    }
    private fun showDisposalBalance(atDisposal: Long) {
        binding.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(atDisposal, true)
    }
    private fun showStakedBalance(totalBalance: Long) {
        binding.accountsOverviewTotalDetailsStaked.text = CurrencyUtil.formatGTU(totalBalance, true)
    }

    //endregion
}
