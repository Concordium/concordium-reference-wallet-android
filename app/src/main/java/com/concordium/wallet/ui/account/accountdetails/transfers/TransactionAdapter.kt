package com.concordium.wallet.ui.account.accountdetails.transfers


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.TransactionViewHelper
import com.concordium.wallet.ui.common.TransactionViewHelper.OnClickListenerInterface
import com.concordium.wallet.uicore.recyclerview.BaseAdapter
import com.concordium.wallet.uicore.recyclerview.pinnedheader.PinnedHeaderListener
import kotlinx.android.synthetic.main.item_footer_progress.view.*
import kotlinx.android.synthetic.main.item_header.view.*
import kotlinx.android.synthetic.main.item_transaction.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TransactionAdapter(
    private var scope: CoroutineScope,
    private var accountUpdater: AccountUpdater,
    private val context: Context,
    data: MutableList<AdapterItem>
) :
    BaseAdapter<AdapterItem>(data), PinnedHeaderListener {

    private lateinit var decryptListener: OnDecryptClickListenerInterface

    var isShieldedAccount: Boolean = false
    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val view: View, val isShieldedAccount: Boolean) : RecyclerView.ViewHolder(view) {
        private val rootLayout: View = view.item_root_layout
        private val titleTextView: TextView = view.title_textview
        private val subHeaderTextView: TextView = view.subheader_textview
        private val totalTextView: TextView = view.total_textview
        private val costTextView: TextView = view.cost_textview
        private val memoTextView: TextView = view.memo_textview
        private val amountTextView: TextView = view.amount_textview
        private val alertImageView: ImageView = view.alert_imageview
        private val statusImageView: ImageView = view.status_imageview
        private val lockImageView: ImageView = view.lock_imageview


        fun bind(item: TransactionItem, onItemClickListener: OnItemClickListener?) {
            val ta = item.transaction as Transaction

            scope.launch {
                TransactionViewHelper.show(
                    accountUpdater,
                    ta,
                    titleTextView,
                    subHeaderTextView,
                    totalTextView,
                    costTextView,
                    memoTextView,
                    amountTextView,
                    alertImageView,
                    statusImageView,
                    lockImageView,
                    isShieldedAccount,
                    decryptCallback = object : OnClickListenerInterface {
                        override fun onDecrypt() {
                            decryptListener?.onDecrypt(ta)
                        }
                    }
                )
            }

            // Click
            if (onItemClickListener != null) {
                rootLayout.setOnClickListener {
                    onItemClickListener.onItemClicked(ta)
                }
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerTextView: TextView = view.header_textview

        fun bind(headerItem: HeaderItem) {
            headerTextView.text = headerItem.title
        }
    }

    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val listProgressLayout: View = view.list_progress_layout
        private val listProgressBar: ProgressBar = view.list_progress_bar

        fun bind() {
        }
    }


    fun setData(data: List<AdapterItem>) {
        clear()
        addAll(data)
        notifyDataSetChanged()
    }

    //region General Adapter overrides
    //************************************************************

    override fun createDummyItemForFooter(): AdapterItem {
        var item = TransactionItem()
        return item
    }

    override fun getItemViewType(position: Int): Int {
        val item = items.get(position)
        if (item.getItemType() == AdapterItem.ItemType.Header) {
            return HEADER
        }
        return super.getItemViewType(position)
    }

    //endregion

    //region Create ViewHolder
    //************************************************************

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction,
                parent,
                false
            ), isShieldedAccount
        )
    }


    override fun onCreateFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_footer_progress, parent, false)
        return FooterViewHolder(view)
    }

    //endregion

    //region Bind ViewHolder
    //************************************************************

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val headerItem = items.get(position)
        (holder as HeaderViewHolder).bind(headerItem as HeaderItem)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.get(position)
        (holder as ItemViewHolder).bind(item as TransactionItem, onItemClickListener)
    }

    override fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder) {
        (holder as FooterViewHolder).bind()
    }

    //endregion

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: Transaction)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion

    //region PinnedHeader overrides
    //************************************************************

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

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.item_header
    }

    override fun bindHeaderData(headerView: View, headerPosition: Int) {
        val item = items.get(headerPosition)
        val headerItem = item as HeaderItem
        headerView.header_textview.text = headerItem.title
    }

    override fun isHeader(itemPosition: Int): Boolean {
        val item = items.get(itemPosition)
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

    //endregion
}