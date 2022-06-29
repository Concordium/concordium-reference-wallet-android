package com.concordium.wallet.ui.identity.identityconfirmed

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentityConfirmedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs

class IdentityConfirmedActivity : BaseActivity(), Dialogs.DialogFragmentListener {
    companion object {
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
    }

    private lateinit var binding: ActivityIdentityConfirmedBinding
    private lateinit var viewModel: IdentityConfirmedViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityConfirmedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identity_confirmed_title)

        val identity = intent.extras!!.getSerializable(EXTRA_IDENTITY) as Identity

        initializeViewModel()
        viewModel.initialize(identity)
        // This observe has to be done after the initialize, where the live data is set up in the view model
        viewModel.accountWithIdentityLiveData.observe(this, Observer<AccountWithIdentity> { accountWithIdentity ->
            accountWithIdentity?.let {
                binding.identityView.setIdentityData(accountWithIdentity.identity)
                binding.accountView.setAccount(accountWithIdentity)
                binding.accountView.isEnabled = accountWithIdentity.identity.status == IdentityStatus.DONE
            }
        })
        initializeViews()
        // If we're being restored from a previous state
        if (savedInstanceState != null) {
            return
        }
        viewModel.startIdentityUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                // Just go back to the identityProvider list to try again
                finish()
            }
        }
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityConfirmedViewModel::class.java]

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.isFirstIdentityLiveData.observe(this, Observer<Boolean> { isFirst ->
            isFirst?.let {
                updateInfoText(isFirst)
            }
        })

        viewModel.identityErrorLiveData.observe(this, Observer<IdentityErrorData> { data ->
            data?.let {
                IdentityErrorDialogHelper.showIdentityError(this, dialogs, data)
            }
        })

        viewModel.newFinalizedAccountLiveData.observe(this, Observer<String> { newAccount ->
            newAccount?.let {
                CustomDialogFragment.newAccountFinalizedDialog(this, newAccount)
            }
        })
    }

    private fun initializeViews() {
        hideActionBarBack(this)
        showWaiting(true)

        binding.confirmButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.identity_confirmed_alert_dialog_title))
            builder.setMessage(getString(R.string.identity_confirmed_alert_dialog_text))
            builder.setPositiveButton(getString(R.string.identity_confirmed_alert_dialog_ok), object: DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which:Int) {
                    gotoIdentityOverview()
                }
            })
            builder.setCancelable(true)
            builder.create().show()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoIdentityOverview() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(MainActivity.EXTRA_SHOW_IDENTITIES, true)
        startActivity(intent)
    }

    private fun updateInfoText(isFirstIdentity: Boolean) {
        if (isFirstIdentity) {
            binding.infoTextview.setText(R.string.identity_confirmed_info_first)
        } else {
            binding.infoTextview.setText(R.string.identity_confirmed_info)
        }
    }

    //endregion
}
