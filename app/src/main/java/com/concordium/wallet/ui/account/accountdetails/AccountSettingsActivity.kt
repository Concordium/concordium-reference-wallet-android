package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountSettingsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.export.ExportAccountKeysActivity
import com.concordium.wallet.ui.more.export.ExportTransactionLogActivity
import com.concordium.wallet.uicore.setEditText
import com.concordium.wallet.util.getSerializable

class AccountSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityAccountSettingsBinding
    private lateinit var viewModel: AccountSettingsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_CONTINUE_TO_SHIELD_INTRO = "EXTRA_CONTINUE_TO_SHIELD_INTRO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.account_settings_title)
        initializeViewModel()
        viewModel.initialize(intent.getSerializable(EXTRA_ACCOUNT, Account::class.java),
            intent.getBooleanExtra(EXTRA_SHIELDED, false))
        initViews()
        initObservers()
        val continueToShieldIntro = intent.extras!!.getBoolean(EXTRA_CONTINUE_TO_SHIELD_INTRO)
        if (continueToShieldIntro) {
            startShieldedIntroFlow()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountSettingsViewModel::class.java]
    }

    private fun initObservers() {
        viewModel.accountUpdated.observe(this) {

        }
        viewModel.shieldingEnabledLiveData.observe(this) {
            initViews()
        }
    }

    private fun initViews() {
        binding.transferFilter.setOnClickListener {
            gotoTransferFilters(viewModel.account)
        }
        binding.showShielded.setOnClickListener {
            startShieldedIntroFlow()
        }
        binding.hideShielded.setOnClickListener {
            viewModel.disableShielded()
            finish()
        }
        binding.releaseSchedule.setOnClickListener {
            gotoAccountReleaseSchedule(viewModel.account, viewModel.isShielded)
        }
        binding.exportKey.setOnClickListener {
            exportKey()
        }
        binding.exportTransactionLog.setOnClickListener {
            exportTransactionLog()
        }
        binding.changeName.setOnClickListener {
            showChangeNameDialog()
        }

        binding.showShielded.visibility = if (viewModel.shieldingEnabledLiveData.value == true || viewModel.account.readOnly) View.GONE else View.VISIBLE
        binding.dividerShowShielded.visibility = if (viewModel.shieldingEnabledLiveData.value == true || viewModel.account.readOnly) View.GONE else View.VISIBLE
        binding.hideShielded.visibility = if (viewModel.shieldingEnabledLiveData.value == true && !viewModel.account.readOnly) View.VISIBLE else View.GONE
        binding.dividerHideShielded.visibility = if (viewModel.shieldingEnabledLiveData.value == true && !viewModel.account.readOnly) View.VISIBLE else View.GONE
        binding.transferFilter.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
        binding.dividerTransferFilter.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
        binding.releaseSchedule.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
        binding.dividerReleaseSchedule.visibility = if (viewModel.isShielded) View.GONE else View.VISIBLE
    }

    private fun gotoTransferFilters(account: Account) {
        val intent = Intent(this, AccountTransactionsFiltersActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoAccountReleaseSchedule(account: Account, isShielded: Boolean) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, account)
        intent.putExtra(AccountDetailsActivity.EXTRA_SHIELDED, isShielded)
        startActivity(intent)
    }

    private fun exportKey() {
        val intent = Intent(this, ExportAccountKeysActivity::class.java)
        intent.putExtra(ExportAccountKeysActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun exportTransactionLog() {
        val intent = Intent(this, ExportTransactionLogActivity::class.java)
        intent.putExtra(ExportTransactionLogActivity.EXTRA_ACCOUNT, viewModel.account)
        startActivity(intent)
    }

    private fun showChangeNameDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.account_details_change_name_popup_title))
        builder.setMessage(getString(R.string.account_details_change_name_popup_subtitle))
        val input = AppCompatEditText(this)
        input.hint = viewModel.account.name
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setEditText(this, input)
        builder.setPositiveButton(getString(R.string.account_details_change_name_popup_save)) { _, _ ->
            viewModel.changeAccountName(input.text.toString())
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun startShieldedIntroFlow() {
        val intent = Intent(this, ShieldingIntroActivity::class.java)
        getResultEnableShielding.launch(intent)
    }

    private val getResultEnableShielding =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getBooleanExtra(ShieldingIntroActivity.EXTRA_RESULT_SHIELDING_ENABLED, false)?.let { enabled ->
                    if (enabled) {
                        viewModel.enableShielded()
                        viewModel.isShielded = true
                        finish()
                    }
                }
            }
        }
}
