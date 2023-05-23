package com.concordium.wallet.ui.common

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

object TransactionViewHelper {
    suspend fun show(
        accountUpdater: AccountUpdater,
        ta: Transaction,
        titleTextView: TextView,
        subHeaderTextView: TextView,
        totalTextView: TextView,
        costTextView: TextView,
        memoTextView: TextView,
        amountTextView: TextView,
        alertImageView: ImageView,
        statusImageView: ImageView,
        lockImageView: ImageView,
        isShieldedAccount: Boolean = false,
        showDate: Boolean = false,
        decryptCallback: OnClickListenerInterface? = null
    ) {
        val colorBlack = ContextCompat.getColor(totalTextView.context, R.color.text_black)
        val colorGreen = ContextCompat.getColor(totalTextView.context, R.color.text_green)
        val colorGrey = ContextCompat.getColor(totalTextView.context, R.color.text_grey)
        val colorBlue = ContextCompat.getColor(totalTextView.context, R.color.text_blue)


        // Title
        titleTextView.text = "${ta.title}"

        memoTextView.text = "${ta.getDecryptedMemo()}"
        memoTextView.visibility = if (ta.hasMemo()) View.VISIBLE else View.GONE

        // Time
        subHeaderTextView.text = if (showDate) {
            DateTimeUtil.formatDateAsLocalMediumWithTime(ta.timeStamp)
        } else {
            DateTimeUtil.formatTimeAsLocal(ta.timeStamp)
        }

        fun setTotalView(total: BigInteger) {
            totalTextView.text = CurrencyUtil.formatGTU(total, withGStroke = true)
            if (total.signum() > 0) {
                totalTextView.setTextColor(colorGreen)
            } else {
                totalTextView.setTextColor(colorBlack)
            }
            totalTextView.visibility = View.VISIBLE
            lockImageView.visibility = View.GONE
        }

        fun showTransactionFeeText() {
            costTextView.visibility = View.VISIBLE
            amountTextView.visibility = View.GONE
            costTextView.text =
                costTextView.context.getString(R.string.account_details_shielded_transaction_fee)
        }

        fun showDecryptedValueOfEncryptedAmount() {
            //show decrypted value of encrypted amount
            lockImageView.visibility = View.VISIBLE
            totalTextView.visibility = View.GONE
            costTextView.visibility = View.GONE
            amountTextView.visibility = View.GONE
            //...for now
        }

        fun hideCostLine() {
            costTextView.visibility = View.GONE
            amountTextView.visibility = View.GONE
        }


        fun showCostLineWithAmounts() {
            // Subtotal and cost
            if (ta.subtotal != null && ta.cost != null) {
                costTextView.visibility = View.VISIBLE
                amountTextView.visibility = View.VISIBLE

                var cost = ta.cost
                var costPrefix = ""

                if (ta.transactionStatus == TransactionStatus.RECEIVED ||
                    (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Ambiguous)
                ) {
                    amountTextView.setTextColor(colorBlack)
                    costTextView.setTextColor(colorBlue)
                    costPrefix = "~"
                } else if (ta.transactionStatus == TransactionStatus.ABSENT) {
                    amountTextView.setTextColor(colorGrey)
                    costTextView.setTextColor(colorGrey)
                    costPrefix = "~"
                } else if (ta.outcome == TransactionOutcome.Reject) {
                    amountTextView.setTextColor(colorGrey)
                    costTextView.setTextColor(colorBlack)
                } else {
                    amountTextView.setTextColor(colorBlack)
                    costTextView.setTextColor(colorBlack)
                }

                amountTextView.text =
                    "${CurrencyUtil.formatGTU(ta.subtotal, withGStroke = true)} - "
                costTextView.text = "$costPrefix${
                    CurrencyUtil.formatGTU(
                        cost, withGStroke = true
                    )
                } ${costTextView.context.getString(R.string.account_details_fee)}"
            } else {
                costTextView.visibility = View.GONE
                amountTextView.visibility = View.GONE
            }

        }

        //Clear first
        totalTextView.text = ""

