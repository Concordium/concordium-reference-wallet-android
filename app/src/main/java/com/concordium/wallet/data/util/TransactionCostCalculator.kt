package com.concordium.wallet.data.util

import com.walletconnect.util.hexToBytes
import okio.utf8Size

/**
 * Local transaction cost calculations.
 *
 * @see <a href="https://github.com/Concordium/concordium-rust-sdk/blob/01b00f7a82e62d3642be51282e6a89045727759d/src/types/transactions.rs#L1276">Rust SDK reference methods</a>
 */
object TransactionCostCalculator {
    /**
     * Base cost of a transaction is the minimum cost that accounts for
     * transaction size and signature checking. In addition to base cost
     * each transaction has a transaction-type specific cost.
     *
     * @param transactionSize size of the transaction, for example [getContractTransactionSize].
     * @param numSignatures number of signatures, which is 1 unless we implement multisig in the wallet.
     *
     * @return energy required to execute this transaction.
     */
    fun getBaseCostEnergy(
        transactionSize: Long,
        numSignatures: Int = 1,
    ): Long =
        B * transactionSize + A * numSignatures

    /**
     * @param receiveName `contractName.methodName`.
     * @param message serialized parameters in hex.
     *
     * @return size in bytes of a transaction invoking a smart contract.
     */
    fun getContractTransactionSize(
        receiveName: String,
        message: String,
    ): Long =
        TRANSACTION_HEADER_SIZE +
                TRANSACTION_TAG_SIZE +
                8 + 16 +
                2 + receiveName.utf8Size() +
                2 + message.hexToBytes().size

    /**
     * The B constant for NRG assignment.
     * This scales the effect of the number of signatures on the energy.
     */
    private const val A: Long = 100

    /**
     * The A constant for NRG assignment.
     * This scales the effect of transaction size on the energy.
     */
    private const val B: Long = 1

    /**
     * Size of a transaction header.
     * This is currently always 60 bytes.
     * Future chain updates might revise this,
     * but this is a big change so this is expected to change seldomly.
     */
    private const val TRANSACTION_HEADER_SIZE: Long = 32 + 8 + 8 + 4 + 8

    private const val TRANSACTION_TAG_SIZE: Long = 1
}
