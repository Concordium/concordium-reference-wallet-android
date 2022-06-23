package com.concordium.wallet.ui.identity.identitiesoverview

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.identity.identitydetails.IdentityDetailsActivity
import kotlinx.android.synthetic.main.activity_identities_overview.*
import kotlinx.android.synthetic.main.progress.*

class IdentitiesOverviewActivity : BaseActivity(R.layout.activity_identities_overview, R.string.identities_overview_title) {

    private lateinit var viewModel: IdentitiesOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var identityAdapter: IdentityAdapter

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
        initializeViews()
        viewModel.initialize()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_item_menu -> gotoCreateIdentity()
        }
        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(IdentitiesOverviewViewModel::class.java)
        mainViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(MainViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.identityListLiveData.observe(this, Observer { identityList ->
            identityList?.let {
                identityAdapter.setData(it)
                showWaiting(false)
                if (identityList.isEmpty()) {
                    no_identity_layout.visibility = View.VISIBLE
                } else {
                    no_identity_layout.visibility = View.GONE
                }
            }
        })
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.identities_overview_title))
        progress_layout.visibility = View.VISIBLE
        no_identity_layout.visibility = View.GONE

        new_identity_button.setOnClickListener {
            gotoCreateIdentity()
        }

        initializeList()
    }

    private fun initializeList() {
        identityAdapter = IdentityAdapter()
        identity_recyclerview.setHasFixedSize(true)
        identity_recyclerview.adapter = identityAdapter

        identityAdapter.setOnItemClickListener(object : IdentityAdapter.OnItemClickListener {
            override fun onItemClicked(item: Identity) {
                gotoIdentityDetails(item)
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoCreateIdentity() {
        val intent = Intent(this, IdentityCreateActivity::class.java)
        startActivity(intent)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun gotoIdentityDetails(identity: Identity) {
        val intent = Intent(this, IdentityDetailsActivity::class.java)
        intent.putExtra(
            IdentityDetailsActivity.EXTRA_IDENTITY, identity
        )
        startActivity(intent)
    }

    //endregion

}