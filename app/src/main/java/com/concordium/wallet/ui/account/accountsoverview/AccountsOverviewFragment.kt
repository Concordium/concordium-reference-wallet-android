package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import com.concordium.wallet.util.Log
import kotlinx.android.synthetic.main.fragment_accounts_overview.*
import kotlinx.android.synthetic.main.fragment_accounts_overview.view.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.progress.view.*

class AccountsOverviewFragment : BaseFragment() {

    companion object {
        private const val REQUESTCODE_ACCOUNT_DETAILS = 2000
    }

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

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
        viewModel.initiateFrequentUpdater()
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

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(AccountsOverviewViewModel::class.java)
        mainViewModel = ViewModelProvider(
            activity!!,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
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
            }
        })

        viewModel.identityLiveData.observe(this, Observer { state ->
            activity?.invalidateOptionsMenu()
        })

        viewModel.identityListLiveData.observe(this, Observer { identityList ->
            viewModel.updateState()
            viewModel.initiateFrequentUpdater()
        })

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

        initializeList(view)
    }

    private fun initializeList(view: View) {
        accountAdapter = AccountAdapter()
        view.account_recyclerview.setHasFixedSize(true)
        view.account_recyclerview.adapter = accountAdapter

        accountAdapter.setOnItemClickListener(object : AccountItemView.OnItemClickListener {
            override fun onRegularBalanceClicked(item: Account) {
                gotoAccountDetails(item, false)
            }
            override fun onShieldedBalanceClicked(item: Account) {
                gotoAccountDetails(item, true)
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

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
        total_balance_shielded_container.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
    }
    private fun showDisposalBalance(atDisposal: Long, containsEncryptedAmount: Boolean) {
        accounts_overview_total_details_disposal.text = CurrencyUtil.formatGTU(atDisposal, true)
        total_balance_shielded_container.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
        accounts_overview_total_details_disposal_shield.visibility = if(containsEncryptedAmount) View.VISIBLE else View.GONE
    }
    private fun showStakedBalance(totalBalance: Long) {
        accounts_overview_total_details_staked.text = CurrencyUtil.formatGTU(totalBalance, true)
    }


    //endregion

}