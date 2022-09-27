package com.concordium.wallet.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.intro.introstart.IntroTermsActivity
import com.concordium.wallet.ui.more.MoreActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : BaseActivity(), IdentityStatusDelegate by IdentityStatusDelegateImpl() {
    companion object {
        const val EXTRA_SHOW_IDENTITIES = "EXTRA_SHOW_IDENTITIES"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)  // Set theme to default to remove launcher theme
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

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

    private fun setupToolbar() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.main_title)
        supportActionBar?.setCustomView(R.layout.app_toolbar_main)
        binding.toolbarLayout.settingsContainer.setOnClickListener {
            startActivity(Intent(this, MoreActivity::class.java))
        }
        binding.toolbarLayout.scanContainer.setOnClickListener {
            scan()
        }
        binding.toolbarLayout.addAccountContainer.setOnClickListener {
            gotoCreateAccount()
        }

        sizeToolbarButton(binding.toolbarLayout.settings)
        sizeToolbarButton(binding.toolbarLayout.scan)
        sizeToolbarButton(binding.toolbarLayout.addAccount)
    }

    private fun sizeToolbarButton(button: AppCompatImageView) {
        val actionBarSize = getDimenFromAttr(this, android.R.attr.actionBarSize) * 0.45
        val layoutParamsAccount = button.layoutParams
        layoutParamsAccount.width = actionBarSize.toInt()
        layoutParamsAccount.height = actionBarSize.toInt()
    }

    private fun getDimenFromAttr(context: Context, attrValue: Int): Float {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrValue, typedValue, true)
        return context.resources.getDimension(typedValue.resourceId)
    }

    private fun gotoCreateAccount() {
        val intent = Intent(this, IdentitiesOverviewActivity::class.java)
        intent.putExtra(IdentitiesOverviewActivity.SHOW_FOR_CREATE_ACCOUNT, true)
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

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        viewModel.stopIdentityUpdate()
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
                viewModel.startIdentityUpdate()
                startCheckForPendingIdentity(this, null, false) {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopCheckForPendingIdentity()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(state: MainViewModel.State) {
        viewModel.setState(state)
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
        viewModel.newFinalizedAccountLiveData.observe(this) { newAccount ->
            newAccount?.let {
            }
        }
    }

    private fun initializeViews() {
        hideActionBarBack()
    }

    //endregion

    //region Menu Navigation
    //************************************************************

    private fun replaceFragment(state: MainViewModel.State) {
        hideActionBarBack()
        val fragment: BaseFragment
        when (state) {
            MainViewModel.State.AccountOverview -> {
                setActionBarTitle(R.string.main_title)
                fragment = AccountsOverviewFragment()
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

    //endregion
}
