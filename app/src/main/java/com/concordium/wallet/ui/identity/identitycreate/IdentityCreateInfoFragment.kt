package com.concordium.wallet.ui.identity.identitycreate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_identity_create_info.*
import kotlinx.android.synthetic.main.progress.*

class IdentityCreateInfoFragment : BaseFragment(R.string.identity_create_title) {

    private lateinit var sharedViewModel: IdentityCreateViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        sharedViewModel.initialize()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_identity_create_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.updateState()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        sharedViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(IdentityCreateViewModel::class.java)

        sharedViewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        sharedViewModel.isFirstIdentityLiveData.observe(this, Observer<Boolean> { isFirst ->
            isFirst?.let {
                updateInfoText(isFirst)
            }
        })
    }

    private fun initializeViews() {
        showWaiting(true)
        confirm_button.setOnClickListener {
            gotoAccountName()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun gotoAccountName() {
        findNavController().navigate(IdentityCreateInfoFragmentDirections.actionNavIdentityCreateInfoToNavIdentityCreateAccountName())
    }

    private fun updateInfoText(isFirstIdentity: Boolean) {
        if (isFirstIdentity) {
            info_textview.setText(R.string.identity_create_info_info_first)
        } else {
            info_textview.setText(R.string.identity_create_info_info)
        }
    }

    //endregion

}