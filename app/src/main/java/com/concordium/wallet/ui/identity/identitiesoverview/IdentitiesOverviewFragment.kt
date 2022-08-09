package com.concordium.wallet.ui.identity.identitiesoverview

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.FragmentIdentitiesOverviewBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.ui.identity.identitydetails.IdentityDetailsActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity

class IdentitiesOverviewFragment : BaseFragment() {
    private var _binding: FragmentIdentitiesOverviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: IdentitiesOverviewViewModel
    private lateinit var identityAdapter: IdentityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIdentitiesOverviewBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        initializeViewModel()
        initializeViews()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun gotoCreateIdentity() {
        startActivity(Intent(requireActivity(), IdentityProviderListActivity::class.java))
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[IdentitiesOverviewViewModel::class.java]
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.identityListLiveData.observe(requireActivity()) { identityList ->
            identityList?.let {
                identityAdapter.setData(it)
                showWaiting(false)
                if (identityList.isEmpty()) {
                    binding.noIdentityLayout.visibility = View.VISIBLE
                    binding.identityRecyclerview.visibility = View.GONE
                } else {
                    binding.noIdentityLayout.visibility = View.GONE
                    binding.identityRecyclerview.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initializeViews() {
        initializeList()
    }

    private fun initializeList() {
        identityAdapter = IdentityAdapter()
        binding.identityRecyclerview.setHasFixedSize(true)
        binding.identityRecyclerview.adapter = identityAdapter
        identityAdapter.setOnItemClickListener(object : IdentityAdapter.OnItemClickListener {
            override fun onItemClicked(item: Identity) {
                gotoIdentityDetails(item)
            }
        })
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoIdentityDetails(identity: Identity) {
        val intent = Intent(activity, IdentityDetailsActivity::class.java)
        intent.putExtra(IdentityDetailsActivity.EXTRA_IDENTITY, identity)
        startActivity(intent)
    }
}
