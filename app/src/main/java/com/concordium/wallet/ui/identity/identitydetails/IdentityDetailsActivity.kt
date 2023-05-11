package com.concordium.wallet.ui.identity.identitydetails

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentityDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.setEditText
import com.concordium.wallet.uicore.view.IdentityView

class IdentityDetailsActivity : BaseActivity() {
    companion object {
        const val EXTRA_IDENTITY = "extra_identity"
    }

    private lateinit var binding: ActivityIdentityDetailsBinding
    private lateinit var viewModel: IdentityDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.identity_details_title
        )

        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity
        initializeViewModel()
        viewModel.initialize(identity)
        initViews()
    }

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

        binding.identityView.enableChangeNameOption(viewModel.identity)

        if (viewModel.identity.status != IdentityStatus.DONE) {
            binding.contentCardview.visibility = View.GONE
        }

        val adapter = IdentityAttributeAdapter(attributes.toSortedMap())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.isNestedScrollingEnabled = false

        viewModel.identityChanged.observe(this) {
            binding.identityView.setIdentityData(it)
        }

        binding.identityView.setOnChangeNameClickListener(object :
            IdentityView.OnChangeNameClickListener {
            override fun onChangeNameClicked(item: Identity) {
                showChangeNameDialog()
            }
        })
    }

    private fun initializeErrorViews() {
        if (viewModel.identity.status == IdentityStatus.ERROR) {
            binding.errorWrapperLayout.visibility = View.VISIBLE
            binding.errorTextview.text = viewModel.identity.detail ?: ""
            binding.identityView.foreground =
                AppCompatResources.getDrawable(this, R.drawable.bg_cardview_error_border)
            binding.removeButton.setOnClickListener {
                viewModel.removeIdentity(viewModel.identity)
                finish()
            }
        } else {
            binding.identityView.foreground =
                AppCompatResources.getDrawable(this, R.drawable.bg_cardview_border)
            binding.errorWrapperLayout.visibility = View.GONE
        }
    }

    private fun showChangeNameDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.account_details_change_name_popup_title))
        builder.setMessage(getString(R.string.account_details_change_name_popup_subtitle))
        val input = AppCompatEditText(this)
        input.hint = viewModel.identity.name
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setEditText(this, input)
        builder.setPositiveButton(getString(R.string.account_details_change_name_popup_save)) { _, _ ->
            viewModel.changeIdentityName(input.text.toString())
        }
        builder.setNegativeButton(getString(R.string.account_details_change_name_popup_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
