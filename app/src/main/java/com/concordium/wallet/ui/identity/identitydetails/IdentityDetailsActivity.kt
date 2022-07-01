package com.concordium.wallet.ui.identity.identitydetails

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentityDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class IdentityDetailsActivity : BaseActivity() {
    companion object {
        const val EXTRA_IDENTITY = "extra_identity"
    }

    private lateinit var binding: ActivityIdentityDetailsBinding
    private lateinit var viewModel: IdentityDetailsViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.identity_details_title)

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
        )[IdentityDetailsViewModel::class.java]
    }

    private fun initViews() {
        initializeErrorViews()
        binding.identityView.setIdentityData(viewModel.identity)
        val attributes = viewModel.identity.identityObject!!.attributeList.chosenAttributes

        if(viewModel.identity.status != IdentityStatus.DONE){
            binding.contentCardview.visibility = View.GONE
        }
        val adapter = IdentityAttributeAdapter(attributes.toSortedMap())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.isNestedScrollingEnabled = false
    }

    private fun initializeErrorViews() {
        if (viewModel.identity.status == IdentityStatus.ERROR){
            binding.errorWrapperLayout.visibility = View.VISIBLE
            binding.errorTextview.text = viewModel.identity.detail
            binding.identityView.foreground = getDrawable(R.drawable.bg_cardview_error_border)
            binding.removeButton.setOnClickListener(View.OnClickListener {
                GlobalScope.launch {
                    viewModel.removeIdentity(viewModel.identity)
                    finish()
                }
            })

            if(IdentityErrorDialogHelper.canOpenSupportEmail(this)){
                binding.errorIssuanceNoEmailClientHeadline.visibility = View.GONE
            }
            else{
                binding.errorIssuanceNoEmailClientHeadline.visibility =  View.VISIBLE
                binding.errorIssuanceNoEmailClientHeadline.text = getString(R.string.contact_issuance_no_email_client_reference, viewModel.identity.identityProvider.ipInfo.ipDescription.name, viewModel.identity.identityProvider.metadata.getSupportWithDefault())
            }

            val hash = IdentityErrorDialogHelper.hash(viewModel.identity.codeUri)
            binding.errorIssuanceReferenceHash.text = hash
            binding.errorIssuanceReferenceHashCopy.setOnClickListener {
                IdentityErrorDialogHelper.copyToClipboard(this, title.toString(), resources.getString(R.string.dialog_support_text, hash, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE))
            }

            binding.supportButton.visibility = if(IdentityErrorDialogHelper.canOpenSupportEmail(this)) View.VISIBLE else View.GONE

            binding.supportButton.setOnClickListener(View.OnClickListener {
                GlobalScope.launch {
                    IdentityErrorDialogHelper.openSupportEmail(this@IdentityDetailsActivity, resources, viewModel.identity.identityProvider.metadata.getSupportWithDefault(), hash)
                }
            })
        }
        else{
            binding.identityView.foreground = getDrawable(R.drawable.bg_cardview_border)
            binding.errorWrapperLayout.visibility = View.GONE
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    //endregion
}
