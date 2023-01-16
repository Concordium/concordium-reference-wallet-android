package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ProofOfIdentity
import com.concordium.wallet.data.model.ProofOfIdentityStatement
import com.concordium.wallet.databinding.DialogRevealInformationBinding
import com.concordium.wallet.databinding.FragmentProofOfIdentityPromptBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel

class ProofOfIdentityPromptFragment : BaseFragment() {

    private var _binding: FragmentProofOfIdentityPromptBinding? = null
    private val binding get() = _binding!!

    private lateinit var proofOfIdentityRevealAdapter: ProofOfIdentityRevealAdapter
    private lateinit var proofOfIdentityZeroAdapter: ProofOfIdentityZeroProofAdapter
    private val viewModel: WalletConnectViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProofOfIdentityPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }
    private fun initViews() {
        viewModel.binder?.getSessionRequestParams()?.let { params ->
            val proofOfIdentity = ProofOfIdentity(
                challenge = params.challenge,
                statement = params.statement
            )
            viewModel.proofOfIdentityRequest.postValue(proofOfIdentity)
        }

        binding.subTitle.text =
            getString(R.string.proof_of_identity_prompt_subtitle, viewModel.sessionName())

        val description =
            SpannableStringBuilder().bold { append("${getString(R.string.proof_of_identity_reveal_main_description_bold)} ") }
                .append(
                    getString(
                        R.string.proof_of_identity_reveal_main_description, viewModel.sessionName()
                    )
                )

        binding.revealProof.statement.text = description

        proofOfIdentityRevealAdapter = ProofOfIdentityRevealAdapter(requireContext(), arrayOf())
        proofOfIdentityRevealAdapter.also { binding.revealProof.proofsList.adapter = it }

        proofOfIdentityZeroAdapter = ProofOfIdentityZeroProofAdapter(requireContext(), arrayOf())
        proofOfIdentityZeroAdapter.also { binding.zeroProofsList.adapter = it }

        binding.revealProof.info.setOnClickListener {

            showInfoDialog(
                title = getString(R.string.dialog_identity_proof_reveal_title),
                content = getString(R.string.dialog_identity_proof_reveal_content),
                icon = R.drawable.warning_exclamation
            )
        }

        binding.accept.setOnClickListener {
            viewModel.proofRejected.postValue(false)
            binding.accept.isEnabled = false
            viewModel.prepareProof()

        }
        binding.reject.setOnClickListener {
            viewModel.binder?.respondError("User reject")
            viewModel.reject.postValue(true)
            viewModel.proofRejected.postValue(true)
            viewModel.stepPageBy.value = 1
        }
    }

    private fun initObservers() {
        viewModel.proofOfIdentityRequest.observe(viewLifecycleOwner) {
            viewModel.validateProofOfIdentity(it)
        }
        viewModel.proofOfIdentityCheck.observe(viewLifecycleOwner) {

            it.proofsReveal?.let { reveal ->
                if (reveal.isEmpty()) {
                    binding.revealProof.root.visibility = View.GONE
                } else {
                    binding.revealProof.root.visibility = View.VISIBLE

                    if (it.revealStatus == true) {
                        loadRevealProofMeet()
                    } else {
                        loadRevealProofFailed()
                    }

                    proofOfIdentityRevealAdapter.arrayList = reveal.toTypedArray()
                    proofOfIdentityRevealAdapter.notifyDataSetChanged()
                }
            }

            it.proofsZeroKnowledge?.let { zeroProofs ->
                proofOfIdentityZeroAdapter.arrayList = zeroProofs.toTypedArray()
                proofOfIdentityZeroAdapter.notifyDataSetChanged()
            }

            _binding!!.accept.isEnabled = it.revealStatus == true && it.zeroKnowledgeStatus == true
            _binding!!.proofNotMet.visibility = if(_binding!!.accept.isEnabled) View.GONE else View.VISIBLE

        }
        viewModel.authCanceled.observe(viewLifecycleOwner) {
            if (it == true) {
                viewModel.proofOfIdentityCheck.value?.let { proofs ->
                    _binding!!.accept.isEnabled =
                        proofs.revealStatus == true && proofs.zeroKnowledgeStatus == true
                }
            }
        }
        viewModel.errorInt.observe(viewLifecycleOwner) {
            viewModel.proofOfIdentityCheck.value?.let { proofs ->
                _binding!!.accept.isEnabled =
                    proofs.revealStatus == true && proofs.zeroKnowledgeStatus == true
            }
            _binding!!.reject.isEnabled = true
        }
    }

    private fun loadRevealProofMeet() {
        binding.revealProof.header.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_top_blue)
        binding.revealProof.proofsHolder.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_bottom_transparent)
        binding.revealProof.subTitle.text = getString(R.string.proof_of_identity_meet)
        val mark = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ok)
        Glide.with(requireContext()).load(mark).fitCenter().into(binding.revealProof.proofStatus)
    }

    private fun loadRevealProofFailed() {
        binding.revealProof.header.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_top_grey)
        binding.revealProof.proofsHolder.background = ContextCompat.getDrawable(
            requireContext(), R.drawable.rounded_bottom_transparent_grey_border
        )
        binding.revealProof.subTitle.text = getString(R.string.proof_of_identity_not_meet)
        val mark = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_close_cross)
        Glide.with(requireContext()).load(mark).fitCenter().into(binding.revealProof.proofStatus)
    }

    private fun showInfoDialog(title: String, content: String, icon: Int) {
        val bind: DialogRevealInformationBinding =
            DialogRevealInformationBinding.inflate(layoutInflater)
        bind.title.text = title
        bind.content.text = content

        Glide.with(requireContext())
            .load(icon)
            .fitCenter()
            .into(bind.info)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(bind.root)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        bind.link.movementMethod = LinkMovementMethod.getInstance()

        bind.okButton.setOnClickListener {
            dialog.dismiss()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}