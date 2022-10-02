package com.concordium.wallet.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivitySettingsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dev.DevActivity
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private var versionNumberPressedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.more_overview_title)
        initViews()
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

        binding.aboutLayout.setOnClickListener {
            about()
        }

        binding.alterLayout.setOnClickListener {
            alterPassword()
        }

        initializeAppVersion()
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