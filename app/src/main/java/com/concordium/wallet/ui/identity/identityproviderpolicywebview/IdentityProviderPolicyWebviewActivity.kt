package com.concordium.wallet.ui.identity.identityproviderpolicywebview

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.databinding.ActivityIdentityProviderPolicyWebviewBinding
import com.concordium.wallet.ui.base.BaseActivity

class IdentityProviderPolicyWebviewActivity : BaseActivity() {
    companion object {
        const val EXTRA_URL = "EXTRA_URL"
    }

    private lateinit var binding: ActivityIdentityProviderPolicyWebviewBinding
    private lateinit var viewModel: IdentityProviderPolicyWebViewViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityProviderPolicyWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        )[IdentityProviderPolicyWebViewViewModel::class.java]
    }

    fun initViews() {
        binding.webview.webViewClient = object: WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return false
            }
        }

        binding.webview.loadUrl(viewModel.url)
    }

    //endregion
}
