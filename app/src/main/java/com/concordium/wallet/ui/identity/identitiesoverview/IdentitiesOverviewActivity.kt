package com.concordium.wallet.ui.identity.identitiesoverview

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentitiesOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.ui.identity.identitydetails.IdentityDetailsActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity

class IdentitiesOverviewActivity : BaseActivity() {
    private lateinit var binding: ActivityIdentitiesOverviewBinding
    private lateinit var viewModel: IdentitiesOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var identityAdapter: IdentityAdapter
    private var showForCreateAccount = false

    companion object {
        const val SHOW_FOR_CREATE_ACCOUNT = "SHOW_FOR_CREATE_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentitiesOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showForCreateAccount = intent.extras?.getBoolean(SHOW_FOR_CREATE_ACCOUNT, false) ?: false

        if (showForCreateAccount) {
            setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identities_overview_create_account_title)
            binding.selectIdentityTitle.visibility = View.VISIBLE
        }
        else {
            setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identities_overview_title)
        }

        initializeViewModel()
        initializeViews()
        viewModel.loadIdentities()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_item_menu -> gotoCreateIdentity()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        )[IdentitiesOverviewViewModel::class.java]
        mainViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        )[MainViewModel::class.java]
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.identityListLiveData.observe(this) { identityList ->
            identityList?.let {
                identityAdapter.setData(it)
                showWaiting(false)
                if (identityList.isEmpty()) {
                    binding.noIdentityLayout.visibility = View.VISIBLE
                } else {
                    binding.noIdentityLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.identities_overview_title))
        binding.includeProgress.progressLayout.visibility = View.VISIBLE
        binding.noIdentityLayout.visibility = View.GONE

        binding.newIdentityButton.setOnClickListener {
            gotoCreateIdentity()
        }

        initializeList()
    }

    private fun initializeList() {
        identityAdapter = IdentityAdapter()
        binding.identityRecyclerview.setHasFixedSize(true)
        binding.identityRecyclerview.adapter = identityAdapter

        identityAdapter.setOnItemClickListener(object : IdentityAdapter.OnItemClickListener {
            override fun onItemClicked(item: Identity) {
                if (showForCreateAccount)
                    gotoSubAccount(item)
                else
                    gotoIdentityDetails(item)
            }
        })
    }

    private fun gotoCreateIdentity() {
        startActivity(Intent(this, IdentityProviderListActivity::class.java))
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoIdentityDetails(identity: Identity) {
        val intent = Intent(this, IdentityDetailsActivity::class.java)
        intent.putExtra(
            IdentityDetailsActivity.EXTRA_IDENTITY, identity
        )
        startActivity(intent)
    }

    private fun gotoSubAccount(identity: Identity) {
        finish()
        val intent = Intent(this, IdentityConfirmedActivity::class.java)
        intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
        intent.putExtra(IdentityConfirmedActivity.SHOW_FOR_CREATE_ACCOUNT, true)
        startActivity(intent)
    }
}
