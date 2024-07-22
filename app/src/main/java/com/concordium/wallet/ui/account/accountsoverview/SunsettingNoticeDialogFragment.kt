package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.more.export.ExportActivity
import kotlinx.android.synthetic.main.dialog_sunsetting_notice.continue_with_old_wallet_button
import kotlinx.android.synthetic.main.dialog_sunsetting_notice.export_file_button
import kotlinx.android.synthetic.main.dialog_sunsetting_notice.extra_space
import kotlinx.android.synthetic.main.dialog_sunsetting_notice.install_cryptox_button
import kotlinx.android.synthetic.main.dialog_sunsetting_notice.title_textview
import kotlinx.coroutines.delay

class SunsettingNoticeDialogFragment : AppCompatDialogFragment() {
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
        return inflater.inflate(R.layout.dialog_sunsetting_notice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extra_space.isVisible = isForced

        title_textview.text =
            if (!isForced)
                getString(R.string.sunsetting_notice_title)
            else
                getString(R.string.sunsetting_notice_forced_title)

        export_file_button.setOnClickListener {
            val intent = Intent(requireContext(), ExportActivity::class.java)
            startActivity(intent)
        }

        continue_with_old_wallet_button.isVisible = !isForced
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
            isCancelable = !isForced
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
