package com.concordium.wallet.ui.account.accountsoverview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.repository.AuthenticationRepository
import com.concordium.wallet.databinding.DialogShieldingNoticeBinding
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

class ShieldingNoticeDialogFragment :
    AppCompatDialogFragment(),
    AuthDelegate by AuthDelegateImpl() {

    private lateinit var binding: DialogShieldingNoticeBinding

    private val authenticationRepository: AuthenticationRepository by inject()
    private val cryptoXUrl: String by lazy {
        requireNotNull(arguments?.getString(CRYPTOX_URL_KEY))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogShieldingNoticeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authenticationRepository.getSeedPhase().onSuccess { phrase ->
            binding.copyPhraseButton.isVisible = true
            binding.copyPhraseButton.setOnClickListener {
                showAuthentication(
                    activity = requireActivity() as AppCompatActivity,
                    authenticated = {
                        val clipboard: ClipboardManager? =
                            getSystemService(requireContext(), ClipboardManager::class.java)
                        val clip = ClipData.newPlainText("Phrase", phrase)
                        clipboard?.setPrimaryClip(clip)
                    }
                )
            }
        }

        binding.continueWithOldWalletButton.setOnClickListener {
            dismiss()
        }

        binding.installCryptoxButton.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(cryptoXUrl)
                )
            )
        }

        // Track showing the notice once it is visible to the user.
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            delay(500)
            App.appCore.session.shieldingNoticeShown()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_deprecation_notice
                )
            )
        }
    }

    companion object {
        const val TAG = "shielding-notice"
        private const val CRYPTOX_URL_KEY = "cryptox_url"

        fun newInstance(
            cryptoXUrl: String,
        ) = ShieldingNoticeDialogFragment().apply {
            arguments = Bundle().apply {
                putString(CRYPTOX_URL_KEY, cryptoXUrl)
            }
        }
    }
}
