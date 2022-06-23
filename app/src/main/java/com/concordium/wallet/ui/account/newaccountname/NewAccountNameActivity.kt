package com.concordium.wallet.ui.account.newaccountname

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.newaccountidentity.NewAccountIdentityActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.ValidationUtil
import kotlinx.android.synthetic.main.activity_new_account_name.*

class NewAccountNameActivity() :
    BaseActivity(R.layout.activity_new_account_name, R.string.new_account_name_title) {

    private lateinit var viewModel: NewAccountNameViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(NewAccountNameViewModel::class.java)
    }

    private fun initViews() {
        next_button.setOnClickListener {
            gotoIdentityList()
        }

        account_name_edittext.afterTextChanged { text ->
            next_button.isEnabled = !text.isNullOrEmpty()
        }

        account_name_edittext.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (textView.text.isNotEmpty())
                        gotoIdentityList()
                    true
                }
                else -> false
            }
        }


    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoIdentityList() {
        if (!ValidationUtil.validateName(account_name_edittext.text.toString())) {
            account_name_edittext.error = getString(R.string.valid_special_chars_error_text)
            return
        }

        val intent = Intent(this, NewAccountIdentityActivity::class.java)
        intent.putExtra(
            NewAccountIdentityActivity.EXTRA_ACCOUNT_NAME,
            account_name_edittext.text.toString()
        )
        startActivity(intent)
    }

    //endregion
}
