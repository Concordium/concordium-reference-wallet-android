package com.concordium.wallet.ui.account.accountdetails

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.concordium.wallet.R




class WebViewPageFragment(private val link: String, private val title: Int) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_webview_shielding_slide_page, container, false)
        var webView = view.findViewById<WebView>(R.id.webview_content)
        webView.loadUrl(link)
        var titleTV = view.findViewById<TextView>(R.id.title)
        titleTV.text = Html.fromHtml(resources.getString(title))
        return view
    }

}
