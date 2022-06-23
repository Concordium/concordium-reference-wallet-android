package com.concordium.wallet.ui.transaction.sendfunds

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import kotlinx.android.synthetic.main.activity_add_memo.*


class AddMemoActivity :
    BaseActivity(R.layout.activity_add_memo, R.string.add_memo_title) {

    companion object {
        const val EXTRA_MEMO = "EXTRA_MEMO"
    }

    //region Lifecycle
    //************************************************************


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val memo = intent.getStringExtra(EXTRA_MEMO)
        memo_edittext.setText(memo)

        memo_edittext.addTextChangedListener(object : TextWatcher {
            private var previousText: String = ""
            override fun afterTextChanged(editable: Editable) {
                var change = false
                var str = editable.toString()
                var bytes = CBORUtil.encodeCBOR(str)
                change = bytes.size <= CBORUtil.MAX_BYTES
                if (!change) {
                    editable.replace(0, editable.length, previousText)
                    memo_edittext.startAnimation(AnimationUtils.loadAnimation(this@AddMemoActivity, R.anim.anim_shake))
                }
                else{
                    previousText = editable.toString()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        confirm_button.setOnClickListener {
            goBackToSendFunds()
        }
    }



    //endregion

    //region Initialize
    //************************************************************

    //endregion

    //region Control/UI
    //************************************************************



    private fun goBackToSendFunds() {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_MEMO, memo_edittext.text.toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    //endregion
}
