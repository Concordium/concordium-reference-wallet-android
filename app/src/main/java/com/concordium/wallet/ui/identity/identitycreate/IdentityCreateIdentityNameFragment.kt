package com.concordium.wallet.ui.identity.identitycreate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil
import kotlinx.android.synthetic.main.fragment_identity_create_identity_name.*

class IdentityCreateIdentityNameFragment : BaseFragment(R.string.identity_create_title) {

    private lateinit var sharedViewModel: IdentityCreateViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        sharedViewModel.initialize()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_identity_create_identity_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        sharedViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(IdentityCreateViewModel::class.java)

    }

    private fun initializeViews() {
        confirm_button.isEnabled = false
        confirm_button.setOnClickListener {
            gotoIdentityName()
        }
        identity_name_edittext.afterTextChanged { text ->
            confirm_button.isEnabled = !text.isNullOrEmpty()
        }

        identity_name_edittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty())
                        gotoIdentityName()
                    true
                }
                else -> false
            }
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoIdentityName() {
        if (!ValidationUtil.validateName(identity_name_edittext.text.toString())) {
            identity_name_edittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }
        sharedViewModel.customAccountName?.let { customAccountName ->
            val intent = Intent(requireContext(), IdentityProviderListActivity::class.java)
            intent.putExtra(IdentityProviderListActivity.EXTRA_IDENTITY_CUSTOM_NAME, identity_name_edittext.text.toString().trim())
            intent.putExtra(IdentityProviderListActivity.EXTRA_ACCOUNT_CUSTOM_NAME, customAccountName)
            startActivity(intent)
        }

    }

    //endregion

}