package com.concordium.wallet.ui.passphrase.recoverprocess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.concordium.wallet.databinding.FragmentRecoverProcessFinishedBinding
import com.concordium.wallet.databinding.ItemIdentityWithAccountsBinding
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessViewModel.Companion.RECOVER_PROCESS_DATA

class RecoverProcessFinishedFragment : RecoverProcessBaseFragment() {
    private var _binding: FragmentRecoverProcessFinishedBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(viewModel: RecoverProcessViewModel) = RecoverProcessFinishedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(RECOVER_PROCESS_DATA, viewModel)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecoverProcessFinishedBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        viewModel.identitiesWithAccounts.forEach { identityWithAccounts ->
            val itemIdentityWithAccounts = ItemIdentityWithAccountsBinding.inflate(LayoutInflater.from(context), null, false)
            itemIdentityWithAccounts.identityName.text = identityWithAccounts.identity.name
            identityWithAccounts.accounts.forEach { account ->
                val accountTextView = TextView(context)
                accountTextView.textSize = 14f
                val accountText = "${account.name} - ${account.totalBalance}"
                accountTextView.text = accountText
                itemIdentityWithAccounts.accounts.addView(accountTextView)
            }
            binding.identitiesAccounts.addView(itemIdentityWithAccounts.root)
        }
    }
}
