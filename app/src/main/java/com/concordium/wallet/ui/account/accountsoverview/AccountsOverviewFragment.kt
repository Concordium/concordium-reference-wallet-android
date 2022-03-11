package com.concordium.wallet.ui.account.accountsoverview

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountdetails.ShieldingIntroActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import kotlinx.android.synthetic.main.activity_account_details.*
import kotlinx.android.synthetic.main.fragment_accounts_overview.*
import kotlinx.android.synthetic.main.fragment_accounts_overview.accounts_overview_total_details_disposal
import kotlinx.android.synthetic.main.fragment_accounts_overview.accounts_overview_total_details_staked
import kotlinx.android.synthetic.main.fragment_accounts_overview.root_layout
import kotlinx.android.synthetic.main.fragment_accounts_overview.view.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.progress.view.*

class AccountsOverviewFragment : BaseFragment() {

    companion object {
        private const val REQUESTCODE_ACCOUNT_DETAILS = 2000
    }

    interface AccountsOverviewFragmentListener {
        fun identityClicked(identity: Identity)
    }

    private var encryptedWarningDialog: AlertDialog? = null
    private var fragmentListener: AccountsOverviewFragmentListener? = null

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
        viewModel.initialize()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_accounts_overview, container, false)
        initializeViews(rootView)
        return rootView
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
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let {
            App.appCore.session.removeAccountsBackedUpListener(it)
        }

    }

    override fun onPause() {
        super.onPause()
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
        ).get(AccountsOverviewViewModel::class.java)
        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(MainViewModel::class.java)

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
            showTotalBalance(totalBalance.totalBalanceForAllAccounts, totalBalance.totalContainsEncrypted)
            showDisposalBalance(totalBalance.totalAtDisposalForAllAccounts, totalBalance.totalContainsEncrypted)
            showStakedBalance(totalBalance.totalStakedForAllAccounts)
        })
        viewModel.accountListLiveData.observe(this, Observer { accountList ->
            accountList?.let {
                accountAdapter.setData(it)
                checkForUnencrypted(it)
            }
        })

        viewModel.identityLiveData.observe(this, Observer { state ->
            activity?.invalidateOptionsMenu()
        })

        viewModel.identityListLiveData.observe(this, Observer { identityList ->
            viewModel.updateState()
            viewModel.initiateFrequentUpdater()
        })

        viewModel.pendingIdentityForWarningLiveData.observe(this, Observer { identity ->
            updateWarnings()
        })

    }

    private fun checkForUnencrypted(accountList: List<AccountWithIdentity>) {
        accountList?.forEach {
            if(it.account.encryptedBalanceStatus != ShieldedAccountEncryptionStatus.DECRYPTED
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

    private fun initializeViews(view: View) {
        mainViewModel.setTitle(getString(R.string.accounts_overview_title))
        view.progress_layout.visibility = View.VISIBLE
        view.no_identities_layout.visibility = View.GONE
        view.no_accounts_textview.visibility = View.GONE
        view.create_account_button.visibility = View.GONE

        view.create_identity_button.setOnClickListener {
            gotoCreateIdentity()
        }
        view.create_account_button.setOnClickListener {
            gotoCreateAccount()
        }

        view.missing_backup.setOnClickListener {
            gotoExport()
        }

        eventListener = object : Preferences.Listener {
            override fun onChange() {
                if(!isDetached){
                    updateWarnings()
                }
            }
        }

        eventListener?.let {
            App.appCore.session.addAccountsBackedUpListener(it)
        }

        initializeList(view)

        context?.let {
            if(App.appCore.session.shouldPromptForBackedUp(it)){
                CustomDialogFragment.showAppUpdateBackupWarningDialog(it)
                updateWarnings()
            }
        }


    }

    private fun updateWarnings(){
        var ident = viewModel.pendingIdentityForWarningLiveData.value
        if(ident != null && !App.appCore.session.isIdentityPendingWarningAcknowledged(ident.id)){
            missing_backup.visibility = View.GONE
            identity_pending.visibility = View.VISIBLE
            viewModel.pendingIdentityForWarningLiveData.value
            identity_pending_tv.text = getString(R.string.accounts_overview_identity_pending_warning, ident.name)
            identity_pending_close.setOnClickListener {
                App.appCore.session.setIdentityPendingWarningAcknowledged(ident.id)
                updateWarnings()
            }
            identity_pending.setOnClickListener {
                fragmentListener?.identityClicked(ident)
            }
        }
        else{
            missing_backup.visibility = if(App.appCore.session.isAccountsBackedUp()) View.GONE else View.VISIBLE
            identity_pending.visibility = View.GONE
        }

    }

    private fun initializeList(view: View) {
        accountAdapter = AccountAdapter()
        view.account_recyclerview.setHasFixedSize(true)
        view.account_recyclerview.adapter = accountAdapter

        accountAdapter.setOnItemClickListener(object : AccountItemView.OnItemClickListener {

            override fun onMoreClicked(account: Account) {
                gotoAccountDetails(account, false)
            }

            override fun onReceiveClicked(account: Account) {
                val intent = Intent(activity, AccountQRCodeActivity::class.java)
                intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, account)
                startActivity(intent)
            }

            override fun onSendClicked(account: Account) {
                val intent = Intent(activity, SendFundsActivity::class.java)
                intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, false)
                intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, account)
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
        val intent = Intent(activity, NewAccountNameActivity::class.java)
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
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun showStateNoIdentities() {
        activity?.invalidateOptionsMenu()
        no_accounts_scrollview.visibility = View.VISIBLE
        showNoIdentities(true)
        showNoAccounts(false)
    }

    private fun showStateNoAccounts() {
        activity?.invalidateOptionsMenu()
        no_accounts_scrollview.visibility = View.VISIBLE
        showNoIdentities(false)
        showNoAccounts(true)
    }

    private fun showStateDefault() {
        activity?.invalidateOptionsMenu()
        no_accounts_scrollview.visibility = View.GONE
        showNoIdentities(false)
        showNoAccounts(false)
    }

    private fun showNoIdentities(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        no_identities_layout.visibility = state
    }

    private fun showNoAccounts(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        no_accounts_textview.visibility = state
        create_account_button.visibility = state
    }

    private fun showTotalBalance(totalBalance: Long, containsEncryptedAmount: Boolean) {
        total_balance_textview.text = CurrencyUtil.formatGTU(totalBalance)
        //total_balance_shielded_container.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
    }
    private fun showDisposalBalance(atDisposal: Long, containsEncryptedAmount: Boolean) {
        accounts_overview_total_details_disposal.text = CurrencyUtil.formatGTU(atDisposal, true)
        //total_balance_shielded_container.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
        //accounts_overview_total_details_disposal_shield.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
    }
    private fun showStakedBalance(totalBalance: Long) {
        accounts_overview_total_details_staked.text = CurrencyUtil.formatGTU(totalBalance, true)
    }


    //endregion

}