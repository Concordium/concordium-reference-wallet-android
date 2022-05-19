package com.concordium.wallet.ui.more.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : BaseActivity(
    R.layout.activity_about,
    R.string.about_title
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        about_contact_text.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        about_support_text.handleUrlClicks { url ->
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(url))
            try {
                //start email intent
                startActivity(Intent.createChooser(emailIntent, ""))
            }
            catch (e: Exception){
                //Left empty on purpose
            }
        }

        about_version_text.text = getString(R.string.app_version_about, AppConfig.appVersion)
    }
}
