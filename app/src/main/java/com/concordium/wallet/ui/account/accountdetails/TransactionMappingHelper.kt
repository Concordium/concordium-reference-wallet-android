package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer

class TransactionMappingHelper(
    private val account: Account,
    private val recipientList: List<Recipient>
) {

    data class RecipientResult(
        val hasFoundRecipient: Boolean,
        val recipientOrAddress: String
    )

    private fun findRecipientOrUseAddress(address: String): RecipientResult {
        for (recipient in recipientList) {
            if (recipient.address == address) {
                return RecipientResult(true, recipient.name)
            }
        }
        val addressFormatted = if (address.length > 6) {
            "${address.substring(0, 6)}..."
        } else {
            address
        }
        return RecipientResult(false, addressFormatted)
    }


    fun addTitlesToTransaction(transaction: Transaction, transfer: Transfer, ctx: Context) {
        if (transaction.isDelegationTransfer()){
            transaction.title = ctx.getString(R.string.account_delegation_pending)
        } else if (transaction.isBakerTransfer()) {
            transaction.title = ctx.getString(R.string.account_baking_pending)
        } else {
            // ...else transfer is always outgoing, so just use toAddress
            val recipientResult = findRecipientOrUseAddress(transfer.toAddress)
            transaction.title = recipientResult.recipientOrAddress
            transaction.fromAddressTitle = account.name
            if (recipientResult.hasFoundRecipient) {
                transaction.toAddressTitle = recipientResult.recipientOrAddress
            }
        }
    }

    fun addTitleToTransaction(
        transaction: Transaction,
        remoteTransaction: RemoteTransaction
    ) {
        var address: String? = null
        var recipientName: String? = null
        val source = remoteTransaction.details.transferSource
        val destination = remoteTransaction.details.transferDestination
        if (source != null && destination != null) {
            address = when (remoteTransaction.origin.type) {
                TransactionOriginType.Self -> destination
                TransactionOriginType.Account -> source
                else -> null
            }
        }
        if (address != null) {
            val recipientResult = findRecipientOrUseAddress(address)
            transaction.title = recipientResult.recipientOrAddress
            if (recipientResult.hasFoundRecipient) {
                recipientName = recipientResult.recipientOrAddress
            }
        } else {
            transaction.title = remoteTransaction.details.description
        }
        when (remoteTransaction.origin.type) {
            TransactionOriginType.Self -> {
                transaction.fromAddressTitle = account.name
                transaction.toAddressTitle = recipientName ?: ""
            }
            else -> {
                transaction.fromAddressTitle = recipientName ?: ""
                transaction.toAddressTitle = account.name
            }
        }
    }
}