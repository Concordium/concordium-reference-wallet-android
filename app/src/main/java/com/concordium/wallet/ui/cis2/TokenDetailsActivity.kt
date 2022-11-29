package com.concordium.wallet.ui.cis2

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
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.UnitConvertUtil
import com.concordium.wallet.util.getSerializable

class TokenDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private val viewModel: TokensViewModel by viewModels()

    private val iconSize: Int get() = UnitConvertUtil.convertDpToPixel(this.resources.getDimension(R.dimen.list_item_height))

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        viewModel.tokenData.account = intent.getSerializable(ACCOUNT, Account::class.java)
        viewModel.tokenData.selectedToken = intent.getSerializable(TOKEN, Token::class.java)
        Log.d("TOKEN : ${viewModel.tokenData.selectedToken}")


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
            startActivity(intent)
        }
        binding.includeButtons.receive.setOnClickListener {

        }

        viewModel.tokenData.selectedToken?.let {
            setContractIndex(it)
            setBalance(it)
        }

        viewModel.tokenData.selectedToken?.tokenMetadata?.let {

            setNameAndIcon(it)

            if (it.unique) {
                setImage(it)
            } else {
                setTicker(it)
                setDecimals(it)
            }
        }
    }

    private fun setBalance(token: Token) {
        if(token.totalBalance != null){
            binding.includeBalance.tokenAmount.text = CurrencyUtil.formatGTU(token.totalBalance, false)
        }
    }

    private fun setNameAndIcon(tokenMetadata: TokenMetadata) {

        val name = tokenMetadata.name
        val thumbnail = tokenMetadata.thumbnail.url
        binding.includeAbout.nameAndIconHolder.visibility = View.VISIBLE

        if (thumbnail.isNotBlank()) {
            Glide.with(this)
                .load(thumbnail)
                .placeholder(R.drawable.ic_token_loading_image)
                .override(iconSize)
                .fitCenter()
                .error(R.drawable.ic_token_no_image)
                .into(binding.includeAbout.icon)
        } else if (thumbnail == "none") {
            binding.includeAbout.icon.setImageResource(R.drawable.ic_token_no_image)
        }
        binding.includeAbout.name.text = name
    }

    private fun setContractIndex(token: Token) {

        if (token.contractIndex.isNotBlank()) {
            binding.includeAbout.contractIndexHolder.visibility = View.VISIBLE
            binding.includeAbout.contractIndex.text = token.contractIndex
        }
    }

    private fun setImage(tokenMetadata: TokenMetadata) {

        if (tokenMetadata.display?.url != null) {

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

        if (tokenMetadata.symbol != null && tokenMetadata.symbol.isNotBlank()) {
            binding.includeAbout.tokenHolder.visibility = View.VISIBLE
            binding.includeAbout.token.text = tokenMetadata.symbol
        }

    }

    private fun setDecimals(tokenMetadata: TokenMetadata) {
        binding.includeAbout.decimalsHolder.visibility = View.VISIBLE
        binding.includeAbout.decimals.text = tokenMetadata.decimals.toString()
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun lookForTokensView() {
    }
}
