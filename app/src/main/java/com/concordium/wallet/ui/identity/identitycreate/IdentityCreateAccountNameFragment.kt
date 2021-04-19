package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil
import kotlinx.android.synthetic.main.fragment_identity_create_account_name.*

class IdentityCreateAccountNameFragment : BaseFragment(R.string.identity_create_title) {

    private lateinit var sharedViewModel: IdentityCreateViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        sharedViewModel.initialize()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_identity_create_account_name, container, false)
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
        account_name_edittext.afterTextChanged { text ->
            confirm_button.isEnabled = !text.isNullOrEmpty()
        }

        account_name_edittext.setOnEditorActionListener { textView, actionId, _ ->
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
        if (!ValidationUtil.validateName(account_name_edittext.text.toString())) {
            account_name_edittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }
        sharedViewModel.customAccountName = account_name_edittext.text.toString().trim()
        findNavController().navigate(IdentityCreateAccountNameFragmentDirections.actionNavIdentityCreateAccountNameToNavIdentityCreateIdentityName())
    }

    //endregion

}