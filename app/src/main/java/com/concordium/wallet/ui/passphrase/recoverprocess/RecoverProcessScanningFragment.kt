package com.concordium.wallet.ui.passphrase.recoverprocess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.databinding.FragmentRecoverProcessScanningBinding

class RecoverProcessScanningFragment : RecoverProcessBaseFragment() {
    private var _binding: FragmentRecoverProcessScanningBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(viewModel: RecoverProcessViewModel) = RecoverProcessScanningFragment().apply {
            arguments = Bundle().apply {
                putSerializable(RecoverProcessViewModel.RECOVER_PROCESS_DATA, viewModel)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecoverProcessScanningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.startScanning()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
