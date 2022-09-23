package com.concordium.wallet.uicore

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setPadding
import com.concordium.wallet.R
import com.concordium.wallet.util.UnitConvertUtil.convertDpToPixel
import kotlin.math.ceil
import kotlin.properties.Delegates

class ButtonsSlider : RelativeLayout {
    private var buttonSizeLeftRight by Delegates.notNull<Int>()
    private var buttonSize by Delegates.notNull<Int>()
    private lateinit var cardContentContainer: RelativeLayout
    private lateinit var buttonsContainer: LinearLayout
    private lateinit var buttonLeft: AppCompatImageView
    private lateinit var buttonRight: AppCompatImageView
    private val numberOfVisibleButtons = 4
    private var currentPosition = 0
    private var buttons: ArrayList<AppCompatImageView> = arrayListOf()

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        afterMeasured {
            buttonSize = width / (numberOfVisibleButtons + 2)
            buttonSizeLeftRight = (buttonSize + ceil((width % numberOfVisibleButtons.toFloat()) / 2)).toInt()
            addCardContentContainer()
            addButtonsContainer()
            addDivider(buttonsContainer)
            addButtonLeft()
            addButtonRight()
            for (button in buttons) {
                button.layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize)
                buttonsContainer.addView(button)
                addDivider(buttonsContainer)
            }
        }
    }

    fun addButton(imageResource: Int, onClick: () -> Unit) {
        val button = AppCompatImageView(ContextThemeWrapper(context, R.style.ActionCardButtonSmall))
        button.setImageResource(imageResource)
        button.setOnClickListener {
            onClick()
        }
        button.setPadding(convertDpToPixel(10f))
        buttons.add(button)
    }

    private fun addCardContentContainer() {
        cardContentContainer = RelativeLayout(context)
        cardContentContainer.layoutParams =
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(cardContentContainer)
    }

    private fun addButtonsContainer() {
        buttonsContainer = LinearLayout(context)
        buttonsContainer.orientation = LinearLayout.HORIZONTAL
        val layoutParams = FrameLayout.LayoutParams(buttonSize * buttons.size, buttonSize)
        layoutParams.marginStart = buttonSizeLeftRight
        layoutParams.marginEnd = buttonSizeLeftRight
        buttonsContainer.layoutParams = layoutParams
        cardContentContainer.addView(buttonsContainer)
    }

    private fun addDivider(parentView: ViewGroup) {
        val divider = View(context)
        divider.layoutParams = FrameLayout.LayoutParams(2, buttonSize)
        divider.setBackgroundColor(Color.WHITE)
        //parentView.addView(divider)
    }

    private fun addButtonLeft() {
        buttonLeft = AppCompatImageView(ContextThemeWrapper(context, R.style.ActionCardButtonSmall))
        buttonLeft.layoutParams = ViewGroup.LayoutParams(buttonSizeLeftRight, buttonSize)
        buttonLeft.setImageResource(R.drawable.ic_button_back)
        buttonLeft.setPadding(convertDpToPixel(16f))
        buttonLeft.setOnClickListener {
            if (currentPosition < 0) {
                buttonRight.isEnabled = false
                currentPosition++
                val buttonsContainerLayout = buttonsContainer.layoutParams as MarginLayoutParams
                val startMargin = buttonsContainerLayout.marginStart
                val animator = ValueAnimator.ofInt(0, buttonSize)
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.duration = 100
                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Int
                    buttonsContainerLayout.marginStart = startMargin + animatedValue
                    buttonsContainer.layoutParams = buttonsContainerLayout
                }
                animator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) { }
                    override fun onAnimationCancel(animation: Animator) { }
                    override fun onAnimationRepeat(animation: Animator) { }
                    override fun onAnimationEnd(animation: Animator) {
                        buttonRight.isEnabled = true
                    }
                })
                animator.start()
            }
        }
        cardContentContainer.addView(buttonLeft)
    }

    private fun addButtonRight() {
        buttonRight = AppCompatImageView(ContextThemeWrapper(context, R.style.ActionCardButtonSmall))
        val layoutParams = LayoutParams(buttonSizeLeftRight, buttonSize)
        layoutParams.addRule(ALIGN_PARENT_END, TRUE)
        buttonRight.layoutParams = layoutParams
        buttonRight.setImageResource(R.drawable.ic_button_next)
        buttonRight.setPadding(convertDpToPixel(16f))
        buttonRight.setOnClickListener {
            if (currentPosition > numberOfVisibleButtons - buttons.size) {
                buttonLeft.isEnabled = false
                currentPosition--
                val buttonsContainerLayout = buttonsContainer.layoutParams as MarginLayoutParams
                val startMargin = buttonsContainerLayout.marginStart
                val animator = ValueAnimator.ofInt(0, buttonSize)
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.duration = 100
                animator.addUpdateListener { valueAnimator ->
                    val animatedValue = valueAnimator.animatedValue as Int
                    buttonsContainerLayout.marginStart = startMargin - animatedValue
                    buttonsContainer.layoutParams = buttonsContainerLayout
                }
                animator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) { }
                    override fun onAnimationCancel(animation: Animator) { }
                    override fun onAnimationRepeat(animation: Animator) { }
                    override fun onAnimationEnd(animation: Animator) {
                        buttonLeft.isEnabled = true
                    }
                })
                animator.start()
            }
        }
        cardContentContainer.addView(buttonRight)
    }
}
