package com.concordium.wallet.ui.identity.identityconfirmed

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.databinding.ActivityIdentityConfirmedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.common.account.BaseAccountActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.uicore.dialog.Dialogs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdentityConfirmedActivity : BaseAccountActivity(), Dialogs.DialogFragmentListener {
    private lateinit var binding: ActivityIdentityConfirmedBinding
    private lateinit var viewModel: IdentityConfirmedViewModel
    private var showForFirstIdentity = false
    private var showForCreateAccount = false
    private var identity: Identity? = null

    companion object {
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
        const val SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
        const val SHOW_FOR_CREATE_ACCOUNT = "SHOW_FOR_CREATE_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityConfirmedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showForFirstIdentity = intent.extras?.getBoolean(SHOW_FOR_FIRST_IDENTITY, false) ?: false
        showForCreateAccount = intent.extras?.getBoolean(SHOW_FOR_CREATE_ACCOUNT, false) ?: false

        if (showForFirstIdentity)
            setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identity_confirmed_title)
        else {
            if (showForCreateAccount)
                setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identity_confirmed_create_new_account)
            else
                setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identity_provider_list_title)
        }

        hideActionBarBack()
        if (!showForFirstIdentity && showForCreateAccount)
            showActionBarBack()

        identity = intent.extras!!.getSerializable(EXTRA_IDENTITY) as Identity

        initializeNewAccountViewModel()
        initializeAuthenticationObservers()
        initializeViewModel()
        initializeViews()

        // If we're being restored from a previous state
        if (savedInstanceState != null) {
            return
        }

        viewModel.startIdentityUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
    }

    override fun onBackPressed() {
        if (!showForFirstIdentity && showForCreateAccount)
            super.onBackPressed()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                // Just go back to the identityProvider list to try again
                finish()
            }
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityConfirmedViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.isFirstIdentityLiveData.observe(this) { isFirst ->
            isFirst?.let {
                updateInfoText(isFirst)
            }
        }
        viewModel.identityErrorLiveData.observe(this) { data ->
            data?.let {
                runOnUiThread {
                    showCreateIdentityError(it.identity.status)
                }
            }
        }
        viewModel.identityDoneLiveData.observe(this) {
            if (showForFirstIdentity) {
                updateIdentityView()
                showSubmitAccount()
            } else {
                updateIdentityView()
            }
        }
    }

    private fun initializeViews() {
        showWaiting(true)

        binding.confirmButton.setOnClickListener {
            if (showForFirstIdentity)
                showSubmitAccount()
            else {
                if (!showForCreateAccount)
                    App.appCore.newIdentityPending = identity
                finish()
            }
        }

        identity?.let {
            binding.identityView.setIdentityData(it)
        }

        binding.rlAccount.visibility = View.GONE

        if (showForFirstIdentity) {
            binding.confirmButton.text = getString(R.string.identity_confirmed_confirm)
        } else {
            if (showForCreateAccount) {
                showSubmitAccount()
            }
            binding.progressLine.visibility = View.GONE
            binding.confirmButton.text = getString(R.string.identity_confirmed_finish_button)
        }

        binding.btnSubmitAccount.setOnClickListener {
            binding.btnSubmitAccount.visibility = View.GONE
            CoroutineScope(Dispatchers.IO).launch {
                identity?.let { indent ->
                    val nextAccountNumber = viewModelNewAccount.nextAccountNumber(indent.id)
                    runOnUiThread {
                        viewModelNewAccount.initialize("${getString(R.string.account)} $nextAccountNumber", indent)
                        viewModelNewAccount.confirmWithoutAttributes()
                    }
                }
            }
        }
    }

    private fun updateIdentityView() {
        CoroutineScope(Dispatchers.IO).launch {
            identity?.let {
                viewModel.getIdentityFromId(it.id)?.let { refreshedIdentity ->
                    identity = refreshedIdentity
                    runOnUiThread {
                        binding.identityView.setIdentityData(refreshedIdentity)
                    }
                }
            }
        }
    }

    private fun showCreateIdentityError(errorFromIdentityProvider: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.dialog_identity_create_error_title)
        builder.setMessage(getString(R.string.dialog_identity_create_error_text, errorFromIdentityProvider))
        builder.setPositiveButton(getString(R.string.dialog_identity_create_error_retry)) { _, _ ->
            finish()
            val intent = Intent(this, IdentityProviderListActivity::class.java)
            if (showForFirstIdentity)
                intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
            startActivity(intent)
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

     override fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    override fun showError(stringRes: Int) {
        // error will be shown on account overview page later
    }

    override fun accountCreated(account: Account) {
        if (showForFirstIdentity) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            binding.confirmButton.visibility = View.VISIBLE
            binding.accountView.setAccount(account)
        }
    }

    private fun showSubmitAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            val identityDao = WalletDatabase.getDatabase(application).identityDao()
            val identityRepository = IdentityRepository(identityDao)
            identity?.let {
                identity = identityRepository.findById(it.id)
            }
            runOnUiThread {
                identity?.let {
                    binding.identityView.setIdentityData(it)
                    binding.accountView.setDefault("${getString(R.string.identity)} ${it.id}", "${getString(R.string.account)} ${it.nextAccountNumber}")
                    binding.accountView.visibility = View.VISIBLE
                    binding.btnSubmitAccount.isEnabled = it.status == IdentityStatus.DONE
                    binding.confirmButton.visibility = View.GONE
                    binding.rlAccount.visibility = View.VISIBLE
                    binding.progressLine.setFilledDots(4)
                    binding.progressLine.invalidate()
                    if (showForCreateAccount) {
                        setActionBarTitle(R.string.identity_confirmed_create_new_account)
                        binding.infoTextview.text = getString(R.string.identity_confirmed_submit_new_account_for_identity, it.id.toString())
                    }
                    else {
                        setActionBarTitle(R.string.identity_confirmed_confirm_account_submission_toolbar)
                        binding.infoTextview.text = getString(R.string.identity_confirmed_confirm_account_submission_text)
                    }
                    binding.tvHeader.text = getString(R.string.identity_confirmed_confirm_account_submission_title)
                }
            }
        }
    }

    private fun updateInfoText(isFirstIdentity: Boolean) {
        if (isFirstIdentity) {
            binding.infoTextview.setText(R.string.identity_confirmed_info_first)
        } else {
            binding.infoTextview.setText(R.string.identity_confirmed_info)
        }
    }
}
