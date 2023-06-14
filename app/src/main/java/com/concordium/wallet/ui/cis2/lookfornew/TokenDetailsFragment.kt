package com.concordium.wallet.ui.cis2.lookfornew

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentDialogTokenDetailsBinding
import com.concordium.wallet.ui.cis2.TokensBaseFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject

class TokenDetailsFragment : TokensBaseFragment() {
    private var _binding: FragmentDialogTokenDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var _viewModel: TokensViewModel
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(this.resources.getDimension(R.dimen.list_item_height))

    companion object {
        @JvmStatic
        fun newInstance(viewModel: TokensViewModel) = TokenDetailsFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TokensViewModel.TOKEN_DATA, viewModel.tokenData)
            }
            _viewModel = viewModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogTokenDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.details.deleteToken.visibility = View.GONE
        binding.backToList.setOnClickListener {
            _viewModel.stepPage(-1)
        }
        _viewModel.chooseTokenInfo.observe(viewLifecycleOwner) { token ->
            setTokenId(token.token)
            token.tokenMetadata?.let { tokenMetadata ->
                setName(tokenMetadata)
                setImage(tokenMetadata)
                setBalance(token, tokenMetadata)
                setDescription(tokenMetadata)
                setContractIndexAndSubIndex(token)
                setDecimals(tokenMetadata)
                setTicker(tokenMetadata)
                showMatadata(tokenMetadata)
            }
        }
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.details.tokenIdHolder.visibility = View.VISIBLE
            binding.details.tokenId.text = tokenId
        }
    }

    private fun setName(tokenMetadata: TokenMetadata) {
        binding.details.nameAndIconHolder.visibility = View.VISIBLE
        binding.details.name.text = tokenMetadata.name
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.description.isNotBlank()) {
            binding.details.descriptionHolder.visibility = View.VISIBLE
            binding.details.description.text = tokenMetadata.description
        }
    }

    private fun setBalance(token: Token, tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique) {
            binding.details.balanceHolder.visibility = View.GONE
            binding.details.ownershipHolder.visibility = View.VISIBLE
            binding.details.ownership.text = if (token.totalBalance != BigInteger.ZERO)
                getString(R.string.cis_owned)
            else
                getString(R.string.cis_not_owned)
        } else {
            binding.details.ownershipHolder.visibility = View.GONE
            binding.details.balanceHolder.visibility = View.VISIBLE
            binding.details.balance.text =
                CurrencyUtil.formatGTU(token.totalBalance, false, tokenMetadata.decimals)
        }
    }

    private fun setContractIndexAndSubIndex(token: Token) {
        val tokenIndex = token.contractIndex

        if (tokenIndex.isNotBlank()) {
            binding.details.contractIndexHolder.visibility = View.VISIBLE
            binding.details.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.details.contractIndex.text = combinedInfo
            } else {
                binding.details.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setImage(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.display?.url.isNullOrBlank()) {
            binding.details.imageHolder.visibility = View.VISIBLE

            Glide.with(this)
                .load(tokenMetadata.display?.url)
                .placeholder(R.drawable.ic_token_loading_image)
                .override(iconSize)
                .fitCenter()
                .error(R.drawable.ic_token_no_image)
                .into(binding.details.image)
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.details.tokenHolder.visibility = View.VISIBLE
            binding.details.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique.not()) {
            binding.details.decimalsHolder.visibility = View.VISIBLE
            binding.details.decimals.text = tokenMetadata.decimals.toString()
        }
    }

    private fun showMatadata(tokenMetadata: TokenMetadata) {
        binding.details.showRawMetadataHolder.setOnClickListener {
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(gson.toJson(tokenMetadata))
            builder.setPositiveButton(getString(R.string.error_database_close)) { _, _ -> }
            builder.setCancelable(true)
            builder.create().show()
        }
    }
}
