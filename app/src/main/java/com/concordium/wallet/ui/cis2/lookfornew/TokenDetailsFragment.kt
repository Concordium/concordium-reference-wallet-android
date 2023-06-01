package com.concordium.wallet.ui.cis2.lookfornew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.databinding.FragmentDialogTokenDetailsBinding
import com.concordium.wallet.ui.cis2.TokensBaseFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.UnitConvertUtil

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
                setNameAndIcon(tokenMetadata)
                setImage(tokenMetadata)
                setOwnership(tokenMetadata)
                setDescription(tokenMetadata)
                setContractIndexAndSubIndex(token)
                setDecimals(tokenMetadata)
                setTicker(tokenMetadata)
                //todo show metadata
            }
        }
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.details.tokenIdHolder.visibility = View.VISIBLE
            binding.details.tokenId.text = tokenId
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.description.isNotBlank()) {
            binding.details.descriptionHolder.visibility = View.VISIBLE
            binding.details.description.text = tokenMetadata.description
        }
    }

    private fun setOwnership(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique) {
            binding.details.ownershipHolder.visibility = View.VISIBLE
        }
    }

    private fun setNameAndIcon(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique.not()) {
            val name = tokenMetadata.name
            val thumbnail = tokenMetadata.thumbnail?.url
            binding.details.nameAndIconHolder.visibility = View.VISIBLE

            if (!thumbnail.isNullOrBlank()) {
                Glide.with(this)
                    .load(thumbnail)
                    .placeholder(R.drawable.ic_token_loading_image)
                    .override(iconSize)
                    .fitCenter()
                    .error(R.drawable.ic_token_no_image)
                    .into(binding.details.icon)
            } else if (thumbnail == "none") {
                binding.details.icon.setImageResource(R.drawable.ic_token_no_image)
            }
            binding.details.name.text = name
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
}
