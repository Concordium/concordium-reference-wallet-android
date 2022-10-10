package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseBottomSheetDialogFragment

class SearchTokenBottomSheet : BaseBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_search_token, container, false)
    }
}
