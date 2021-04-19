package com.concordium.wallet.ui.more.export

import android.app.AlertDialog
import android.content.DialogInterface
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
import kotlinx.android.synthetic.main.fragment_export.*
import kotlinx.android.synthetic.main.fragment_export.view.*

class ExportFragment(val titleId: Int ?= null) : BaseFragment(titleId) {

    private val viewModel: ExportViewModel by activityViewModels()


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
        val rootView = inflater.inflate(R.layout.fragment_export, container, false)
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

        viewModel.errorExportLiveData.observe(this, object : EventObserver<List<String>>() {
            override fun onUnhandledEvent(value: List<String>) {
                val buffer = StringBuffer()
                buffer.append(getString(R.string.export_error_account_not_finalised))
                var count = 1
                for (accountName in value) {
                    buffer.append(getString(R.string.export_error_account_not_finalised_name,count.toString(),accountName))
                    count++
                }
                val builder = AlertDialog.Builder(activity)
                builder.setMessage(buffer.toString())
                builder.setPositiveButton(getString(R.string.export_error_account_not_finalised_continue), object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which:Int) {
                        viewModel.export(true)
                    }
                })
                builder.setNegativeButton(getString(R.string.export_error_account_not_finalised_cancel), object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which:Int) {
                        dialog.dismiss()
                    }
                })
                builder.create().show()
            }
        })

    }

    private fun initializeViews(view: View) {
        view.confirm_button.setOnClickListener {
            viewModel.export(false)
        }
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