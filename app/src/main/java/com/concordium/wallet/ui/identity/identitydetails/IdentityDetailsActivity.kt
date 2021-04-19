package com.concordium.wallet.ui.identity.identitydetails

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_identity_details.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class IdentityDetailsActivity :
    BaseActivity(R.layout.activity_identity_details, R.string.identity_details_title) {

    companion object {
        const val EXTRA_IDENTITY = "extra_identity"
    }

    private lateinit var viewModel: IdentityDetailsViewModel


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity
        initializeViewModel()
        viewModel.initialize(identity)
        initViews()
    }

    // endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(IdentityDetailsViewModel::class.java)
    }

    private fun initViews() {
        initializeErrorViews()
        identity_view.setIdentityData(viewModel.identity)
        val attributes = viewModel.identity.identityObject!!.attributeList.chosenAttributes

        if(viewModel.identity.status != IdentityStatus.DONE){
            content_cardview.visibility = View.INVISIBLE
        }
        val adapter = IdentityAttributeAdapter(attributes.toSortedMap())
        recyclerview.adapter = adapter
        recyclerview.isNestedScrollingEnabled = false
    }

    private fun initializeErrorViews() {
        if(viewModel.identity.status == IdentityStatus.ERROR){
            error_wrapper_layout.visibility = View.VISIBLE
            error_textview.text = viewModel.identity.detail
            remove_button.setOnClickListener(View.OnClickListener {
                GlobalScope.launch {
                    viewModel.removeIdentity(viewModel.identity)
                    finish()
                }
            })
        }
        else{
            error_wrapper_layout.visibility = View.GONE
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    //endregion

}
