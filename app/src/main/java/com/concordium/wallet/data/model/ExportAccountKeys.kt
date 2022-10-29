package com.concordium.wallet.data.model

data class ExportAccountKeys(
    val environment: String,
    val type: String,
    val v: Int,
    val value: AccountDataKeys
)
/*
data class Value(
    val accountKeys: AccountKeys,
    val address: String,
    val credentials: Credentials
)

data class AccountKeys(
    val keys: Keys,
    val threshold: Int
)

data class Credentials(
    val `0`: String
)
*/
