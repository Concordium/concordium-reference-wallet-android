package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Activity
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
    private lateinit var binding: ActivityAddMemoBinding

    companion object {
        const val EXTRA_MEMO = "EXTRA_MEMO"
    }

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
                val str = editable.toString()
                val bytes = CBORUtil.encodeCBOR(str)
                val change = bytes.size <= CBORUtil.MAX_BYTES
                if (!change) {
                    editable.replace(0, editable.length, previousText)
                    binding.memoEdittext.startAnimation(AnimationUtils.loadAnimation(this@AddMemoActivity, R.anim.anim_shake))
                }
                else {
                    previousText = editable.toString()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        binding.confirmButton.setOnClickListener {
            goBackWithMemo()
        }
    }

    private fun goBackWithMemo() {
        val intent = Intent()
        intent.putExtra(EXTRA_MEMO, binding.memoEdittext.text.toString())
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
