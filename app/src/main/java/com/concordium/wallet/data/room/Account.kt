package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.App
import com.concordium.wallet.data.model.*
import com.concordium.wallet.data.room.typeconverter.AccountTypeConverters
import com.google.gson.JsonObject
import java.io.Serializable


@Entity(tableName = "account_table")
@TypeConverters(AccountTypeConverters::class)
data class Account(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "identity_id")
    val identityId: Int,
    var name: String,
    var address: String,

    @ColumnInfo(name = "submission_id")
    var submissionId: String,

    @ColumnInfo(name = "transaction_status")
    var transactionStatus: TransactionStatus,

    @ColumnInfo(name = "encrypted_account_data")
    var encryptedAccountData: String,

    @ColumnInfo(name = "revealed_attributes")
    var revealedAttributes: List<IdentityAttribute>,

    var credential: CredentialWrapper?,

    @ColumnInfo(name = "finalized_balance")
    var finalizedBalance: Long = 0,

    @ColumnInfo(name = "current_balance")
    var currentBalance: Long = 0,

    @ColumnInfo(name = "total_balance")
    var totalBalance: Long = 0,

    @ColumnInfo(name = "total_unshielded_balance")
    var totalUnshieldedBalance: Long = 0,

    @ColumnInfo(name = "total_shielded_balance")
    var totalShieldedBalance: Long = 0,

    @ColumnInfo(name = "finalized_encrypted_balance")
    var finalizedEncryptedBalance: AccountEncryptedAmount?,

    @ColumnInfo(name = "current_encrypted_balance")
    var currentEncryptedBalance: AccountEncryptedAmount?,

    @ColumnInfo(name = "current_balance_status")
    var encryptedBalanceStatus: ShieldedAccountEncryptionStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,

    @ColumnInfo(name = "total_staked")
    var totalStaked: Long = 0,

    @ColumnInfo(name = "total_at_disposal")
    var totalAtDisposal: Long = 0,

    @ColumnInfo(name = "read_only")
    var readOnly: Boolean = false,

    @ColumnInfo(name = "finalized_account_release_schedule")
    var finalizedAccountReleaseSchedule: AccountReleaseSchedule?,

    @ColumnInfo(name = "baker_id")
    var bakerId: Long? = null

) : Serializable {


    fun isInitial(): Boolean {
        if (readOnly) {
            return false
        }
        val credential = this.credential ?: return true
        val gson = App.appCore.gson
        val credentialValueJsonObject = gson.fromJson(credential.value.json, JsonObject::class.java)
        if (credentialValueJsonObject.get("type")?.asString == "initial") {
            return true
        }
        if (credentialValueJsonObject.getAsJsonObject("credential")?.get("type")?.asString == "initial") {
            return true
        }
        return false
    }

    public fun getAccountName(): String {
        if (readOnly) {
            return address.substring(0, 8)
        } else {
            return name
        }
    }

    fun isBaker(): Boolean {
        return bakerId != null
    }

    fun getAtDisposalSubstraction(): Long {
        return Math.max((finalizedAccountReleaseSchedule?.total?.toLong() ?: 0), totalStaked)
    }
}