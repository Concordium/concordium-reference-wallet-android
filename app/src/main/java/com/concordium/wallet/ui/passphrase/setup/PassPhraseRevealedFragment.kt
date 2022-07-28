package com.concordium.wallet.ui.passphrase.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentPassPhraseRevealBinding
import com.concordium.wallet.ui.passphrase.setup.PassPhraseViewModel.Companion.PASS_PHRASE_DATA
import com.concordium.wallet.uicore.afterMeasured

class PassPhraseRevealedFragment : PassPhraseBaseFragment() {
    private var _binding: FragmentPassPhraseRevealBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(viewModel: PassPhraseViewModel) = PassPhraseRevealedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(PASS_PHRASE_DATA, viewModel)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassPhraseRevealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*
        binding.gvReveal.afterMeasured {
            // in Figma the view is 367 x 334 in size
            val layoutParams = binding.gvReveal.layoutParams
            layoutParams.height = (binding.root.width.toFloat() * (367f / 334f)).toInt()
            binding.gvReveal.layoutParams = layoutParams
        }*/
        binding.cbConfirm.setOnCheckedChangeListener { _, checked ->
            viewModel.passPhraseConfirmChecked = checked
        }
        viewModel.generateMnemonicCode()
        binding.gvReveal.adapter = WordsAdapter(requireContext(), viewModel.mnemonicCodeToConfirm)
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
