package com.concordium.wallet.ui.account.newaccountsetup

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityNewAccountSetupBinding
import com.concordium.wallet.ui.account.newaccountconfirmed.NewAccountConfirmedActivity
import com.concordium.wallet.ui.account.newaccountidentityattributes.NewAccountIdentityAttributesActivity
import com.concordium.wallet.ui.common.account.BaseAccountActivity
import com.concordium.wallet.util.KeyboardUtil

class NewAccountSetupActivity : BaseAccountActivity() {
    companion object {
        const val EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME"
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
    }

    private lateinit var binding: ActivityNewAccountSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.new_account_setup_title)

        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) as String
        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity

        initializeNewAccountViewModel()
        initializeAuthenticationObservers()

        viewModelNewAccount.initialize(accountName, identity)
        initViews()
    }

    private fun initViews() {
        binding.includeProgress.progressLayout.visibility = View.GONE
        binding.confirmRevealButton.setOnClickListener {
            gotoNewAccountIdentityAttributes(viewModelNewAccount.accountName, viewModelNewAccount.identity)
        }
        binding.confirmSubmitButton.setOnClickListener {
            binding.confirmRevealButton.isEnabled = false
            binding.confirmSubmitButton.isEnabled = false
            viewModelNewAccount.confirmWithoutAttributes()
        }
    }

     override fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
            binding.confirmRevealButton.isEnabled = false
            binding.confirmSubmitButton.isEnabled = false
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
            binding.confirmRevealButton.isEnabled = true
            binding.confirmSubmitButton.isEnabled = true
        }
    }

     override fun showError(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    override fun accountCreated(account: Account) {
        val intent = Intent(this, NewAccountConfirmedActivity::class.java)
        intent.putExtra(NewAccountConfirmedActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoNewAccountIdentityAttributes(accountName: String, identity: Identity) {
        val intent = Intent(this, NewAccountIdentityAttributesActivity::class.java)
        intent.putExtra(NewAccountIdentityAttributesActivity.EXTRA_ACCOUNT_NAME, accountName)
        intent.putExtra(NewAccountIdentityAttributesActivity.EXTRA_IDENTITY, identity)
        startActivity(intent)
    }
}
