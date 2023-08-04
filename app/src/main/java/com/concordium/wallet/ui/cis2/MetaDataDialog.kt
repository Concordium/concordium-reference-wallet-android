package com.concordium.wallet.ui.cis2

import android.view.View
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.databinding.ShowRawMetadataDialogBinding

fun setMetadataDialog(
    tokenMetadata: TokenMetadata,
    dialogRoot: View,
    dialogBinding: ShowRawMetadataDialogBinding,
    openButton: View,
) {
    dialogRoot.visibility = View.GONE
    dialogRoot.setOnClickListener {
        dialogRoot.visibility = View.GONE
    }

    dialogBinding.closeDialog.setOnClickListener {
        dialogRoot.visibility = View.GONE
    }
    openButton.setOnClickListener {
        dialogBinding.apply {
            thumbnailTitle.visibility =
                if (tokenMetadata.thumbnail != null) View.VISIBLE else View.GONE
            thumbnailValue.visibility =
                if (tokenMetadata.thumbnail != null) View.VISIBLE else View.GONE
            tokenMetadata.thumbnail?.let { thumbnail ->
                thumbnailValue.text = thumbnail.url
            }

            displayTitle.visibility =
                if (tokenMetadata.display != null) View.VISIBLE else View.GONE
            displayValue.visibility =
                if (tokenMetadata.display != null) View.VISIBLE else View.GONE
            tokenMetadata.display?.let { display ->
                displayValue.text = display.url
            }

            nameValue.text = tokenMetadata.name

            decimalValue.text = tokenMetadata.decimals.toString()

            descriptionValue.text = tokenMetadata.description

            symbolTitle.visibility =
                if (tokenMetadata.symbol != null) View.VISIBLE else View.GONE
            symbolValue.visibility =
                if (tokenMetadata.symbol != null) View.VISIBLE else View.GONE
            tokenMetadata.symbol?.let { symbol ->
                symbolValue.text = symbol
            }

            uniqueValue.text = tokenMetadata.unique.toString()

            dialogRoot.visibility = View.VISIBLE
        }
    }
}