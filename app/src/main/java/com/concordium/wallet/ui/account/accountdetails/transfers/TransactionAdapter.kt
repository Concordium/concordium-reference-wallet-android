package com.concordium.wallet.ui.account.accountdetails.transfers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.databinding.ItemFooterProgressBinding
import com.concordium.wallet.databinding.ItemHeaderBinding
import com.concordium.wallet.databinding.ItemTransactionBinding
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.TransactionViewHelper
import com.concordium.wallet.ui.common.TransactionViewHelper.OnClickListenerInterface
import com.concordium.wallet.uicore.recyclerview.BaseAdapter
import com.concordium.wallet.uicore.recyclerview.pinnedheader.PinnedHeaderListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TransactionAdapter(
    private val context: Context,
    private var scope: CoroutineScope,
    private var accountUpdater: AccountUpdater,
    data: MutableList<AdapterItem>
) : BaseAdapter<AdapterItem>(data), PinnedHeaderListener {

    private lateinit var decryptListener: OnDecryptClickListenerInterface

    var isShieldedAccount: Boolean = false
    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemTransactionBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as ItemViewHolder) {
            val transactionItem = items[position] as TransactionItem
                    val transaction = transactionItem.transaction as Transaction
                    scope.launch {
                        TransactionViewHelper.show(
                            accountUpdater,
                            transaction,
                            binding.titleTextview,
                            binding.subheaderTextview,
                            binding.totalTextview,
                            binding.memoTextview,
                            binding.amountTextview,
                            binding.alertImageview,
                            binding.statusImageview,
                            binding.lockImageview,
                            isShieldedAccount,
                            decryptCallback = object : OnClickListenerInterface {
                                override fun onDecrypt() {
                                    decryptListener.onDecrypt(transaction)
                                }
                            }
                        )
                    }

                    if (onItemClickListener != null) {
                        binding.itemRootLayout.setOnClickListener {
                    onItemClickListener?.onItemClicked(transaction)
                }
            }
        }
    }

    fun setData(data: List<AdapterItem>) {
        clear()
        addAll(data)
        notifyDataSetChanged()
    }

    override fun createDummyItemForFooter(): AdapterItem {
        return TransactionItem()
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position].getItemType() == AdapterItem.ItemType.Header) {
            return HEADER
        }
        return super.getItemViewType(position)
    }

    interface OnItemClickListener {
        fun onItemClicked(item: Transaction)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var itemPos = itemPosition
        var headerPosition = 0
        do {
            if (this.isHeader(itemPos)) {
                headerPosition = itemPos
                break
            }
            itemPos -= 1
        } while (itemPos >= 0)
        return headerPosition
    }

    override fun bindHeaderData(headerPosition: Int): View {
        val item = items[headerPosition]
        val headerItem = item as HeaderItem
        val binding = ItemHeaderBinding.inflate(LayoutInflater.from(context))
        binding.headerTextview.text = headerItem.title
        return binding.headerTextview
    }

    override fun isHeader(itemPosition: Int): Boolean {
        val item = items[itemPosition]
        return (item.getItemType() == AdapterItem.ItemType.Header)
    }

    fun setIsShielded(shielded: Boolean) {
        isShieldedAccount = shielded
    }

    fun setOnDecryptListener(listener: OnDecryptClickListenerInterface) {
        decryptListener = listener
    }

    interface OnDecryptClickListenerInterface {
        fun onDecrypt(ta: Transaction)
    }

    // Header
    inner class HeaderViewHolder(val binding: ItemHeaderBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as HeaderViewHolder) {
            val item = items[position] as HeaderItem
            binding.headerTextview.text = item.title
        }
    }

    // Footer
    inner class FooterViewHolder(val binding: ItemFooterProgressBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemFooterProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FooterViewHolder(binding)
    }

    override fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder) {
    }
}
