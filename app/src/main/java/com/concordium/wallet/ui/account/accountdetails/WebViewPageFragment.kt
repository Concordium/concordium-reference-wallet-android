package com.concordium.wallet.ui.account.accountdetails

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentWebviewShieldingSlidePageBinding

class WebViewPageFragment(private val link: String, private val title: Int) : Fragment() {
    private var _binding: FragmentWebviewShieldingSlidePageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWebviewShieldingSlidePageBinding.inflate(inflater, container, false)
        binding.webviewContent.loadUrl(link)
        binding.title.text = Html.fromHtml(resources.getString(title))
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
