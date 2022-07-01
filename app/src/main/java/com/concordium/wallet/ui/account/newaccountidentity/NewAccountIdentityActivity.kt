package com.concordium.wallet.ui.account.newaccountidentity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityNewAccountIdentityBinding
import com.concordium.wallet.ui.account.newaccountsetup.NewAccountSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.uicore.dialog.Dialogs

class NewAccountIdentityActivity : BaseActivity(), Dialogs.DialogFragmentListener {
    companion object {
        const val EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME"
        const val REQUESTCODE_ERROR_DIALOG = 1000
    }

    private lateinit var binding: ActivityNewAccountIdentityBinding
    private lateinit var viewModel: NewAccountIdentityViewModel
    private var identityAdapter: IdentityAdapter = IdentityAdapter()

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountIdentityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.new_account_identity_title)

        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) as String

        initializeViewModel()
        viewModel.initialize(accountName)
        initializeViews()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountIdentityViewModel::class.java]

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.errorDialogLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorDialog(value)
            }
        })
        viewModel.identityListLiveData.observe(this, Observer { identityList ->
            identityList?.let {
                identityAdapter.setData(it)
            }
        })
    }

    private fun initializeViews() {
        binding.includeProgress.progressBar.visibility = View.GONE

        val linearLayoutManager = LinearLayoutManager(this)

        binding.identityRecyclerview.setHasFixedSize(true)
        binding.identityRecyclerview.layoutManager = linearLayoutManager
        binding.identityRecyclerview.adapter = identityAdapter

        identityAdapter.setOnItemClickListener(object :
            IdentityAdapter.OnItemClickListener {
            override fun onItemClicked(item: Identity) {
                gotoIdentityDetails(item)
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    private fun showErrorDialog(stringRes: Int) {
        dialogs.showOkDialog(
            this,
            REQUESTCODE_ERROR_DIALOG,
            R.string.new_account_identity_attributes_error_max_accounts_title,
            stringRes
        )
    }

    private fun gotoIdentityDetails(item: Identity) {
        val canCreateAccountForIdentity = viewModel.canCreateAccountForIdentity(item)
        if (!canCreateAccountForIdentity) {
            return
        }
        val intent = Intent(this, NewAccountSetupActivity::class.java)
        intent.putExtra(
            NewAccountSetupActivity.EXTRA_ACCOUNT_NAME,
            viewModel.accountName
        )
        intent.putExtra(NewAccountSetupActivity.EXTRA_IDENTITY, item)
        startActivity(intent)
    }

    //endregion
}
