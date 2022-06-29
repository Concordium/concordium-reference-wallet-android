package com.concordium.wallet.uicore.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewImportResultBinding
import com.concordium.wallet.ui.more.import.ImportResult

class ImportResultView : ConstraintLayout {
    private val binding = ViewImportResultBinding.inflate(LayoutInflater.from(context), this, true)

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.view_import_result, this)
    }

    fun setAddressBookData(importResult: ImportResult) {
        binding.readonlyHeaderTextview.visibility = View.GONE
        binding.readonlyContentTextview.visibility = View.GONE
        binding.headerIdTextview.visibility = View.GONE
        binding.subheaderTextview.setText(R.string.import_confirmed_recipient_accounts)
        val headerText = StringBuilder(resources.getText(R.string.import_confirmed_address_book))

        var hasAddedFirstLine = false
        val strBuilder = StringBuilder()
        for (recipientImportResult in importResult.recipientResultList) {
            if (recipientImportResult.status == ImportResult.Status.Ok) {
                if (hasAddedFirstLine) {
                    strBuilder.append("\n")
                }
                strBuilder.append(recipientImportResult.name)
                hasAddedFirstLine = true
            }
        }
        val duplicateCount = importResult.duplicateRecipientCount
        if (duplicateCount > 0) {
            if (hasAddedFirstLine) {
                strBuilder.append("\n")
            }
            val str = context.resources.getQuantityString(
                R.plurals.import_confirmed_state_recipients_duplicate,
                duplicateCount,
                duplicateCount
            )
            strBuilder.append(str)
            hasAddedFirstLine = true
        }
        val failedCount = importResult.failedRecipientCount
        if (failedCount > 0) {
            if (hasAddedFirstLine) {
                strBuilder.append("\n")
            }
            val str = context.resources.getQuantityString(
                R.plurals.import_confirmed_state_recipients_failed,
                failedCount,
                failedCount
            )
            strBuilder.append(str)

            val headerStr = context.resources.getQuantityString(
                R.plurals.import_confirmed_state_errors,
                failedCount,
                failedCount
            )
            headerText.append("\n$headerStr")
        }
        binding.contentTextview.setText(strBuilder.toString())
        binding.headerTextview.setText(headerText)
    }

    fun setIdentityData(identityImportResult: ImportResult.IdentityImportResult) {
        binding.headerIdTextview.setText(R.string.import_confirmed_id)
        binding.subheaderTextview.setText(R.string.import_confirmed_accounts)
        val headerText = StringBuilder()

        if (identityImportResult.status == ImportResult.Status.Failed) {
            headerText.append(resources.getText(R.string.import_confirmed_state_id_import_failed))
        } else {
            headerText.append(identityImportResult.name)
        }
        if (identityImportResult.status == ImportResult.Status.Duplicate) {
            headerText.append(" ${resources.getText(R.string.import_confirmed_state_duplicate)}")
        }

        var hasAddedFirstLine = false
        val strBuilder = StringBuilder()
        if (identityImportResult.status == ImportResult.Status.Failed) {
            strBuilder.append(resources.getText(R.string.import_confirmed_state_id_import_failed_accounts))
        } else {
            for (accountImportResult in identityImportResult.accountResultList) {
                if (accountImportResult.status != ImportResult.Status.Failed) {
                    if (hasAddedFirstLine) {
                        strBuilder.append("\n")
                    }
                    strBuilder.append(accountImportResult.name)
                    hasAddedFirstLine = true
                    if (accountImportResult.status == ImportResult.Status.Duplicate) {
                        strBuilder.append(" ${resources.getText(R.string.import_confirmed_state_duplicate)}")
                    }
                }
            }
            val failedCount = identityImportResult.failedAccountCount
            if (failedCount > 0) {
                if (hasAddedFirstLine) {
                    strBuilder.append("\n")
                }
                val str = context.resources.getQuantityString(
                    R.plurals.import_confirmed_state_accounts_failed,
                    failedCount,
                    failedCount
                )
                strBuilder.append(str)

                val headerStr = context.resources.getQuantityString(
                    R.plurals.import_confirmed_state_errors,
                    failedCount,
                    failedCount
                )
                headerText.append("\n$headerStr")
            }
        }
        binding.contentTextview.setText(strBuilder.toString())
        binding.headerTextview.setText(headerText)

        // Readonly accounts
        if (identityImportResult.readonlyAccountResultList.isEmpty()) {
            binding.readonlyHeaderTextview.visibility = View.GONE
            binding.readonlyContentTextview.visibility = View.GONE
        } else {
            addReadOnlyAccounts(identityImportResult)
        }
    }

    private fun addReadOnlyAccounts(identityImportResult: ImportResult.IdentityImportResult) {
        var hasAddedFirstLine = false
        val strBuilder = StringBuilder()
        for (accountImportResult in identityImportResult.readonlyAccountResultList) {

            if (hasAddedFirstLine) {
                strBuilder.append("\n")
            }
            strBuilder.append(accountImportResult.name)
            hasAddedFirstLine = true


        }
        binding.readonlyContentTextview.setText(strBuilder.toString())
    }
}
