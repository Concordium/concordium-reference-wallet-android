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
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectApproveFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectApproveBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

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
        _binding = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.serviceName.text = _viewModel.walletConnectData.sessionProposal?.name ?: ""
        binding.disconnect.setOnClickListener {
            showDisconnectWarning()
        }
    }

    private fun initObservers() {
        _viewModel.connectStatus.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                binding.statusImageview.setImageResource(R.drawable.ic_big_logo_ok)
                binding.disconnect.isEnabled = true
                binding.header1.visibility = View.GONE
                binding.header2.text = getString(R.string.wallet_connect_connecting_is_connected_to)
                binding.waitForActions.visibility = View.VISIBLE
            }
            else {
                binding.statusImageview.setImageResource(R.drawable.ic_logo_icon_pending)
                binding.disconnect.isEnabled = false
                binding.header1.visibility = View.VISIBLE
                binding.header2.text = getString(R.string.wallet_connect_connecting_account_to)
                binding.waitForActions.visibility = View.GONE
            }
        }
    }

    private fun showDisconnectWarning() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_disconnect_warning_title)
        builder.setMessage(getString(R.string.wallet_connect_disconnect_warning_message, _viewModel.walletConnectData.sessionProposal?.name ?: ""))
        builder.setPositiveButton(getString(R.string.wallet_connect_disconnect_warning_button_disconnect)) { _, _ ->
            _viewModel.disconnectWalletConnect()
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_disconnect_warning_button_stay)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}
