package com.concordium.wallet.uicore.view

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.databinding.ViewPasscodeBinding
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class PasscodeView : ConstraintLayout {
    private val binding = ViewPasscodeBinding.inflate(LayoutInflater.from(context), this, true)

    companion object {
        const val BULLET = "\u2022"
    }

    private var digitEditTextList: ArrayList<EditText> = ArrayList()
    private var previousInputLength = 0
    private val renderHandler = Handler()
    private var renderRunnable: Runnable? = null

    interface PasscodeListener {
        fun onInputChanged()
        fun onDone()
    }

    var passcodeListener: PasscodeListener? = null

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        digitEditTextList.add(binding.digit1Edittext)
        digitEditTextList.add(binding.digit2Edittext)
        digitEditTextList.add(binding.digit3Edittext)
        digitEditTextList.add(binding.digit4Edittext)
        digitEditTextList.add(binding.digit5Edittext)
        digitEditTextList.add(binding.digit6Edittext)

        // Let focus go to input field when digit gets focused
        for (digitEditText in digitEditTextList) {
            digitEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.inputEdittext.requestFocus()
                    KeyboardUtil.showKeyboard(context, binding.inputEdittext)
                }
            }
        }
        // Show text from input field in digit fields
        binding.inputEdittext.afterTextChanged {
            renderPasscode()
        }

        binding.digit1Edittext.afterTextChanged {
            passcodeListener?.onInputChanged()
        }
        binding.digit6Edittext.afterTextChanged {
            if (binding.digit6Edittext.text.length == 1 && BULLET == binding.digit6Edittext.text.toString()) {
                passcodeListener?.onDone()
            }
        }
        binding.digit6Edittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    passcodeListener?.onDone()
                    true
                }

                else -> false
            }
        }
    }

    private fun renderPasscode() {
        val runnable = renderRunnable
        runnable?.let {
            renderHandler.removeCallbacks(it)
        }
        val inputLength = binding.inputEdittext.length()
        val showLastChar = inputLength > previousInputLength
        previousInputLength = inputLength
        for (i in 0..digitEditTextList.lastIndex) {
            val digitEditText = digitEditTextList[i]
            if (inputLength == i + 1 && showLastChar) {
                // For the last char show it for a short while, if it was just added
                digitEditText.setText("${binding.inputEdittext.text[i]}")
                renderRunnable = Runnable { digitEditText.setText(BULLET) }
                renderHandler.postDelayed(renderRunnable!!, 500)
            } else if (inputLength > i) {
                digitEditText.setText(BULLET)
            } else {
                digitEditText.setText("")
            }
        }
    }


    fun getPasscode(): String {
        return binding.inputEdittext.text.toString()
    }

    fun clearPasscode() {
        binding.inputEdittext.setText("")
    }
}
