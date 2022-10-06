package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.widget.TextView
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class TokenDetails(private var activity: Activity) {
    private val dialog = BottomSheetDialog(activity)

    fun test(tokenName: String, accountName: String) {
        dialog.setContentView(R.layout.dialog_token_details)
        val title = dialog.findViewById<TextView>(R.id.title)
        title?.text = activity.getString(R.string.cis_token_details_title, tokenName, accountName)
    }

    fun show() {
        dialog.show()
    }
}

/*
class TokenDetailsActivity : BottomSheetDialog() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private lateinit var viewModel: TokensViewModel

    companion object {
        const val TOKEN_NAME = "TOKEN_NAME"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initializeViewModel()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        val tokenName = intent.extras!!.getString(TOKEN_NAME)
        val accountName = intent.extras!!.getString(ACCOUNT_NAME)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.app_name)
        setActionBarTitle(getString(R.string.cis_token_details_title, tokenName, accountName))
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]
    }

    private fun initObservers() {
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun lookForTokensView() {

    }
}
*/