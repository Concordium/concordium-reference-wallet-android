package com.concordium.wallet.uicore.recyclerview.pinnedheader

import android.view.View

interface PinnedHeaderListener {
    /**
     * This method gets called by [PinnedHeaderItemDecoration] to fetch the position of the header item in the adapter
     * that is used for (represents) item at specified position.
     * @param itemPosition int. Adapter's position of the item for which to do the search of the position of the header item.
     * @return int. Position of the header item in the adapter.
     */
    fun getHeaderPositionForItem(itemPosition: Int): Int

    /**
     * This method gets called by [PinnedHeaderItemDecoration] to setup the header View.
     * @param headerPosition int. Position of the header item in the adapter.
     */
    fun bindHeaderData(headerPosition: Int): View

    /**
     * This method gets called by [PinnedHeaderItemDecoration] to verify whether the item represents a header.
     * @param itemPosition int.
     * @return true, if item at the specified adapter's position represents a header.
     */
    fun isHeader(itemPosition: Int): Boolean
}