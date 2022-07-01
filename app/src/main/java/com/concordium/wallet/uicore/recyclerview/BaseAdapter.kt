package com.concordium.wallet.uicore.recyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A basic recycler adapter that supports a footer (for example for loading more or error) and header items.
 * @param <T>
</T> */
abstract class BaseAdapter<T>(val items: MutableList<T>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class FooterType {
        LOAD_MORE,
        ERROR
    }

    companion object {
        val HEADER = 0
        val ITEM = 1
        val FOOTER = 2
    }

    private var isFooterAdded = false
    private var footerPosition = -1

    val isEmpty: Boolean
        get() = itemCount == 0

    interface OnItemClickListener {
        fun onItemClick(position: Int, view: View)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewHolder: RecyclerView.ViewHolder?

        if (isItemViewType(viewType)) {
            return onCreateItemViewHolder(parent, viewType)
        }
        viewHolder = when (viewType) {
            HEADER -> onCreateHeaderViewHolder(parent)
            FOOTER -> onCreateFooterViewHolder(parent)
            else -> {
                onCreateItemViewHolder(parent, viewType)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (isItemViewType(getItemViewType(position))) {
            onBindItemViewHolder(viewHolder, position)
        }
        when (getItemViewType(position)) {
            HEADER -> onBindHeaderViewHolder(viewHolder, position)
            FOOTER -> onBindFooterViewHolder(viewHolder)
            else -> {
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLastPosition(position) && isFooterAdded) FOOTER else ITEM
    }

    open fun isItemViewType(viewType: Int): Boolean {
        return viewType == ITEM
    }

    protected abstract fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    protected abstract fun onCreateItemViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder

    protected abstract fun onCreateFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    protected abstract fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int)

    protected abstract fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int)

    protected abstract fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder)

    private fun displayLoadMoreFooter() {}

    private fun displayErrorFooter() {}

    abstract fun createDummyItemForFooter(): T

    private fun getItem(position: Int): T? {
        return items[position]
    }

    fun add(item: T) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addAll(items: List<T>) {
        for (item in items) {
            add(item)
        }
    }

    private fun remove(item: T?) {
        val position = items.indexOf(item)
        if (position > -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clear() {
        isFooterAdded = false
        while (itemCount > 0) {
            remove(getItem(0))
        }
    }

    private fun isLastPosition(position: Int): Boolean {
        return position == items.size - 1
    }

    fun addFooter() {
        if (!isFooterAdded) {
            isFooterAdded = true
            add(createDummyItemForFooter())
            footerPosition = items.size - 1
        }
    }

    fun removeFooter() {
        if (isFooterAdded) {
            isFooterAdded = false

            val position = footerPosition
            if (position > items.size - 1) {
                return
            }
            val item = getItem(position)

            if (item != null) {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }

    }

    fun updateFooter(footerType: FooterType) {
        when (footerType) {
            FooterType.LOAD_MORE -> displayLoadMoreFooter()
            FooterType.ERROR -> displayErrorFooter()
        }
    }
}
