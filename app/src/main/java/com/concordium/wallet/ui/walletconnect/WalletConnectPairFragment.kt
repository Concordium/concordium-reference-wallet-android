package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectPairBinding
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.WALLET_CONNECT_DATA
import com.google.android.material.snackbar.Snackbar

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
                Snackbar.make(binding.root, getString(R.string.wallet_connect_timeout_dialog_content), Snackbar.LENGTH_LONG).show()
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
        binding.walletConnectStatusCard.statusTextAccount.text = _viewModel.walletConnectData.account?.name ?: ""
        binding.connect.setOnClickListener {
            binding.connect.isEnabled = false
            binding.walletConnectStatusCard.statusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_logo_icon_pending))
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
                binding.walletConnectStatusCard.statusTextService.text = serviceName
            }
        }
        _viewModel.permissions.observe(viewLifecycleOwner) { permissions ->
            permissions?.let {
                cancelTimer()
                binding.ableToSee.text = permissions.joinToString("\n") { "\u2022 $it" }
                binding.progressContainer.visibility = View.GONE
                binding.connect.isEnabled = true
            }
        }
    }
}
