package com.concordium.wallet.ui.more.import

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.FragmentImportConfirmedBinding
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.view.ImportResultView

class ImportConfirmedFragment(titleId: Int? = null) : BaseFragment(titleId) {
    private var _binding: FragmentImportConfirmedBinding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentImportConfirmedBinding.inflate(inflater, container, false)
        initializeViews(binding.root)
        return binding.root
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
    }

    private fun initializeViews(view: View) {
        binding.confirmButton.setOnClickListener {
            viewModel.finishImport()
        }

        val importResult = viewModel.importResult
        binding.importHeadlineTextview.setText(
            if (importResult.hasAnyFailed())
                R.string.import_confirmed_header_partially else R.string.import_confirmed_header
        )

        addImportResultViews(view)
    }
    
    private fun addImportResultViews(view: View) {
        val importResult = viewModel.importResult
        for (identityImportResult in importResult.identityResultList) {
            val importResultView = ImportResultView(requireActivity())
            importResultView.setIdentityData(identityImportResult)
            addImportResultView(view, importResultView)
        }

        val importResultView = ImportResultView(requireActivity())
        importResultView.setAddressBookData(importResult)
        addImportResultView(view, importResultView)
    }

    private fun addImportResultView(view: View, importResultView: ImportResultView) {
        binding.importResultLayout.addView(importResultView)
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
