package com.concordium.wallet.ui.transaction.sendfunds

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import com.concordium.wallet.CBORUtil
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAddMemoBinding
import com.concordium.wallet.ui.base.BaseActivity

class AddMemoActivity : BaseActivity() {
    companion object {
        const val EXTRA_MEMO = "EXTRA_MEMO"
    }

    private lateinit var binding: ActivityAddMemoBinding

    //region Lifecycle
    //************************************************************
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.add_memo_title)

        val memo = intent.getStringExtra(EXTRA_MEMO)
        binding.memoEdittext.setText(memo)

        binding.memoEdittext.addTextChangedListener(object : TextWatcher {
            private var previousText: String = ""
            override fun afterTextChanged(editable: Editable) {
                var change = false
                val str = editable.toString()
                val bytes = CBORUtil.encodeCBOR(str)
                change = bytes.size <= CBORUtil.MAX_BYTES
                if (!change) {
                    editable.replace(0, editable.length, previousText)
                    binding.memoEdittext.startAnimation(AnimationUtils.loadAnimation(this@AddMemoActivity, R.anim.anim_shake))
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

        binding.confirmButton.setOnClickListener {
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
        intent.putExtra(SendFundsActivity.EXTRA_MEMO, binding.memoEdittext.text.toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    //endregion
}
