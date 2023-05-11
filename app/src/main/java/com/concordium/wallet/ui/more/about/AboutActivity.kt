package com.concordium.wallet.ui.more.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAboutBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.about_title
        )

        binding.aboutContactText.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        binding.aboutSupportText.handleUrlClicks { url ->
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(url))
            try {
                //start email intent
                startActivity(Intent.createChooser(emailIntent, ""))
            } catch (e: Exception) {
                //Left empty on purpose
            }
        }

        binding.aboutVersionText.text = getString(R.string.app_version_about, AppConfig.appVersion)
    }
}
