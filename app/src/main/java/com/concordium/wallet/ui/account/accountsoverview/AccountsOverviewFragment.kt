package com.concordium.wallet.ui.account.accountsoverview

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.data.model.BakerStakePendingChange
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.util.TokenUtil
import java.math.BigInteger

class AccountsOverviewFragment : BaseFragment(),
    IdentityStatusDelegate by IdentityStatusDelegateImpl(), EarnDelegate by EarnDelegateImpl() {
    private var _binding: FragmentAccountsOverviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val REQUESTCODE_ACCOUNT_DETAILS = 2000
    }

    private var encryptedWarningDialog: AlertDialog? = null
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
    }

    override fun onResume() {
        super.onResume()
        //Make sure when returning the dialog is cleared, else it will popup when returning
        encryptedWarningDialog?.dismiss()
        encryptedWarningDialog = null

        viewModel.updateState()
        viewModel.initiateFrequentUpdater()

        if (!App.appCore.appSettingsForceUpdateChecked)
            viewModel.loadAppSettings()

        startCheckForPendingIdentity(activity, null, false) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        stopCheckForPendingIdentity()
        viewModel.stopFrequentUpdater()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUESTCODE_ACCOUNT_DETAILS) {
            if (resultCode == AccountDetailsActivity.RESULT_RETRY_ACCOUNT_CREATION) {
                gotoCreateAccount()
            }
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
        viewModel.newFinalizedAccountLiveData.observe(this) { newAccount ->
            newAccount?.let {
                context?.let { }
            }
        }
        viewModel.newFinalizedAccountLiveData.observe(this) { newAccount ->
            newAccount?.let {
                context?.let { }
            }
        }
        viewModel.stateLiveData.observe(this) { state ->
            when (state) {
                AccountsOverviewViewModel.State.NO_IDENTITIES -> showStateNoIdentities()
                AccountsOverviewViewModel.State.NO_ACCOUNTS -> showStateNoAccounts()
                AccountsOverviewViewModel.State.DEFAULT -> showStateDefault()
                else -> {
                }
            }
        }
        viewModel.totalBalanceLiveData.observe(this) { totalBalance ->
            showTotalBalance(totalBalance.totalBalanceForAllAccounts)
            showDisposalBalance(totalBalance.totalAtDisposalForAllAccounts)
            showStakedBalance(totalBalance.totalStakedForAllAccounts)
        }
        viewModel.accountListLiveData.observe(this) { accountList ->
            accountList?.let {
                accountAdapter.setData(it)
                checkForClosingPools(it)
            }
        }
        viewModel.identityLiveData.observe(this) {
            activity?.invalidateOptionsMenu()
        }
        viewModel.identityListLiveData.observe(this) {
            viewModel.updateState()
            viewModel.initiateFrequentUpdater()
        }
        viewModel.pendingIdentityForWarningLiveData.observe(this) {
            updateWarnings()
        }
        viewModel.appSettingsLiveData.observe(this) { appSettings ->
            checkAppSettings(appSettings)
        }
        viewModel.localTransfersLoaded.observe(this) { account ->
            activity?.let {
                gotoEarn(
                    it,
                    account,
                    viewModel.hasPendingDelegationTransactions,
                    viewModel.hasPendingBakingTransactions
                )
            }
        }
    }

    private fun checkAppSettings(appSettings: AppSettings?) {
        appSettings?.let {
            when (appSettings.status) {
                AppSettings.APP_VERSION_STATUS_WARNING -> it.url?.let { url ->
                    showAppUpdateWarning(
                        url
                    )
                }

                AppSettings.APP_VERSION_STATUS_NEEDS_UPDATE -> it.url?.let { url ->
                    showAppUpdateNeedsUpdate(
                        url
                    )
                }

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
        builder.setCancelable(false)
        forceUpdateDialog = builder.create()
        forceUpdateDialog?.show()
    }

    private fun gotoAppStore(url: String) {
        if (url.isNotBlank())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        Process.killProcess(Process.myPid())
    }

    private fun checkForClosingPools(accountList: List<AccountWithIdentity>) {
        if (App.appCore.closingPoolsChecked)
            return

        App.appCore.closingPoolsChecked =
            true // avoid calling this more than once for each app cold launch

        val poolIds = accountList.mapNotNull { accountWithIdentity ->
            accountWithIdentity.account.accountDelegation?.delegationTarget?.bakerId?.toString()
        }.distinct()

        viewModel.poolStatusesLiveData.observe(this) { poolStatuses ->
            if (poolStatuses.isNotEmpty()) {
                var affectedAccounts = ""
                poolStatuses.forEach { poolStatus ->
                    if (poolStatus.second == BakerStakePendingChange.CHANGE_REMOVE_POOL) {
                        val accountNames =
                            accountList.filter { it.account.accountDelegation?.delegationTarget?.bakerId?.toString() == poolStatus.first }
                                .map { it.account.name }
                        accountNames.forEach { accountName ->
                            if (accountName.isNotEmpty())
                                affectedAccounts = affectedAccounts.plus("\n").plus(accountName)
                        }
                    }
                }
                if (affectedAccounts.isNotEmpty()) {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(R.string.accounts_overview_closing_pools_notice_title)
                    builder.setMessage(
                        getString(R.string.accounts_overview_closing_pools_notice_message).plus(
                            affectedAccounts
                        )
                    )
                    builder.setPositiveButton(getString(R.string.accounts_overview_closing_pools_notice_ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.create().show()
                }
            }
        }

        viewModel.loadPoolStatuses(poolIds)
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

        eventListener = object : Preferences.Listener {
            override fun onChange() {
                if (isInLayout && isVisible && !isDetached) {
                    updateWarnings()
                }
            }
        }

        initializeList()
    }

    private fun updateWarnings() {
        val identity = viewModel.pendingIdentityForWarningLiveData.value
        if (identity != null && !App.appCore.session.isIdentityPendingWarningAcknowledged(identity.id)) {
            binding.identityPending.visibility = View.VISIBLE
            viewModel.pendingIdentityForWarningLiveData.value
            binding.identityPendingTv.text =
                getString(R.string.accounts_overview_identity_pending_warning, identity.name)
            binding.identityPendingClose.setOnClickListener {
                App.appCore.session.setIdentityPendingWarningAcknowledged(identity.id)
                updateWarnings()
            }
            binding.identityPending.setOnClickListener {
                startActivity(Intent(activity, IdentitiesOverviewActivity::class.java))
            }
        } else {
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

            override fun onEarnClicked(item: Account) {
                viewModel.loadLocalTransfers(item)
            }

            override fun onReceiveClicked(item: Account) {
                val intent = Intent(activity, AccountQRCodeActivity::class.java)
                intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, item)
                startActivity(intent)
            }

            override fun onSendClicked(item: Account) {
                val intent = Intent(activity, SendTokenActivity::class.java)
                intent.putExtra(SendTokenActivity.ACCOUNT, item)
                intent.putExtra(SendTokenActivity.TOKEN, TokenUtil.getCCDToken(item))
                startActivity(intent)
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoCreateIdentity() {
        startActivity(Intent(activity, IdentityProviderListActivity::class.java))
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

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.totalBalanceTextview.text = CurrencyUtil.formatGTU(totalBalance)
    }

    private fun showDisposalBalance(atDisposal: BigInteger) {
        binding.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(atDisposal, true)
    }

    private fun showStakedBalance(totalBalance: BigInteger) {
        binding.accountsOverviewTotalDetailsStaked.text = CurrencyUtil.formatGTU(totalBalance, true)
    }

    //endregion
}
