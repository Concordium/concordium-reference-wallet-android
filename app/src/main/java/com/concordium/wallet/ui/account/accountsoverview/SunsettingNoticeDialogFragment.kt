package com.concordium.wallet.ui.account.accountsoverview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.repository.AuthenticationRepository
import com.concordium.wallet.databinding.DialogSunsettingNoticeBinding
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SunsettingNoticeDialogFragment :
    AppCompatDialogFragment(),
    AuthDelegate by AuthDelegateImpl() {

    private lateinit var binding: DialogSunsettingNoticeBinding

    private val authenticationRepository: AuthenticationRepository by inject()
    private val isForced: Boolean by lazy {
        arguments?.getBoolean(IS_FORCED_KEY, false) == true
    }
    private val cryptoXUrl: String by lazy {
        requireNotNull(arguments?.getString(CRYPTOX_URL_KEY))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSunsettingNoticeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.extraSpace.isVisible = isForced

        binding.titleTextview.text =
            if (!isForced)
                getString(R.string.sunsetting_notice_title)
            else
                getString(R.string.sunsetting_notice_forced_title)

        authenticationRepository.getSeedPhase()
            .onSuccess { phrase ->
                binding.messageTextview.text = getText(R.string.sunsetting_notice_message)
                binding.copySeedButton.isVisible = false
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
                binding.continueWithOldWalletButton.isVisible = !isForced
                dialog?.setCancelable(!isForced)
            }
            .onFailure {
                binding.messageTextview.text = getText(R.string.sunsetting_notice_no_phrase_message)
                binding.copyPhraseButton.isVisible = false
                binding.continueWithOldWalletButton.isVisible = true
                binding.copySeedButton.isVisible = true
                binding.copySeedButton.setOnClickListener {
                    showAuthentication(
                        activity = requireActivity() as AppCompatActivity,
                        authenticated = { password ->
                            lifecycleScope.launch {
                                try {
                                    val seed = AuthPreferences(requireContext())
                                        .getSeedPhrase(password!!)
                                    val clipboard: ClipboardManager? =
                                        getSystemService(
                                            requireContext(),
                                            ClipboardManager::class.java
                                        )
                                    val clip = ClipData.newPlainText("Wallet private key", seed)
                                    clipboard?.setPrimaryClip(clip)
                                } catch (e: Exception) {
                                    Log.e("seed_decrypt_failed", e)
                                }
                            }
                        }
                    )
                }
                dialog?.setCancelable(true)
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
            App.appCore.session.sunsettingNoticeShown()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (!isForced)
                    ViewGroup.LayoutParams.WRAP_CONTENT
                else
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(
                if (!isForced)
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.bg_deprecation_notice
                    )
                else
                    ColorDrawable(Color.WHITE)
            )
        }
    }

    companion object {
        const val TAG = "sunsetting-notice"
        private const val IS_FORCED_KEY = "is_forced"
        private const val CRYPTOX_URL_KEY = "cryptox_url"

        fun newInstance(
            cryptoXUrl: String,
            isForced: Boolean = false,
        ) = SunsettingNoticeDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(IS_FORCED_KEY, isForced)
                putString(CRYPTOX_URL_KEY, cryptoXUrl)
            }
        }
    }
}
