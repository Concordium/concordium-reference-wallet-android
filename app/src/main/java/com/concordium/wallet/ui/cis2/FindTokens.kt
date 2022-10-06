package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.widget.TextView
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class FindTokens(private var activity: Activity) {
    private val dialog = BottomSheetDialog(activity)

    fun test() {
        dialog.setContentView(R.layout.dialog_token_details)
        val title = dialog.findViewById<TextView>(R.id.title)
        title?.text = activity.getString(R.string.cis_find_tokens_title)
    }

    fun show() {
        dialog.show()
    }
}


/*
class FindTokensActivity : BaseActivity() {
    private lateinit var binding: ActivityFindTokensBinding
    private lateinit var viewModel: TokensViewModel

    companion object {
        const val ACCOUNT_ADDRESS = "ACCOUNT_ADDRESS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindTokensBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initializeViewModel()
        initObservers()
        lookForTokensView()
    }

    private fun initViews() {
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.cis_find_tokens_title)
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
