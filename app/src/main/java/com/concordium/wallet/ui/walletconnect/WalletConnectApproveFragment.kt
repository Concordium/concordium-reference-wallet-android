package com.concordium.wallet.ui.walletconnect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectApproveBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA
import java.util.*
import kotlin.concurrent.schedule

class WalletConnectApproveFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectApproveBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel
    private var didConnectBefore = false
    private var pingTimer: Timer? = null

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel, walletConnectData: WalletConnectData) = WalletConnectApproveFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletConnectApproveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        _viewModel.approve()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPingTimer()
        _binding = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.serviceName.text = _viewModel.sessionName()
        binding.disconnect.setOnClickListener {
            showDisconnectWarning()
        }
    }

    private fun initObservers() {
        _viewModel.connectStatus.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                didConnectBefore = true
                binding.statusImageview.setImageResource(R.drawable.ic_big_logo_ok)
                binding.disconnect.isEnabled = true
                binding.header1.visibility = View.GONE
                binding.header2.text = getString(R.string.wallet_connect_connecting_is_connected_to)
                binding.waitForActions.visibility = View.VISIBLE
                (activity as BaseActivity).setActionBarTitle(getString(R.string.wallet_connect_session_with, _viewModel.sessionName()))
                startPingTimer()
            }
            else {
                if (didConnectBefore) {
                    stopPingTimer()
                    showConnectionLost()
                } else {
                    binding.statusImageview.setImageResource(R.drawable.ic_logo_icon_pending)
                    binding.disconnect.isEnabled = false
                    binding.header1.visibility = View.VISIBLE
                    binding.header2.text = getString(R.string.wallet_connect_connecting_account_to)
                    binding.waitForActions.visibility = View.GONE
                    (activity as BaseActivity).setActionBarTitle(getString(R.string.wallet_connect_session))
                }
            }
        }
        _viewModel.errorWalletConnectApprove.observe(viewLifecycleOwner) {
            showTryApproveAgain()
        }
    }

    private fun showTryApproveAgain() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_approve_try_again_title)
        builder.setMessage(getString(R.string.wallet_connect_approve_try_again_message))
        builder.setPositiveButton(getString(R.string.wallet_connect_approve_try_again_try_again)) { _, _ ->
            _viewModel.approve()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_approve_try_again_later)) {dialog, _ ->
            dialog.dismiss()
            gotoMain()
        }
        builder.create().show()
    }

    private fun showDisconnectWarning() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_disconnect_warning_title)
        builder.setMessage(getString(R.string.wallet_connect_disconnect_warning_message, _viewModel.sessionName()))
        builder.setPositiveButton(getString(R.string.wallet_connect_disconnect_warning_button_disconnect)) { _, _ ->
            gotoMain()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_disconnect_warning_button_stay)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showConnectionLost() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_connection_lost_title)
        builder.setMessage(getString(R.string.wallet_connect_connection_lost_message))
        builder.setPositiveButton(getString(R.string.wallet_connect_connection_lost_okay)) { _, _ ->
            gotoMain()
        }
        builder.create().show()
    }

    private fun gotoMain() {
        _viewModel.disconnect()
        activity?.finish()
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun stopPingTimer() {
        pingTimer?.cancel()
        pingTimer?.purge()
        pingTimer = null
    }

    private fun startPingTimer() {
        stopPingTimer()
        pingTimer = Timer()
        pingTimer?.schedule(5000) {
            _viewModel.ping()
        }
    }
}
