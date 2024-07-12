package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.more.export.ExportActivity
import kotlinx.android.synthetic.main.dialog_shielding_notice.continue_with_old_wallet_button
import kotlinx.android.synthetic.main.dialog_shielding_notice.export_file_button
import kotlinx.android.synthetic.main.dialog_shielding_notice.install_cryptox_button
import kotlinx.coroutines.delay

class ShieldingNoticeDialogFragment :
    AppCompatDialogFragment() {
    private val cryptoXUrl: String by lazy {
        requireNotNull(arguments?.getString(CRYPTOX_URL_KEY))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_shielding_notice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        export_file_button.setOnClickListener {
            val intent = Intent(requireContext(), ExportActivity::class.java)
            startActivity(intent)
        }

        continue_with_old_wallet_button.setOnClickListener {
            dismiss()
        }

        install_cryptox_button.setOnClickListener {
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
