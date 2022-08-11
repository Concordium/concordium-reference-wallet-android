package com.concordium.wallet.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewFragment
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.intro.introstart.IntroTermsActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.moreoverview.MoreOverviewFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : BaseActivity(), Dialogs.DialogFragmentListener, AccountsOverviewFragment.AccountsOverviewFragmentListener {
    companion object {
        const val EXTRA_SHOW_IDENTITIES = "EXTRA_SHOW_IDENTITIES"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var hasHandledPossibleImportFile = false

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)  // Set theme to default to remove launcher theme
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.main_title)

        initializeViewModel()
        viewModel.initialize()

        initializeViews()

        // If we're being restored from a previous state,
        // then we don't want to add fragments and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return
        }

        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.databaseVersionAllowed) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.error_database))
            builder.setPositiveButton(getString(R.string.error_database_close)) { _, _ -> finish() }
            builder.setCancelable(false)
            builder.create().show()
        }
        else {
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
                switchToIdentities()
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

    override fun onBackPressed() {
        if (viewModel.stateLiveData.value == MainViewModel.State.IdentitiesOverview)
            viewModel.setState(MainViewModel.State.AccountOverview)
        else
            super.onBackPressed()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(state: MainViewModel.State) {
        viewModel.setState(state)
    }

    private fun switchToIdentities() {
        viewModel.setState(MainViewModel.State.IdentitiesOverview)
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
        )[MainViewModel::class.java]

        viewModel.titleLiveData.observe(this) { title ->
            title?.let {
                setActionBarTitle(it)
            }
        }
        viewModel.stateLiveData.observe(this) { state ->
            state?.let {
                replaceFragment(state)
            }
        }
        viewModel.identityErrorLiveData.observe(this) { data ->
            data?.let {
                IdentityErrorDialogHelper.showIdentityError(this, dialogs, data)
            }
        }
        viewModel.newFinalizedAccountLiveData.observe(this) { newAccount ->
            newAccount?.let {
            }
        }
    }

    private fun initializeViews() {
        forceMenuSelection(R.id.menuitem_accounts)
        binding.bottomNavigationView.setOnItemSelectedListener {
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

    private fun forceMenuSelection(resId: Int) {
        GlobalScope.launch(Dispatchers.Main){
            delay(1)
            binding.bottomNavigationView.menu.findItem(resId).isChecked = true
        }
    }

    private fun getState(menuItem: MenuItem): MainViewModel.State? {
        return when (menuItem.itemId) {
            R.id.menuitem_accounts -> MainViewModel.State.AccountOverview
            R.id.menuitem_more -> MainViewModel.State.More
            else -> null
        }
    }

    private fun replaceFragment(state: MainViewModel.State) {
        hideActionBarBack(this)
        val fragment = when (state) {
            MainViewModel.State.AccountOverview -> AccountsOverviewFragment()
            MainViewModel.State.More -> MoreOverviewFragment()
            MainViewModel.State.IdentitiesOverview -> {
                setActionBarTitle(R.string.identities_overview_title)
                showActionBarBack(this)
                IdentitiesOverviewFragment()
            }
        }
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showAuthenticationIfRequired() {
        if (viewModel.shouldShowTerms())
            startActivity(Intent(this, IntroTermsActivity::class.java))
        else if (App.appCore.session.hasSetupPassword)
            startActivity(Intent(this, AuthLoginActivity::class.java))
        else
            startActivity(Intent(this, AuthSetupActivity::class.java))
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
        viewModel.identityErrorLiveData.value?.let { _ ->
            startActivity(Intent(this, IdentityProviderListActivity::class.java))
        }
    }

    /* invoked eg when clicking pending warning */
    override fun identityClicked(identity: Identity) {
        switchToIdentities()
    }

    //endregion
}
