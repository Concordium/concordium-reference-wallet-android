package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentProofOfIdentityStatusBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel

class ProofOfIdentityStatusFragment : BaseFragment() {

    private var _binding: FragmentProofOfIdentityStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletConnectViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProofOfIdentityStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    private fun initObservers() {
        viewModel.proofRejected.observe(viewLifecycleOwner) { isRejected ->
            val icon: Int
            val description: String
            if (!isRejected) {
                icon = R.drawable.ic_big_logo_ok
                description = getString(R.string.proof_of_identity_status_success)
            } else {
                icon = R.drawable.ic_logo_icon_error
                description = getString(R.string.proof_of_identity_status_rejected)
            }

            Glide.with(requireContext())
                .load(icon)
                .fitCenter()
                .into(_binding!!.statusImage)

            _binding!!.subTitle.text = description

        }
    }

    private fun initViews() {
        _binding!!.done.setOnClickListener {
            viewModel.stepPageBy.value = -1
            if (viewModel.proofRejected.value != true) {
                viewModel.proofOfIdentityOkay.postValue(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}