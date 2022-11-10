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
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.intro.introsetup.IntroSetupActivity
import com.concordium.wallet.ui.intro.introstart.IntroTermsActivity
import com.concordium.wallet.ui.intro.introstart.WalletNotSetupActivity
import com.concordium.wallet.ui.more.SettingsActivity
import com.concordium.wallet.ui.recipient.scanqr.ScanQRActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectActivity
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : BaseActivity(), IdentityStatusDelegate by IdentityStatusDelegateImpl() {
    companion object {
        const val EXTRA_SHOW_IDENTITIES = "EXTRA_SHOW_IDENTITIES"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var wcUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)  // Set theme to default to remove launcher theme
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        initializeViewModel()
        viewModel.initialize()

        initializeViews()

        intent?.data?.let { if (it.toString().startsWith("wc")) wcUri = it.toString() }

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
            startActivity(Intent(this, SettingsActivity::class.java))
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
        val actionBarSize = getDimenFromAttr(this, android.R.attr.actionBarSize) * 0.4
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
                it.data?.getStringExtra(ScanQRActivity.EXTRA_BARCODE)?.let { uri ->
                    wcUri = uri
                    gotoWalletConnect()
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

        val pairings: List<Core.Model.Pairing> = CoreClient.Pairing.getPairings()
        println("LC -> EXISTING PAIRINGS in MainActivity = ${pairings.count()}")
        pairings.forEach { pairing ->
            CoreClient.Pairing.disconnect(pairing.topic) { modelError ->
                println("LC -> DISCONNECT ERROR ${modelError.throwable.stackTraceToString()}")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            if (it.toString().startsWith("wc")) {
                wcUri = it.toString()
                if (App.appCore.session.isLoggedIn.value == true && AuthPreferences(this).hasSeedPhrase()) {
                    gotoWalletConnect()
                } else {
                    showAuthenticationIfRequired()
                }
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

    override fun loggedOut() {
    }

    private fun showAuthenticationIfRequired() {
        if (shouldShowTerms()) {
            if (wcUri.isNotBlank()) {
                wcUri = ""
                getResultWalletNotSetupIntroTerms.launch(Intent(this, WalletNotSetupActivity::class.java))
            }
            else {
                startActivity(Intent(this, IntroTermsActivity::class.java))
            }
        }
        else if (App.appCore.session.hasSetupPassword) {
            if (wcUri.isNotBlank()) {
                if (AuthPreferences(this).hasSeedPhrase())
                    getResultAuthLogin.launch(Intent(this, AuthLoginActivity::class.java))
                else {
                    wcUri = ""
                    getResultWalletNotSetupPassPhrase.launch(Intent(this, WalletNotSetupActivity::class.java))
                }
            }
            else
                startActivity(Intent(this, AuthLoginActivity::class.java))
        }
        else {
            if (wcUri.isNotBlank()) {
                wcUri = ""
                getResultWalletNotSetupAuthSetup.launch(Intent(this, WalletNotSetupActivity::class.java))
            }
            else
                startActivity(Intent(this, AuthSetupActivity::class.java))
        }
    }

    private val getResultWalletNotSetupIntroTerms =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
            startActivity(Intent(this, IntroTermsActivity::class.java))
        }

    private val getResultWalletNotSetupAuthSetup =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
            startActivity(Intent(this, AuthSetupActivity::class.java))
        }

    private val getResultWalletNotSetupPassPhrase =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            startActivity(Intent(this, IntroSetupActivity::class.java))
        }

    private val getResultAuthLogin =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
            gotoWalletConnect()
        }

    private fun gotoWalletConnect() {
        val intent = Intent(this, WalletConnectActivity::class.java)
        intent.putExtra(WalletConnectActivity.FROM_DEEP_LINK, false)
        intent.putExtra(WalletConnectActivity.WC_URI, wcUri)
        startActivity(intent)
    }

    private fun shouldShowTerms(): Boolean {
        val hashNew = App.appContext.getString(R.string.terms_text).hashCode()
        val hashOld = App.appCore.session.getTermsHashed()
        return hashNew != hashOld
    }
}
