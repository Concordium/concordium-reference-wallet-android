package com.concordium.wallet.data.model

import java.io.Serializable

data class BakerPoolStatus(
    val poolType: String,
    val bakerId: Int,
    val bakerEquityCapital: String,
    val delegatedCapitalCap: String,
    val bakerAddress: String,
    val delegatedCapital: String,
    val currentPaydayStatus: PayDayStatus,
    val poolInfo: BakerPoolInfo,
    val bakerStakePendingChange: BakerStakePendingChange

) : Serializable

/*

{
x	"poolType": "BakerPool",
x	"bakerId": 0,
x	"bakerEquityCapital": "3000000000000",
x	"delegatedCapitalCap": "1000500230710",
	"poolInfo": {
		"commissionRates": {
			"transactionCommission": 5.0e-2,
			"finalizationCommission": 1.0,
			"bakingCommission": 5.0e-2
		},
		"openStatus": "openForAll",
		"metadataUrl": ""
	},
	"bakerStakePendingChange": {
		"pendingChangeType": "NoChange"
	},
x	"bakerAddress": "2zmRFpd7g12oBAZHSDqnbJ3Eg5HGr2sE9aFCL6mD3pyUSsiDSJ",
x	"delegatedCapital": "1500480508",
x	"currentPaydayStatus": {
x		"finalizationLive": true,
x		"effectiveStake": "3001500480508",
x		"transactionFeesEarned": "0",
x		"bakerEquityCapital": "3000000000000",
x		"lotteryPower": 0.20008001762276778,
x		"blocksBaked": 27,
x		"delegatedCapital": "1500480508"
x	}
}

*/