package com.concordium.wallet.ui.identity.identitiesoverview

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.ui.identity.identitycreate.IdentityCreateActivity
import com.concordium.wallet.ui.identity.identitydetails.IdentityDetailsActivity
import kotlinx.android.synthetic.main.fragment_identities_overview.*
import kotlinx.android.synthetic.main.fragment_identities_overview.view.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.progress.view.*

class IdentitiesOverviewFragment : BaseFragment() {

    private lateinit var viewModel: IdentitiesOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var identityAdapter: IdentityAdapter

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initializeViewModel()
        viewModel.initialize()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_identities_overview, container, false)
        initializeViews(rootView)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_item_menu -> gotoCreateIdentity()
        }
        return true
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(IdentitiesOverviewViewModel::class.java)
        mainViewModel = ViewModelProvider(
            activity!!,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
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

    private fun initializeViews(view: View) {
        mainViewModel.setTitle(getString(R.string.identities_overview_title))
        view.progress_layout.visibility = View.VISIBLE
        view.no_identity_layout.visibility = View.GONE

        view.new_identity_button.setOnClickListener {
            gotoCreateIdentity()
        }

        initializeList(view)
    }

    private fun initializeList(view: View) {
        identityAdapter = IdentityAdapter()
        view.identity_recyclerview.setHasFixedSize(true)
        view.identity_recyclerview.adapter = identityAdapter

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
        val intent = Intent(activity, IdentityCreateActivity::class.java)
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
        val intent = Intent(activity, IdentityDetailsActivity::class.java)
        intent.putExtra(
            IdentityDetailsActivity.EXTRA_IDENTITY, identity
        )
        startActivity(intent)
    }

    //endregion

}