package com.concordium.wallet.ui.more.export

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.FragmentExportBinding
import com.concordium.wallet.ui.base.BaseFragment

class ExportFragment(val titleId: Int ?= null) : BaseFragment(titleId) {
    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!
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
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        initializeViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun initializeViews() {
        binding.confirmButton.setOnClickListener {
            viewModel.export(false)
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        // The 'parent' activity is handling the progress_layout
        binding.confirmButton.isEnabled = !waiting
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    //endregion
}
