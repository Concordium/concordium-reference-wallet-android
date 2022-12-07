package com.concordium.wallet.ui.walletconnect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityWalletConnectBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class WalletConnectActivity : BaseActivity() {
    private lateinit var binding: ActivityWalletConnectBinding
    private lateinit var viewModel: WalletConnectViewModel
    private var fromDeepLink = true
    private var currentPage = 0

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            viewModel.binder = service as WalletConnectService.LocalBinder
            if (viewModel.walletConnectData.account != null)
                pairView()
            else
                accountsView()
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            println("LC -> onServiceDisconnected")
        }
    }

    companion object {
        const val FROM_DEEP_LINK = "FROM_DEEP_LINK"
        const val WC_URI = "WC_URI"
        const val ACCOUNT = "ACCOUNT"
        const val PAGE_CHOOSE_ACCOUNT = 0
        const val PAGE_PAIR = 1
        const val PAGE_APPROVE = 2
        const val PAGE_TRANSACTION = 3
        const val PAGE_TRANSACTION_SUBMITTED = 4
        const val PAGE_MESSAGE = 5
        const val PAGE_MESSAGE_SIGNED = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fromDeepLink = intent.extras?.getBoolean(FROM_DEEP_LINK, true) ?: true

        initViews()
        initializeViewModel()
        initObservers()

        viewModel.register()

        if (fromDeepLink) {
            intent?.data?.let {
                viewModel.walletConnectData.wcUri = it.toString()
                println("LC -> From DeepLink = $it")
            }
        } else {
            if (intent?.hasExtra(ACCOUNT) == true)
                viewModel.walletConnectData.account = intent?.getSerializable(ACCOUNT, Account::class.java)
            viewModel.walletConnectData.wcUri = intent?.getStringExtra(WC_URI) ?: ""
            println("LC -> From Camera = ${viewModel.walletConnectData.wcUri}")
        }

        Intent(this, WalletConnectService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }


    override fun onDestroy() {
        viewModel.disconnect()
        viewModel.unregister()
        unbindService(connection)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (currentPage < PAGE_APPROVE) {
            if (!fromDeepLink) {
                viewModel.rejectSession()
                viewModel.decline.postValue(true)
            }
            else {
                CoroutineScope(Dispatchers.IO).launch {
                    if (viewModel.hasAccounts()) {
                        finish()
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                    } else {
                        finish()
                    }
                }
            }
        }
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.wallet_connect_accounts_title)
        if (fromDeepLink)
            hideActionBarBack()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[WalletConnectViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
        viewModel.errorString.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        viewModel.chooseAccount.observe(this) { accountWithIdentity ->
            viewModel.walletConnectData.account = accountWithIdentity.account
            pairView()
        }
        viewModel.connect.observe(this) {
            approveView()
        }
        viewModel.decline.observe(this) {
            gotoMain()
        }
        viewModel.reject.observe(this) {
            approveView()
        }
        viewModel.transactionAction.observe(this) {
            runOnUiThread {
                transactionView()
            }
        }
        viewModel.transactionSubmittedSuccess.observe(this) { submissionId ->
            viewModel.respondSuccess(submissionId)
            transactionSubmittedView()
        }
        viewModel.transactionSubmittedError.observe(this) { submissionId ->
            viewModel.respondError(submissionId)
        }
        viewModel.transactionSubmittedOkay.observe(this) {
            approveView()
        }
        viewModel.messageAction.observe(this) {
            runOnUiThread {
                messageView()
            }
        }
        viewModel.proofOfIdentityAction.observe(this) {
            runOnUiThread {
                // proofOfIdentityView()
            }
        }
        viewModel.messageSignedSuccess.observe(this) { message ->
            viewModel.respondSuccess(message)
            messageSignedView()
        }
        viewModel.messageSignedError.observe(this) { message ->
            viewModel.respondError(message)
        }
        viewModel.messagedSignedOkay.observe(this) {
            approveView()
        }
        viewModel.showAuthentication.observe(this) {
            showAuthentication(authenticateText(), object : AuthenticationCallback {
                override fun getCipherForBiometrics() : Cipher? {
                    return viewModel.getCipherForBiometrics()
                }
                override fun onCorrectPassword(password: String) {
                    viewModel.continueWithPassword(password)
                }
                override fun onCipher(cipher: Cipher) {
                    viewModel.checkLogin(cipher)
                }
                override fun onCancelled() {
                }
            })
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun accountsView() {
        currentPage = PAGE_CHOOSE_ACCOUNT
        showActionBarBack()
        setActionBarTitle(getString(R.string.wallet_connect_accounts_title))
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectChooseAccountFragment.newInstance(viewModel), null).commit()
    }

    private fun pairView() {
        currentPage = PAGE_PAIR
        showActionBarBack()
        setActionBarTitle(getString(R.string.wallet_connect_allow_session))
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectPairFragment.newInstance(viewModel), null).commit()
    }

    private fun approveView() {
        currentPage = PAGE_APPROVE
        hideActionBarBack()
        setActionBarTitle(getString(R.string.wallet_connect_session_with))
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectApproveFragment.newInstance(viewModel), null).commit()
    }

    private fun transactionView() {
        currentPage = PAGE_TRANSACTION
        hideActionBarBack()
        setActionBarTitle(getString(R.string.wallet_connect_session_with))
        viewModel.walletConnectData.isTransaction = true
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectTransactionFragment.newInstance(viewModel), null).commit()
    }

    private fun transactionSubmittedView() {
        currentPage = PAGE_TRANSACTION_SUBMITTED
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectTransactionSubmittedFragment.newInstance(viewModel), null).commit()
    }

    private fun messageView() {
        currentPage = PAGE_MESSAGE
        hideActionBarBack()
        setActionBarTitle(getString(R.string.wallet_connect_session_with))
        viewModel.walletConnectData.isTransaction = false
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectMessageFragment.newInstance(viewModel), null).commit()
    }

    private fun messageSignedView() {
        currentPage = PAGE_MESSAGE_SIGNED
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, WalletConnectMessageSignedFragment.newInstance(viewModel), null).commit()
    }

    private fun gotoMain() {
        finish()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
