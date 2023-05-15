package com.concordium.wallet.ui.more

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySettingsBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dev.DevActivity
import com.concordium.wallet.ui.passphrase.recover.ExportPassPhraseViewModel
import com.concordium.wallet.ui.passphrase.recover.ExportSeedPhraseState
import com.concordium.wallet.ui.passphrase.recover.ExportSeedPhraseActivity
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import kotlin.system.exitProcess

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private var versionNumberPressedCount = 0

    private val passPhraseViewModel: ExportPassPhraseViewModel by
    viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.more_overview_title)
        initViews()

        setUpViewModel()
    }

    private fun setUpViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                passPhraseViewModel.state.collect { state ->
                    when (state) {
                        is ExportSeedPhraseState.Success ->
                            binding.viewSeedPhraseLayout.visibility = View.VISIBLE

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun initViews() {
        binding.devLayout.visibility = View.GONE
        binding.devLayout.setOnClickListener {
            gotoDevConfig()
        }

        if (BuildConfig.INCL_DEV_OPTIONS) {
            binding.devLayout.visibility = View.VISIBLE
        }

        binding.identities.setOnClickListener {
            gotoIdentities()
        }

        binding.addressBookLayout.setOnClickListener {
            gotoAddressBook()
        }

        binding.recoverLayout.setOnClickListener {
            recover()
        }

        binding.viewSeedPhraseLayout.setOnClickListener {
            showSeedPhrase()
        }

        binding.aboutLayout.setOnClickListener {
            about()
        }

        binding.alterLayout.setOnClickListener {
            alterPassword()
        }

        binding.walletConnectLayout.setOnClickListener {
            clearWalletConnectAndRestart()
        }

        initializeAppVersion()
    }

    private fun clearWalletConnectAndRestart() {
        showConfirmDeleteWalletConnect()
    }

    private fun showConfirmDeleteWalletConnect() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.wallet_connect_clear_data_warning_title)
        builder.setMessage(getString(R.string.wallet_connect_clear_data_warning_message))
        builder.setPositiveButton(getString(R.string.wallet_connect_clear_data_warning_ok)) { _, _ ->
            deleteWCDatabaseAndRestart()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_clear_data_warning_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun deleteWCDatabaseAndRestart() {
        try {
            val path = dataDir.absolutePath
            File("$path/databases/WalletConnectAndroidCore.db").delete()
            File("$path/databases/WalletConnectV2.db").delete()
            restartApp()
        } catch (ex: Exception) {
            println(ex.stackTraceToString())
        }
    }

    private fun restartApp () {
        try {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(applicationContext,9999, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager[AlarmManager.RTC, System.currentTimeMillis() + 100] = pendingIntent
            exitProcess(0)
        } catch (ex: Exception) {
            println(ex.stackTraceToString())
        }
    }

    private fun gotoDevConfig() {
        startActivity(Intent(this, DevActivity::class.java))
    }

    private fun gotoIdentities() {
        startActivity(Intent(this, IdentitiesOverviewActivity::class.java))
    }

    private fun gotoAddressBook() {
        startActivity(Intent(this, RecipientListActivity::class.java))
    }

    private fun recover() {
        val intent = Intent(this, RecoverProcessActivity::class.java)
        intent.putExtra(RecoverProcessActivity.SHOW_FOR_FIRST_RECOVERY, false)
        startActivity(intent)
    }

    private fun showSeedPhrase() {
        startActivity(Intent(this, ExportSeedPhraseActivity::class.java))
    }

    private fun about() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    private fun alterPassword() {
        startActivity(Intent(this, AlterPasswordActivity::class.java))
    }

    private fun initializeAppVersion() {
        binding.versionTextview.text = getString(R.string.app_version, AppConfig.appVersion)
        binding.versionTextview.setOnClickListener {
            versionNumberPressedCount++
            if (versionNumberPressedCount >= 5) {
                Toast.makeText(
                    this,
                    "Build " + BuildConfig.BUILD_NUMBER + "." + BuildConfig.BUILD_TIME_TICKS,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}