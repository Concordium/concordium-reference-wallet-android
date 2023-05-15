package com.concordium.wallet.ui.passphrase.recover

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentExportPassPhraseRevealBinding
import com.concordium.wallet.ui.base.BaseBindingFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExportPassPhraseFragment : BaseBindingFragment<FragmentExportPassPhraseRevealBinding>(),
    AuthDelegate by AuthDelegateImpl() {

    private val viewModel: ExportPassPhraseViewModel by activityViewModel()
    override fun getLayoutResId() = R.layout.fragment_export_pass_phrase_reveal

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        viewDataBinding.llTapToReveal.setOnClickListener {
            showAuthentication(requireActivity() as AppCompatActivity) { password ->
                crossFade()
            }
        }
    }

    private fun setupViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ExportSeedPhraseState.Error -> Snackbar.make(
                        viewDataBinding.root,
                        R.string.export_seed_phrase_error,
                        Snackbar.LENGTH_LONG
                    ).show()

                    is ExportSeedPhraseState.Success -> {
                        viewDataBinding.gvReveal.adapter = WordsAdapter(
                            requireContext(),
                            state.seedPhrase
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun crossFade() {
        val shortAnimationDuration = 500L
        viewDataBinding.apply {
            gvReveal.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration)
                    .setListener(null)
            }
            llTapToReveal.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        llTapToReveal.visibility = View.GONE
                    }
                })
        }
    }

    private class WordsAdapter constructor(
        private val context: Context,
        private val items: List<String>
    ) : BaseAdapter() {
        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context)
                    .inflate(R.layout.item_passphrase_word, parent, false)
                holder = ViewHolder()
                holder.contentView = view
                holder.tvPosition = view.findViewById(R.id.tvPosition)
                holder.tvTitle = view.findViewById(R.id.tvTitle)
                view.tag = holder
            } else {
                view = convertView
                holder = convertView.tag as ViewHolder
            }

            holder.tvPosition.text = "${position + 1}."
            holder.tvTitle.text = items[position]

            return view
        }

        inner class ViewHolder {
            lateinit var contentView: View
            lateinit var tvPosition: TextView
            lateinit var tvTitle: TextView
        }
    }
}
