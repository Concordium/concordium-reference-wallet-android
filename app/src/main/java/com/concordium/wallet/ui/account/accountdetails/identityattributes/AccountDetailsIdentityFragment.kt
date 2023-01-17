package com.concordium.wallet.ui.account.accountdetails.identityattributes


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_account_details_identity.*

class AccountDetailsIdentityFragment : BaseFragment() {

    private lateinit var viewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    //region Lifecycle
    //************************************************************

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_details_identity, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(AccountDetailsViewModel::class.java)

        viewModel.identityLiveData.observe(viewLifecycleOwner, Observer { identity ->
            identity?.let {
                initIdentityAttributeList(identity.identityProvider.ipInfo.ipDescription.name)
            }
        })
    }

    private fun initIdentityAttributeList(providerName: String) {
        if(viewModel.account.revealedAttributes.isEmpty()){
            no_identity_data_textview.visibility = View.VISIBLE
        }
        else{
            no_identity_data_textview.visibility = View.GONE
        }

        identity_recyclerview.adapter =
            IdentityAttributeAdapter(
                viewModel.account.revealedAttributes,
                providerName
            )
    }

    //endregion

    //region Control/UI
    //************************************************************

    //endregion
}
