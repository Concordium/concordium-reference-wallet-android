package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.UnitConvertUtil
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger

class TokenDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private val viewModel: TokensViewModel by viewModels()
    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(this.resources.getDimension(R.dimen.list_item_height))

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val DELETED = "DELETED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initObservers()
    }

    private fun initViews() {
        viewModel.tokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.tokenData.selectedToken = intent.getSerializable(TOKEN, Token::class.java)
        Log.d("TOKEN : ${viewModel.tokenData}")
        Log.d("ACCOUNT : ${viewModel.tokenData.account}")

        val tokenName = viewModel.tokenData.selectedToken?.tokenMetadata?.name
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.app_name
        )
        setActionBarTitle(
            getString(
                R.string.cis_token_details_title,
                tokenName,
                viewModel.tokenData.account?.name
            )
        )

        binding.includeButtons.send.setOnClickListener {
            val intent = Intent(this, SendTokenActivity::class.java)
            intent.putExtra(SendTokenActivity.ACCOUNT, viewModel.tokenData.account)
            intent.putExtra(SendTokenActivity.TOKEN, viewModel.tokenData.selectedToken)
            intent.putExtra(SendTokenActivity.SEND_ONLY_SELECTED_TOKEN, true)
            startActivity(intent)
        }
        binding.includeButtons.receive.setOnClickListener {
            val intent = Intent(this, AccountQRCodeActivity::class.java)
            intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModel.tokenData.account)
            startActivity(intent)
        }

        binding.includeAbout.deleteToken.setOnClickListener {
            showDeleteDialog()
        }

        viewModel.tokenData.selectedToken?.let { token ->
            setContractIndexAndSubIndex(token)
            setTokenId(token.token)
            token.tokenMetadata?.let { tokenMetadata ->
                setName(tokenMetadata)
                setImage(tokenMetadata)
                setOwnership(token, tokenMetadata)
                setDescription(tokenMetadata)
                setTicker(tokenMetadata)
                setDecimals(tokenMetadata)
                setMetadataDialog(
                    tokenMetadata,
                    binding.showRawMetadataDialogRoot,
                    binding.showRawMetadataDialogContainer,
                    binding.includeAbout.showRawMetadataHolder
                )
            }
        }
    }

    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.cis_delete_dialog_title)
        builder.setMessage(getString(R.string.cis_delete_dialog_content))
        builder.setPositiveButton(getString(R.string.cis_delete_dialog_confirm)) { dialog, _ ->
            dialog.dismiss()
            viewModel.deleteSingleToken(
                viewModel.tokenData.account!!.address,
                viewModel.tokenData.selectedToken!!.contractIndex,
                viewModel.tokenData.selectedToken!!.id
            )
            val intent = Intent()
            intent.putExtra(DELETED, true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        builder.setNegativeButton(getString(R.string.cis_delete_dialog_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.includeAbout.tokenIdHolder.visibility = View.VISIBLE
            binding.includeAbout.tokenId.text = tokenId
        }
    }

    private fun setDescription(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.description.isNotBlank()) {
            binding.includeAbout.descriptionHolder.visibility = View.VISIBLE
            binding.includeAbout.description.text = tokenMetadata.description
        }
    }

    private fun setOwnership(token: Token, tokenMetadata: TokenMetadata) {
        binding.includeBalance.tokenAmount.text =
            CurrencyUtil.formatGTU(token.totalBalance, false, token.tokenMetadata?.decimals ?: 0)
        binding.includeAbout.balanceHolder.visibility = View.GONE
        if (tokenMetadata.unique) {
            binding.includeAbout.ownershipHolder.visibility = View.VISIBLE
            binding.includeAbout.ownership.text = if (token.totalBalance != BigInteger.ZERO)
                getString(R.string.cis_owned)
            else
                getString(R.string.cis_not_owned)
        }
    }

    private fun setContractIndexAndSubIndex(token: Token) {
        val tokenIndex = token.contractIndex
        if (tokenIndex.isNotBlank()) {
            binding.includeAbout.contractIndexHolder.visibility = View.VISIBLE
            binding.includeAbout.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.includeAbout.contractIndex.text = combinedInfo
            } else {
                binding.includeAbout.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setName(tokenMetadata: TokenMetadata) {
        binding.includeAbout.nameAndIconHolder.visibility = View.VISIBLE
        binding.includeAbout.name.text = tokenMetadata.name
    }

    private fun setImage(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.display?.url.isNullOrBlank()) {
            binding.includeAbout.imageHolder.visibility = View.VISIBLE
            Glide.with(this)
                .load(tokenMetadata.display?.url)
                .placeholder(R.drawable.ic_token_loading_image)
                .override(iconSize)
                .fitCenter()
                .error(R.drawable.ic_token_no_image)
                .into(binding.includeAbout.image)
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.includeAbout.tokenHolder.visibility = View.VISIBLE
            binding.includeAbout.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique.not()) {
            binding.includeAbout.decimalsHolder.visibility = View.VISIBLE
            binding.includeAbout.decimals.text = tokenMetadata.decimals.toString()
        } else {
            binding.includeAbout.decimalsHolder.visibility = View.GONE
            binding.includeAbout.decimals.text = ""
        }
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
