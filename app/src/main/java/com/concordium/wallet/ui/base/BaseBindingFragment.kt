package com.concordium.wallet.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.concordium.wallet.util.Log

abstract class BaseBindingFragment<BINDING : ViewDataBinding> : Fragment() {

    protected var _viewDataBinding: BINDING? = null

    /**
     * will throw an exception if accessed before "onCreateView" and after "onDestroyView"
     */
    protected val viewDataBinding
        get() = _viewDataBinding!!

    @LayoutRes
    abstract fun getLayoutResId(): Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("${javaClass.simpleName} created")
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewDataBinding =
            (DataBindingUtil.inflate(
                inflater,
                getLayoutResId(),
                container,
                false
            ) as BINDING).apply {
                lifecycleOwner = viewLifecycleOwner
            }
        return viewDataBinding.root
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        _viewDataBinding = null
    }
}
