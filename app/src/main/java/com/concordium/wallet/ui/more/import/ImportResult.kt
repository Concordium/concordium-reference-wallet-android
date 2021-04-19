package com.concordium.wallet.ui.more.import

class ImportResult {

    val recipientResultList = mutableListOf<RecipientImportResult>()
    val identityResultList = mutableListOf<IdentityImportResult>()
    var failedRecipientCount = 0
        private set
    var failedIdentityCount = 0
        private set
    var duplicateRecipientCount = 0
        private set

    enum class Status {
        Ok, Duplicate, Failed
    }

    data class RecipientImportResult(
        var name: String,
        var status: Status
    )

    data class IdentityImportResult(
        var name: String,
        var status: Status
    ) {
        val accountResultList: MutableList<AccountImportResult> = mutableListOf()
        val readonlyAccountResultList: MutableList<AccountImportResult> = mutableListOf()
        var failedAccountCount = 0
            private set

        fun addAccountResult(accountImportResult: AccountImportResult) {
            accountResultList.add(accountImportResult)
            if (accountImportResult.status == Status.Failed) {
                failedAccountCount++
            }
        }

        fun addReadOnlyAccountResult(accountImportResult: AccountImportResult) {
            readonlyAccountResultList.add(accountImportResult)
        }
    }

    data class AccountImportResult(
        var name: String,
        var status: Status
    )

    fun addRecipientResult(recipientImportResult: RecipientImportResult) {
        recipientResultList.add(recipientImportResult)
        if (recipientImportResult.status == Status.Failed) {
            failedRecipientCount++
        }
        if (recipientImportResult.status == Status.Duplicate) {
            duplicateRecipientCount++
        }
    }

    fun addIdentityResult(identityImportResult: IdentityImportResult) {
        identityResultList.add(identityImportResult)
        if (identityImportResult.status == Status.Failed) {
            failedIdentityCount++
        }
    }

    fun hasAnyFailed(): Boolean {
        return (failedRecipientCount > 0) || (failedIdentityCount > 0) || (identityResultList.any {
            it.failedAccountCount > 0
        })
    }
}