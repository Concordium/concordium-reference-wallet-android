package com.concordium.wallet.ui.more.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : BaseActivity(
    R.layout.activity_about,
    R.string.about_title
) {

    //region Lifecycle
    //************************************************************

    fun TextView.handleUrlClicks(onClicked: ((String) -> Unit)? = null) {
        //create span builder and replaces current text with it
        text = SpannableStringBuilder.valueOf(text).apply {
            //search for all URL spans and replace all spans with our own clickable spans
            getSpans(0, length, URLSpan::class.java).forEach {
                //add new clickable span at the same position
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onClicked?.invoke(it.url)
                        }
                    },
                    getSpanStart(it),
                    getSpanEnd(it),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
                //remove old URLSpan
                removeSpan(it)
            }
        }
        //make sure movement method is set
        movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        about_support_text.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        about_contact_text.handleUrlClicks { url ->
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
    }

    //endregion

}
