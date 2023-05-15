package com.concordium.wallet.ui.account.accountdetails.identityattributes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.FragmentAccountDetailsIdentityBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.base.BaseFragment

class AccountDetailsIdentityFragment : BaseFragment() {
    private var _binding: FragmentAccountDetailsIdentityBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    //region Lifecycle
    //************************************************************

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAccountDetailsIdentityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountDetailsViewModel::class.java]

        viewModel.identityLiveData.observe(viewLifecycleOwner, Observer { identity ->
            identity?.let {
                initIdentityAttributeList(identity.identityProvider.ipInfo.ipDescription.name)
            }
        })
    }

    private fun initIdentityAttributeList(providerName: String) {
        if(viewModel.account.revealedAttributes.isEmpty()){
            binding.noIdentityDataTextview.visibility = View.VISIBLE
        }
        else{
            binding.noIdentityDataTextview.visibility = View.GONE
        }

        binding.identityRecyclerview.adapter =
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
