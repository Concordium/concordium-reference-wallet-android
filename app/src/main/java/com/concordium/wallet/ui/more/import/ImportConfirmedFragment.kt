package com.concordium.wallet.ui.more.import

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.view.ImportResultView
import kotlinx.android.synthetic.main.fragment_import_confirmed.*
import kotlinx.android.synthetic.main.fragment_import_confirmed.view.*

class ImportConfirmedFragment(titleId: Int? = null) : BaseFragment(titleId) {

    private val viewModel: ImportViewModel by activityViewModels()


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initializeViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_import_confirmed, container, false)
        initializeViews(rootView)
        return rootView
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })


    }

    private fun initializeViews(view: View) {
        view.confirm_button.setOnClickListener {
            viewModel.finishImport()
        }

        val importResult = viewModel.importResult
        view.import_headline_textview.setText(
            if (importResult.hasAnyFailed())
                R.string.import_confirmed_header_partially else R.string.import_confirmed_header
        )

        addImportResultViews(view)
    }
    
    private fun addImportResultViews(view: View) {
        val importResult = viewModel.importResult
        for (identityImportResult in importResult.identityResultList) {
            val importResultView = ImportResultView(activity)
            importResultView.setIdentityData(identityImportResult)
            addImportResultView(view, importResultView)
        }

        val importResultView = ImportResultView(activity)
        importResultView.setAddressBookData(importResult)
        addImportResultView(view, importResultView)
    }

    private fun addImportResultView(view: View, importResultView: ImportResultView) {
        view.import_result_layout.addView(importResultView)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        // The 'parent' activity is handling the progress_layout
        confirm_button.isEnabled = !waiting
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }

    //endregion

}