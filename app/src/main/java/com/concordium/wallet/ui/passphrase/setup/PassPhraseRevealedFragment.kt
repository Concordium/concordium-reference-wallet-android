package com.concordium.wallet.ui.passphrase.setup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseRevealBinding
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PassPhraseRevealedFragment : Fragment() {
    private var _binding: FragmentPassPhraseRevealBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PassPhraseViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseRevealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cbConfirm.setOnCheckedChangeListener { _, checked ->
            viewModel.continueEnabled.postValue(checked)
        }
        viewModel.generateMnemonicCode()
        binding.gvReveal.adapter = WordsAdapter(requireContext(), viewModel.mnemonicCodeToConfirm)
        binding.llTapToReveal.setOnClickListener {
            crossFade()
            binding.cbConfirm.isEnabled = true
        }
    }

    private fun crossFade() {
        val shortAnimationDuration = 500L
        binding.gvReveal.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)
        }
        binding.llTapToReveal.animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.llTapToReveal.visibility = View.GONE
                }
            })
    }

    class WordsAdapter internal constructor(private val context: Context, private val items: List<CharArray>) : BaseAdapter() {
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
                view = LayoutInflater.from(context).inflate(R.layout.item_passphrase_word, parent, false)
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
            holder.tvTitle.text = String(items[position])

            return view
        }

        inner class ViewHolder {
            lateinit var contentView: View
            lateinit var tvPosition: TextView
            lateinit var tvTitle: TextView
        }
    }
}
