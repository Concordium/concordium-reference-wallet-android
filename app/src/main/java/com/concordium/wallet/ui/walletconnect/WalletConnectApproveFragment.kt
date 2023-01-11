package com.concordium.wallet.ui.walletconnect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectApproveBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.PROOF_OF_IDENTITY
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.SIGN_AND_SEND_TRANSACTION
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.SIGN_MESSAGE
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectApproveFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectApproveBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel
    private var didConnectBefore = false
    private var timeoutTimer: CountDownTimer? = null
    private var requestMethod: String = ""

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

    override fun onResume() {
        super.onResume()
        binding.walletConnectActionCard.root.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimeOutTimer()
        _binding = null
    }

    private fun initViews() {
        binding.walletConnectStatusCard.statusTextAccount.text =
            _viewModel.walletConnectData.account?.name ?: ""
        binding.walletConnectStatusCard.statusTextService.text = _viewModel.sessionName()
        binding.disconnect.setOnClickListener {
            binding.disconnect.isEnabled = false
            showDisconnectWarning()
        }
/*

        //FIXME: Remove when ID 2.0 is not mocked
        //
        binding.walletConnectTestCard.actionIcon.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_identity
            )
        )
        binding.walletConnectTestCard.actionText.text = getString(R.string.wallet_connect_proof_of_identity)
        binding.walletConnectTestCard.root.setOnClickListener {
            _viewModel.proofOfIdentityAction.postValue(true)
        }
        //
*/


        binding.walletConnectActionCard.root.setOnClickListener {
            binding.walletConnectActionCard.root.visibility = View.GONE
            when (requestMethod) {
                SIGN_AND_SEND_TRANSACTION -> _viewModel.transactionAction.postValue(true)
                SIGN_MESSAGE-> _viewModel.messageAction.postValue(true)
                PROOF_OF_IDENTITY -> _viewModel.proofOfIdentityAction.postValue(true)
            }
            requestMethod = ""
        }
    }

    private fun initObservers() {
        _viewModel.connectStatus.observe(viewLifecycleOwner) { isConnected ->
            _viewModel.waiting.postValue(false)
            if (isConnected) {
                stopTimeOutTimer()
                didConnectBefore = true
                binding.walletConnectStatusCard.statusIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_big_logo_ok
                    )
                )

                binding.walletConnectStatusCard.statusIcon.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.theme_green
                    )
                )
                binding.walletConnectStatusCard.statusText.text = getString(R.string.wallet_connect_opened_connection_between)
                binding.disconnect.isEnabled = true
                binding.waitForActions.visibility = View.VISIBLE
            } else {
                if (didConnectBefore) {
                    showConnectionLost()
                } else {
                    binding.walletConnectStatusCard.statusIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_logo_icon_pending
                        )
                    )
                    binding.walletConnectStatusCard.statusIcon.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.theme_green
                        )
                    )
                    binding.disconnect.isEnabled = false
                    binding.waitForActions.visibility = View.GONE
                    (activity as BaseActivity).setActionBarTitle(getString(R.string.wallet_connect_session))
                }
            }
        }
        _viewModel.errorWalletConnectApprove.observe(viewLifecycleOwner) {
            _viewModel.waiting.postValue(false)
            showTryApproveAgain()
        }
        _viewModel.transaction.observe(viewLifecycleOwner) { method ->
            setAction(method, R.drawable.ic_send, R.string.wallet_connect_transaction)
        }
        _viewModel.message.observe(viewLifecycleOwner) { method ->
            setAction(method, R.drawable.ic_message, R.string.wallet_connect_message)
        }
        _viewModel.proofOfIdentity.observe(viewLifecycleOwner) { method ->
            setAction(method, R.drawable.ic_identity, R.string.wallet_connect_proof_of_identity)
        }
    }

    private fun setAction(method: String, iconResource: Int, stringResource: Int) {
        requestMethod = method
        binding.walletConnectActionCard.actionIcon.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                iconResource
            )
        )
        binding.walletConnectActionCard.actionText.text = getString(stringResource)
        binding.walletConnectActionCard.root.visibility = View.VISIBLE
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