        // Public balance
        if (!isShieldedAccount) {
            // remote transactions
            if (ta.isRemoteTransaction()) {
                // simpleTransfer (as before: so use total subtotal and cost for display)
                // transferToSecret (as simpleTransfer)
                // transferToPublic (as simpleTransfer)
                if (ta.isSimpleTransfer() || ta.isTransferToSecret() || ta.isTransferToPublic()) {
                    setTotalView(ta.getTotalAmountForRegular())
                    showCostLineWithAmounts()
                } else
                // encryptedTransfer
                //    if origin is self => show as simpleTransfer, but show subtotal/cost row as "Shielded transaction fee
                //    else => do NOT show
                    if (ta.isEncryptedTransfer()) {
                        setTotalView(ta.getTotalAmountForRegular())
                        if (ta.isOriginSelf()) {
                            showTransactionFeeText()
                        } else {
                            // left empty intentionally (won't be called as item is filtered out)
                        }
                    } else { // baker
                        setTotalView(ta.getTotalAmountForRegular())
                        showCostLineWithAmounts()
                    }
            }
            // local (unfinalized) transactions
            else {

                // simpleTransfer (handle as before)
                // transferToSecret (as simpleTransfer)
                if (ta.isSimpleTransfer() || ta.isTransferToSecret()) {
                    setTotalView(ta.getTotalAmountForRegular())
                    showCostLineWithAmounts()
                } else
                // transferToPublic (show only the cost as total, no subtotal or fee on second row - clarified with Concordium)
                    if (ta.isTransferToPublic()) {
                        setTotalView(ta.getTotalAmountForRegular())
                        hideCostLine()
                    } else
                    // encryptedTransfer (show only the cost as total, subtotal/cost row should be "Shielded transaction fee")
                        if (ta.isEncryptedTransfer()) {
                            setTotalView(ta.getTotalAmountForRegular())
                            showTransactionFeeText()
                        }
            }
        }
        // Shielded balance
        else {
            // remote transactions
            if (ta.isRemoteTransaction()) {
                // simpleTransfer - NOT shown
                if (ta.isSimpleTransfer()) {
                    // left empty intentionally (filtered out)
                } else
                // transferToSecret (only one row - show the subtotal with minus)
                // transferToPublic (only one row - show the subtotal with minus)
                    if (ta.isTransferToSecret() || ta.isTransferToPublic()) {
                        setTotalView(ta.getTotalAmountForShielded())
                        hideCostLine()
                    } else
                    // encryptedTransfer (decrypted value of encrypted amount)
                        if (ta.isEncryptedTransfer()) {
                            ta.encrypted?.encryptedAmount?.let {
                                val amount = accountUpdater.lookupMappedAmount(it)?.toBigInteger()
                                if (amount != null) {
                                    setTotalView(if (ta.isOriginSelf()) -amount else amount)
                                } else {
                                    showDecryptedValueOfEncryptedAmount()
                                    lockImageView.setOnClickListener {
                                        decryptCallback?.onDecrypt()
                                    }
                                    hideCostLine()
                                }
                            }
                        }
                        // local (unfinalized) transactions
                        else {
                            // simpleTransfer - NOT shown
                            // transferToSecret - NOT shown
                            if (ta.isSimpleTransfer() || ta.isTransferToSecret()) {
                                // left empty intentionally (filtered out)
                            } else
                            // transferToPublic - show the amount with minus
                                if (ta.isTransferToPublic()) {
                                    setTotalView(ta.getTotalAmountForShielded())
                                    hideCostLine()
                                } else
                                // encryptedTransfer -  we know the amount because it is outgoing. We show it with minus. When getting the submission status, we save the association between the known amount and the encryptedValue we get from the submission status, as clarified with Ales from Concordium
                                    if (ta.isEncryptedTransfer()) {
                                        setTotalView(ta.getTotalAmountForShielded())
                                        hideCostLine()
                                    }
                        }
            }


            // Alert image
            if (ta.transactionStatus == TransactionStatus.ABSENT ||
                (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Reject) ||
                (ta.transactionStatus == TransactionStatus.FINALIZED && ta.outcome == TransactionOutcome.Reject)
            ) {
                alertImageView.visibility = View.VISIBLE
                titleTextView.setTextColor(colorGrey)
            } else {
                alertImageView.visibility = View.GONE
                titleTextView.setTextColor(colorBlack)
            }

            // Status image
            if (ta.transactionStatus == TransactionStatus.RECEIVED ||
                (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Ambiguous)
            ) {
                statusImageView.setImageDrawable(
                    ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_time)
                )
            } else if (ta.transactionStatus == TransactionStatus.COMMITTED) {
                statusImageView.setImageDrawable(
                    ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_ok)
                )
            } else if (ta.transactionStatus == TransactionStatus.FINALIZED) {
                statusImageView.setImageDrawable(
                    ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_ok_x2)
                )
            } else {
                statusImageView.setImageDrawable(null)
            }
        }
    }

    interface OnClickListenerInterface {
        fun onDecrypt()
    }
}
