package com.concordium.wallet.uicore

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.RelativeLayout.ALIGN_PARENT_END
import android.widget.RelativeLayout.TRUE
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.view.forEach
import androidx.core.view.setPadding
import com.concordium.wallet.R
import com.concordium.wallet.util.UnitConvertUtil.convertDpToPixel
import com.concordium.wallet.util.roundUpToInt
import kotlin.properties.Delegates

class ButtonsSlider : CardView {
    private lateinit var cardContentContainer: RelativeLayout
    private lateinit var buttonsContainer: LinearLayout
    private lateinit var buttonLeft: AppCompatImageView
    private lateinit var buttonRight: AppCompatImageView
    private var widthLeftRightButton by Delegates.notNull<Float>()
    private var widthEachActionButton by Delegates.notNull<Float>()
    private val dividerWidth: Float = 3f
    private val numberOfVisibleButtons: Float = 4f
    private var currentPosition = 0
    private var buttons: ArrayList<AppCompatImageView> = arrayListOf()
    private var measured = false

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
            val widthWithoutDividers = width - ((numberOfVisibleButtons + 1) * dividerWidth)
            widthEachActionButton = widthWithoutDividers / (numberOfVisibleButtons + 2)
            widthLeftRightButton = widthEachActionButton + ((widthWithoutDividers % (numberOfVisibleButtons + 2)) / 2)
            addCardContentContainer()
            addButtonsContainer()
            addButtonLeft()
            addButtonRight()
            measured = true
            commitButtons()
        }
    }

    fun addButton(imageResource: Int, id: String, isEnabled: Boolean = true, setToWhite: Boolean = true, onClick: () -> Unit) {
        val button = AppCompatImageView(ContextThemeWrapper(context, R.style.ButtonsSliderButton))
        button.setImageResource(imageResource)
        button.isEnabled = isEnabled
        if (setToWhite)
            button.setColorFilter(Color.WHITE)
        button.setOnClickListener {
            onClick()
        }
        button.setPadding(convertDpToPixel(16f))
        button.tag = id
        buttons.add(button)
    }

    fun commitButtons() {
        if (!measured)
            return
        currentPosition = 0
        val width = (buttons.size * widthEachActionButton.roundUpToInt()) + ((buttons.size - 1) * dividerWidth)
        val layoutParams = RelativeLayout.LayoutParams(width.roundUpToInt(), widthEachActionButton.toInt())
        layoutParams.marginStart = (widthLeftRightButton + dividerWidth).roundUpToInt()
        layoutParams.marginEnd = (widthLeftRightButton + dividerWidth).roundUpToInt()
        buttonsContainer.layoutParams = layoutParams
        for ((i, button) in buttons.withIndex()) {
            button.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            val buttonContainer = RelativeLayout(ContextThemeWrapper(context, R.style.ButtonsSliderButton))
            buttonContainer.tag = button.tag
            buttonContainer.layoutParams = LayoutParams(widthEachActionButton.roundUpToInt(), widthEachActionButton.roundUpToInt())
            buttonContainer.addView(button)

            val marker = View(context)
            marker.tag = "MARKER"
            val layoutParamsMarker = RelativeLayout.LayoutParams((widthEachActionButton * 0.5).toInt(), 4)
            layoutParamsMarker.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, TRUE)
            layoutParamsMarker.addRule(RelativeLayout.CENTER_HORIZONTAL, TRUE)
            layoutParamsMarker.bottomMargin = (widthEachActionButton / 10).toInt()
            marker.layoutParams = layoutParamsMarker
            marker.setBackgroundColor(Color.WHITE)
            marker.visibility = View.GONE
            buttonContainer.addView(marker)

            buttonsContainer.addView(buttonContainer)

            if (i < buttons.size - 1)
                addDivider(buttonsContainer)
        }
    }

    fun removeAllButtons() {
        buttons = arrayListOf()
        if (this::buttonsContainer.isInitialized)
            buttonsContainer.removeAllViews()
    }

    fun setEnableButtons(isEnabled: Boolean) {
        for (button in buttons)
            button.isEnabled = isEnabled
    }

    fun setMarkerOn(id: String) {
        if (!this::buttonsContainer.isInitialized)
            return

        buttonsContainer.forEach { buttonsContainerView ->
            if (buttonsContainerView is RelativeLayout) {
                val buttonContainer = buttonsContainerView as RelativeLayout
                buttonContainer.forEach { view ->
                    if (view.tag == "MARKER") {
                        if (buttonsContainerView.tag == id)
                            view.visibility = View.VISIBLE
                        else
                            view.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun addCardContentContainer() {
        cardContentContainer = RelativeLayout(context)
        cardContentContainer.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        cardContentContainer.setBackgroundColor(Color.TRANSPARENT)
        addView(cardContentContainer)
    }

    private fun addButtonsContainer() {
        buttonsContainer = LinearLayout(context)
        buttonsContainer.orientation = LinearLayout.HORIZONTAL
        buttonsContainer.setBackgroundColor(Color.TRANSPARENT)
        cardContentContainer.addView(buttonsContainer)
    }

    private fun addDivider(parentView: ViewGroup) {
        val divider = View(context)
        divider.layoutParams = LayoutParams(dividerWidth.toInt(), widthEachActionButton.toInt())
        divider.setBackgroundColor(Color.WHITE)
        parentView.addView(divider)
    }

    private fun addButtonLeft() {
        buttonLeft = AppCompatImageView(ContextThemeWrapper(context, R.style.ButtonsSliderButtonArrow))
        val buttonContainerLayout = LinearLayout(context)
        buttonContainerLayout.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        buttonContainerLayout.orientation = LinearLayout.HORIZONTAL
        buttonLeft.layoutParams = ViewGroup.LayoutParams(widthLeftRightButton.roundUpToInt(), widthEachActionButton.toInt())
        buttonLeft.setImageResource(R.drawable.ic_button_back)
        buttonLeft.setPadding(convertDpToPixel(16f))
        buttonLeft.setOnClickListener {
            if (currentPosition < 0) {
                buttonRight.isEnabled = false
                currentPosition++
                val buttonsContainerLayout = buttonsContainer.layoutParams as MarginLayoutParams
                val startMargin = buttonsContainerLayout.marginStart
                val move = widthLeftRightButton.roundUpToInt() + dividerWidth.toInt()
                val animator = ValueAnimator.ofInt(0, move)
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
        buttonContainerLayout.addView(buttonLeft)
        addDivider(buttonContainerLayout)
        cardContentContainer.addView(buttonContainerLayout)
    }

    private fun addButtonRight() {
        val buttonContainerLayout = LinearLayout(context)
        val layoutParamsButtonContainer = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsButtonContainer.addRule(ALIGN_PARENT_END, TRUE)
        buttonContainerLayout.layoutParams = layoutParamsButtonContainer
        buttonContainerLayout.orientation = LinearLayout.HORIZONTAL
        buttonRight = AppCompatImageView(ContextThemeWrapper(context, R.style.ButtonsSliderButtonArrow))
        val layoutParams = LayoutParams(widthLeftRightButton.roundUpToInt(), widthEachActionButton.toInt())
        buttonRight.layoutParams = layoutParams
        buttonRight.setImageResource(R.drawable.ic_button_next)
        buttonRight.setPadding(convertDpToPixel(16f))
        buttonRight.setOnClickListener {
            if (currentPosition > numberOfVisibleButtons - buttons.size) {
                buttonLeft.isEnabled = false
                currentPosition--
                val buttonsContainerLayout = buttonsContainer.layoutParams as MarginLayoutParams
                val startMargin = buttonsContainerLayout.marginStart
                val move = widthLeftRightButton.roundUpToInt() + dividerWidth.toInt()
                val animator = ValueAnimator.ofInt(0, move)
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
        addDivider(buttonContainerLayout)
        buttonContainerLayout.addView(buttonRight)
        cardContentContainer.addView(buttonContainerLayout)
    }
}
