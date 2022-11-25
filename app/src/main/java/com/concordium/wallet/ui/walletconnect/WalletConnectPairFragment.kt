package com.concordium.wallet.ui.walletconnect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectPairBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA

class WalletConnectPairFragment : WalletConnectBaseFragment() {
    private var _binding: FragmentWalletConnectPairBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: WalletConnectViewModel

    private var countDownTimer: CountDownTimer? = null

    companion object {

        private const val TIMER_VALUE = 15000L

        @JvmStatic
        fun newInstance(viewModel: WalletConnectViewModel) = WalletConnectPairFragment().apply {
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
        _binding = FragmentWalletConnectPairBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        if (savedInstanceState == null) {
            _viewModel.pair()
        }
        countDownTimer = object : CountDownTimer(TIMER_VALUE, 1000) {
            override fun onTick(millisecondFinished: Long) {
            }
            override fun onFinish() {
               showTimeOut()
            }
        }.start()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelTimer()
        _binding = null
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun initViews() {
        binding.accountName.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.connect.setOnClickListener {
            binding.connect.isEnabled = false
            _viewModel.connect.postValue(true)
        }
        binding.decline.setOnClickListener {
            cancelTimer()
            binding.decline.isEnabled = false
            _viewModel.rejectSession()
            _viewModel.decline.postValue(true)
        }
    }

    private fun initObservers() {
        _viewModel.serviceName.observe(viewLifecycleOwner) { serviceName ->
            serviceName?.let {
                binding.serviceName.text = serviceName
            }
        }
        _viewModel.permissions.observe(viewLifecycleOwner) { permissions ->
            permissions?.let {
                cancelTimer()
                binding.servicePermissions.text = permissions.joinToString("\n") { it }
                binding.progressContainer.visibility = View.GONE
                binding.doYouWantToOpen.visibility = View.VISIBLE
                binding.connect.isEnabled = true
            }
        }
    }

    private fun showTimeOut() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.wallet_connect_timeout_dialog_title)
        builder.setMessage(getString(R.string.wallet_connect_timeout_dialog_content))

        builder.setNegativeButton(getString(R.string.wallet_connect_connection_lost_okay)) {dialog, _ ->
            dialog.dismiss()
            gotoMain()
        }
        builder.setCancelable(false)
        builder.create().show()
    }

    private fun gotoMain() {
        activity?.finish()
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
