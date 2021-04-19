package com.concordium.wallet.ui.identity.identityproviderpolicywebview

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_identity_provider_policy_webview.*


class IdentityProviderPolicyWebviewActivity : BaseActivity(
    R.layout.activity_identity_provider_policy_webview,
    R.string.identity_provider_webview_title
) {

    companion object {
        const val EXTRA_URL = "EXTRA_URL"
    }

    private lateinit var viewModel: IdentityProviderPolicyWebviewViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.extras!!.getString(EXTRA_URL) as String

        initializeViewModel()
        viewModel.initialize(url)
        initViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(IdentityProviderPolicyWebviewViewModel::class.java)

    }

    fun initViews() {
        webview.webViewClient = object: WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return false
            }
        }

        webview.loadUrl(viewModel.url)
    }

    //endregion

    //region Control/UI
    //************************************************************


    //endregion

}

