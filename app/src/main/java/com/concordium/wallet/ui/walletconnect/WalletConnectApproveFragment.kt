package com.concordium.wallet.ui.walletconnect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectApproveBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectApproveFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectApproveBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel
    private var didConnectBefore = false
    private var timeoutTimer: CountDownTimer? = null

    companion object {
        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectApproveFragment().apply {
            arguments = Bundle().apply {
                putSerializable(WALLET_CONNECT_DATA, viewModel.walletConnectData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletConnectApproveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        if (_viewModel.binder?.getSessionTopic()
                ?.isNotEmpty() == false && savedInstanceState == null
        ) {
            _viewModel.waiting.postValue(true)
            _viewModel.approveSession()
        }
        startTimeOutTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimeOutTimer()
        _binding = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.serviceName.text = _viewModel.sessionName()
        binding.disconnect.setOnClickListener {
            binding.disconnect.isEnabled = false
            showDisconnectWarning()
        }
    }

    private fun initObservers() {
        _viewModel.connectStatus.observe(viewLifecycleOwner) { isConnected ->
            _viewModel.waiting.postValue(false)
            if (isConnected) {
                stopTimeOutTimer()
                didConnectBefore = true
                binding.statusImageview.setImageResource(R.drawable.ic_big_logo_ok)
                binding.disconnect.isEnabled = true
                binding.header1.visibility = View.GONE
                binding.header2.text = getString(R.string.wallet_connect_connecting_is_connected_to)
                binding.waitForActions.visibility = View.VISIBLE
                (activity as BaseActivity).setActionBarTitle(
                    getString(
                        R.string.wallet_connect_session_with,
                        _viewModel.sessionName()
                    )
                )
            } else {
                if (didConnectBefore) {
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
            _viewModel.waiting.postValue(false)
            showTryApproveAgain()
        }
    }

    private fun startTimeOutTimer() {
        timeoutTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisecondFinished: Long) {
            }

            override fun onFinish() {
                if (!didConnectBefore)
                    showTryApproveAgain()
            }
        }.start()
    }

    private fun stopTimeOutTimer() {
        timeoutTimer?.cancel()
        timeoutTimer = null
    }

    private fun showTryApproveAgain() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_approve_try_again_title)
        builder.setMessage(getString(R.string.wallet_connect_approve_try_again_message))
        builder.setPositiveButton(getString(R.string.wallet_connect_approve_try_again_try_again)) { _, _ ->
            _viewModel.approveSession()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_approve_try_again_later)) { dialog, _ ->
            dialog.dismiss()
            gotoMain()
        }
        builder.create().show()
    }

    private fun showDisconnectWarning() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_disconnect_warning_title)
        builder.setMessage(
            getString(
                R.string.wallet_connect_disconnect_warning_message,
                _viewModel.sessionName()
            )
        )
        builder.setPositiveButton(getString(R.string.wallet_connect_disconnect_warning_button_disconnect)) { _, _ ->
            gotoMain()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_disconnect_warning_button_stay)) { dialog, _ ->
            binding.disconnect.isEnabled = true
            dialog.dismiss()
        }
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
        activity?.finish()
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
