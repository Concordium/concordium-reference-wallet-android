package com.concordium.wallet.ui.walletconnect.proof.of.identity

import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.*
import com.concordium.wallet.util.AttributeInRangeUtil
import com.concordium.wallet.util.AttributeInSetUtil
import com.concordium.wallet.util.AttributeNotInSetUtil
import com.concordium.wallet.util.AttributeRevealUtil
import java.util.*
import kotlin.collections.HashMap

class ProofOfIdentityHelper(
    private val challenge: String,
    private val proofOfIdentity: ProofOfIdentity,
    private val data: HashMap<String, String>,
) {

    companion object {
        private val EU_MEMBERS = listOf(
            "AT",
            "BE",
            "BG",
            "CY",
            "CZ",
            "DK",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "HU",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PL",
            "PT",
            "RO",
            "SK",
            "SI",
            "ES",
            "SE",
            "HR",
        )
    }

    private val proofsReveal = ArrayList<ProofReveal>()
    private val proofsZeroKnowledge = ArrayList<ProofZeroKnowledge>()

    fun getProofs(): Proofs {

        var revealStatus = true
        var zeroKnowledgeStatus = true

        for (statement in proofOfIdentity.statement!!) {
            when (statement.type) {
                AttributeType.REVEAL_ATTRIBUTE.type -> {
                    AttributeRevealUtil.revealProof(statement, data).let {
                        if (it.status != true) {
                            revealStatus = false
                        }
                        proofsReveal.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_IN_SET.type -> {
                    AttributeInSetUtil.getZeroProofKnowledge(statement, data, EU_MEMBERS).let {
                        if (it.status != true) {
                            zeroKnowledgeStatus = false
                        }
                        proofsZeroKnowledge.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_NOT_IN_SET.type -> {
                    AttributeNotInSetUtil.getZeroProofKnowledge(statement, data, EU_MEMBERS).let {
                        if (it.status != true) {
                            zeroKnowledgeStatus = false
                        }
                        proofsZeroKnowledge.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_IN_RANGE.type -> {
                    AttributeInRangeUtil.attributeInRange(statement, data).let {
                        if (it.status != true) {
                            zeroKnowledgeStatus = false
                        }
                        proofsZeroKnowledge.add(it)
                    }
                }
                else -> {
                    revealStatus = false
                    zeroKnowledgeStatus = false
                    val unknown = App.appContext.getString(R.string.proof_of_identity_not_available)
                    proofsZeroKnowledge.add(
                        ProofZeroKnowledge(
                            AttributeType.ATTRIBUTE_IN_SET,
                            AttributeTag.UNKNOWN,
                            unknown,
                            unknown,
                            null,
                            null,
                            unknown,
                            false
                        )
                    )
                }
            }
        }
        return Proofs(
            challenge,
            null,
            revealStatus,
            zeroKnowledgeStatus,
            proofsReveal,
            proofsZeroKnowledge
        )

    }
}