package com.concordium.wallet.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewFragment
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.intro.introstart.IntroStartActivity
import com.concordium.wallet.ui.intro.introstart.IntroTermsActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.moreoverview.MoreOverviewFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(R.layout.activity_main, R.string.main_title), Dialogs.DialogFragmentListener {

    companion object {
        const val EXTRA_SHOW_IDENTITIES = "EXTRA_SHOW_IDENTITIES"
    }

    private lateinit var viewModel: MainViewModel
    private var hasHandledPossibleImportFile = false

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)  // Set theme to default to remove launcher theme
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()

        initializeViews()

        // If we're being restored from a previous state,
        // then we don't want to add fragments and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return
        }
    }

    override fun onResume() {
        super.onResume()

        if(!viewModel.databaseVersionAllowed){
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.error_database))
            builder.setPositiveButton(getString(R.string.error_database_close), object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which:Int) {
                    finish()
                }
            })
            builder.setCancelable(false)
            builder.create().show()
        }
        else{
            if (viewModel.shouldShowAuthentication()) {
                showAuthenticationIfRequired()
            } else {
                viewModel.setInitialStateIfNotSet()

                if (!hasHandledPossibleImportFile) {
                    hasHandledPossibleImportFile = true
                    val handlingImportFile = handlePossibleImportFile()
                    if(handlingImportFile){
                        // Do not start identity update, since we are leaving this page
                        return
                    }
                }

                viewModel.startIdentityUpdate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopIdentityUpdate()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getBooleanExtra(EXTRA_SHOW_IDENTITIES, false)?.let { showIdentities ->
            if (showIdentities) {
                viewModel.setState(MainViewModel.State.IdentityOverview)
                bottom_navigation_view.menu.findItem(R.id.menuitem_identities)?.let { menuItem ->
                    menuItem.isChecked = true
                }
            }
        }
        // MainActivity has launchMode singleTask to not start a new instance (if already running),
        // when selecting an import file to launch the app. In this case onNewIntent will be called.
        intent?.data?.let { _ ->
            // Save this new intent to handle it in onResume (in case of an import file)
            this.intent = intent
            hasHandledPossibleImportFile = false
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                gotoIdentityProviderList()
            }
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(MainViewModel::class.java)

        viewModel.titleLiveData.observe(this, Observer<String> { title ->
            title?.let {
                val actionbar = supportActionBar ?: return@Observer
                actionbar.setTitle(title)
            }
        })
        viewModel.stateLiveData.observe(this, Observer<MainViewModel.State> { state ->
            state?.let {
                replaceFragment(state)
            }
        })

        viewModel.identityErrorLiveData.observe(this, Observer<IdentityErrorData> { data ->
            data?.let {
                IdentityErrorDialogHelper.showIdentityError(this, dialogs, data)
            }
        })
    }

    private fun initializeViews() {
        bottom_navigation_view.setOnNavigationItemSelectedListener {
            onNavigationItemSelected(it)
        }
        hideActionBarBack(this)
    }

    //endregion

    //region Menu Navigation
    //************************************************************

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true

        val state = getState(menuItem)
        if (state != null) {
            viewModel.setState(state)
            return true
        }
        return false
    }

    private fun getState(menuItem: MenuItem): MainViewModel.State? {
        return when (menuItem.itemId) {
            R.id.menuitem_accounts -> MainViewModel.State.AccountOverview
            R.id.menuitem_identities -> MainViewModel.State.IdentityOverview
            R.id.menuitem_more -> MainViewModel.State.More
            else -> null
        }
    }

    private fun replaceFragment(state: MainViewModel.State) {
        val fragment = when (state) {
            MainViewModel.State.AccountOverview -> AccountsOverviewFragment()
            MainViewModel.State.IdentityOverview -> IdentitiesOverviewFragment()
            MainViewModel.State.More -> MoreOverviewFragment()
        }
        replaceFragment(fragment, false)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showAuthenticationIfRequired() {
        if (viewModel.shouldShowUserSetup()) {
            val intent = Intent(this, IntroTermsActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, AuthLoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun loggedOut() {
    }

    private fun handlePossibleImportFile(): Boolean {
        val uri = intent?.data
        if (uri != null) {
            val intent = Intent(this, ImportActivity::class.java)
            intent.putExtra(ImportActivity.EXTRA_FILE_URI, uri)
            startActivity(intent)
            return true
        }
        return false
    }

    private fun gotoIdentityProviderList() {
        viewModel.identityErrorLiveData.value?.let { data ->
            val intent = Intent(this, IdentityProviderListActivity::class.java)
            intent.putExtra(IdentityProviderListActivity.EXTRA_IDENTITY_CUSTOM_NAME, data.identity.name)
            intent.putExtra(IdentityProviderListActivity.EXTRA_ACCOUNT_CUSTOM_NAME, data.account?.name ?: data.identity.name)
            startActivity(intent)
        }
    }



    //endregion
}
