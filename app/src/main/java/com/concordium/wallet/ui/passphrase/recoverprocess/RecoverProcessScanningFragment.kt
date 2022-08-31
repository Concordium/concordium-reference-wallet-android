package com.concordium.wallet.ui.passphrase.recoverprocess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentRecoverProcessScanningBinding
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessViewModel.Companion.RECOVER_PROCESS_DATA

class RecoverProcessScanningFragment : RecoverProcessBaseFragment() {
    private var _binding: FragmentRecoverProcessScanningBinding? = null
    private val binding get() = _binding!!
    private lateinit var _password: String
    private lateinit var _viewModel: RecoverProcessViewModel

    companion object {
        @JvmStatic
        fun newInstance(viewModel: RecoverProcessViewModel, recoverProcessData: RecoverProcessData, password: String) = RecoverProcessScanningFragment().apply {
            arguments = Bundle().apply {
                putSerializable(RECOVER_PROCESS_DATA, recoverProcessData)
            }
            _viewModel = viewModel
            _password = password
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecoverProcessScanningBinding.inflate(inflater, container, false)
        initObservers()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        _viewModel.recoverIdentitiesAndAccounts(_password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initObservers() {
        _viewModel.waiting.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        _viewModel.progress.observe(viewLifecycleOwner) { progress ->
            progress?.let {
                binding.progressBar.progress = progress
            }
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
