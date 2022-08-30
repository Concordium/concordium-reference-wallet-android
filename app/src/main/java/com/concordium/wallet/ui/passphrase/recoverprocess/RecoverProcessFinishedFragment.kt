package com.concordium.wallet.ui.passphrase.recoverprocess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentRecoverProcessFinishedBinding
import com.concordium.wallet.databinding.ItemIdentityWithAccountsBinding
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessViewModel.Companion.RECOVER_PROCESS_DATA

class RecoverProcessFinishedFragment : RecoverProcessBaseFragment() {
    private var _binding: FragmentRecoverProcessFinishedBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(recoverProcessData: RecoverProcessData) = RecoverProcessFinishedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(RECOVER_PROCESS_DATA, recoverProcessData)
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
        if (recoverProcessData.noResponseFrom.size > 0) {
            binding.resultIcon.setImageDrawable(context?.let { ContextCompat.getDrawable(it, R.drawable.ic_logo_icon_error) })
            binding.partialTextview.visibility = View.VISIBLE
            binding.providerNames.visibility = View.VISIBLE
            binding.providerNames.text = recoverProcessData.noResponseFrom.joinToString("\n") { it }
            binding.headerTextview.text = getString(R.string.pass_phrase_recover_process_partial)
        }
        else {
            binding.resultIcon.setImageDrawable(context?.let { ContextCompat.getDrawable(it, R.drawable.ic_big_logo_ok) })
            binding.partialTextview.visibility = View.GONE
            binding.providerNames.visibility = View.GONE
            binding.headerTextview.text = getString(R.string.pass_phrase_recover_process_finished)
        }

        recoverProcessData.identitiesWithAccounts.forEach { identityWithAccounts ->
            val itemIdentityWithAccounts = ItemIdentityWithAccountsBinding.inflate(LayoutInflater.from(context), null, false)
            itemIdentityWithAccounts.identityName.text = identityWithAccounts.identity.name
            identityWithAccounts.accounts.forEach { account ->
                val accountTextView = TextView(context)
                accountTextView.setTextAppearance(R.style.TextView_Standard)
                val finalizedBalance = CurrencyUtil.formatGTU(account.finalizedBalance, true)
                val accountText = "${account.name} - $finalizedBalance"
                accountTextView.text = accountText
                itemIdentityWithAccounts.accounts.addView(accountTextView)
            }
            binding.identitiesAccounts.addView(itemIdentityWithAccounts.root)
        }
    }
}
