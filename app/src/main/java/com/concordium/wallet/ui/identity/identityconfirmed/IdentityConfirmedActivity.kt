package com.concordium.wallet.ui.identity.identityconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.databinding.ActivityIdentityConfirmedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.common.account.BaseAccountActivity
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdentityConfirmedActivity : BaseAccountActivity(), IdentityStatusDelegate by IdentityStatusDelegateImpl() {
    private lateinit var binding: ActivityIdentityConfirmedBinding
    private lateinit var viewModel: IdentityConfirmedViewModel
    private var showForFirstIdentity = false
    private var showForCreateAccount = false
    private var identity: Identity? = null
    private var accountCreated = false

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

        identity = intent.getSerializable(EXTRA_IDENTITY, Identity::class.java)

        initializeNewAccountViewModel()
        initializeAuthenticationObservers()
        initializeViewModel()
        initializeViews()

        // If we're being restored from a previous state
        if (savedInstanceState != null) {
            return
        }

        if (showForFirstIdentity)
            viewModel.startIdentityUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
    }

    override fun onPause() {
        super.onPause()
        stopCheckForPendingIdentity()
        if (showForFirstIdentity)
            viewModel.stopIdentityUpdate()
    }

    override fun onBackPressed() {
        if (!showForFirstIdentity && showForCreateAccount && !accountCreated)
            super.onBackPressed()
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
    }

    private fun initializeViews() {
        showWaiting(true)

        binding.confirmButton.setOnClickListener {
            if (showForFirstIdentity) {
                binding.progressLine.setFilledDots(4)
                showSubmitAccount()
            }
            else {
                gotoAccountsOverview()
            }
        }

        identity?.let {
            binding.identityView.setIdentityData(it)
            startCheckForPendingIdentity(this, it.id, showForFirstIdentity) { newIdentity ->
                identity = newIdentity
                binding.btnSubmitAccount.isEnabled = newIdentity.status == IdentityStatus.DONE
                binding.identityView.setIdentityData(newIdentity)
            }
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
                    runOnUiThread {
                        viewModelNewAccount.initialize(Account.getDefaultName(""), indent)
                        viewModelNewAccount.confirmWithoutAttributes()
                    }
                }
            }
        }
    }

    private fun gotoAccountsOverview() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
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
            accountCreated = true
            hideActionBarBack()
            binding.confirmButton.visibility = View.VISIBLE
            binding.accountView.setAccount(account)
        }
    }

    private fun showSubmitAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            val identityRepository = IdentityRepository(WalletDatabase.getDatabase(application).identityDao())
            identity?.let {
                identity = identityRepository.findById(it.id)
            }
            runOnUiThread {
                identity?.let {
                    binding.identityView.setIdentityData(it)
                    binding.accountView.setDefault(it.name, "4WHF...eNu8")
                    binding.accountView.visibility = View.VISIBLE
                    binding.btnSubmitAccount.isEnabled = it.status == IdentityStatus.DONE
                    binding.confirmButton.visibility = View.GONE
                    binding.rlAccount.visibility = View.VISIBLE
                    if (showForCreateAccount) {
                        setActionBarTitle(R.string.identity_confirmed_create_new_account)
                        binding.infoTextview.text = getString(R.string.identity_confirmed_submit_new_account_for_identity, it.name)
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
            if (!showForCreateAccount)
                binding.infoTextview.setText(R.string.identity_confirmed_info_next_identity)
        }
    }
}
