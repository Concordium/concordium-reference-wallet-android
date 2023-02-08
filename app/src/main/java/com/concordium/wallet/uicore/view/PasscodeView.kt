package com.concordium.wallet.uicore.view

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil
import kotlinx.android.synthetic.main.view_passcode.view.*

class PasscodeView : ConstraintLayout {

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

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.view_passcode, this)

        digitEditTextList.add(digit1_edittext)
        digitEditTextList.add(digit2_edittext)
        digitEditTextList.add(digit3_edittext)
        digitEditTextList.add(digit4_edittext)
        digitEditTextList.add(digit5_edittext)
        digitEditTextList.add(digit6_edittext)

        // Let focus go to input field when digit gets focused
        for (digitEditText in digitEditTextList) {
            digitEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    input_edittext.requestFocus()
                    KeyboardUtil.showKeyboard(context, input_edittext)
                }
            }
        }
        // Show text from input field in digit fields
        input_edittext.afterTextChanged {
            renderPasscode()
        }

        digit1_edittext.afterTextChanged {
            passcodeListener?.onInputChanged()
        }
        digit6_edittext.afterTextChanged {
            if (digit6_edittext.text.length == 1 && BULLET.equals(digit6_edittext.text.toString())) {
                passcodeListener?.onDone()
            }
        }
        digit6_edittext.setOnEditorActionListener { _, actionId, _ ->
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
        val inputLength = input_edittext.length()
        val showLastChar = inputLength > previousInputLength
        previousInputLength = inputLength
        for (i in 0..digitEditTextList.lastIndex) {
            val digitEditText = digitEditTextList[i]
            if (inputLength == i + 1 && showLastChar) {
                // For the last char show it for a short while, if it was just added
                digitEditText.setText("${input_edittext.text.get(i)}")
                renderRunnable = object : Runnable {
                    override fun run() {
                        digitEditText.setText(BULLET)
                    }
                }
                renderHandler.postDelayed(renderRunnable!!, 500)
            } else if (inputLength > i) {
                digitEditText.setText(BULLET)
            } else {
                digitEditText.setText("")
            }
        }
    }


    fun getPasscode(): String {
        return input_edittext.text.toString()
    }

    fun clearPasscode() {
        input_edittext.setText("")
    }


}