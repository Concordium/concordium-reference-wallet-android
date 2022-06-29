package com.concordium.wallet.data.backend

import android.net.UrlQuerySanitizer
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.model.*
import com.concordium.wallet.util.AssetUtil
import com.concordium.wallet.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

class OfflineMockInterceptor : Interceptor {
    companion object {
        const val initialTimestampSecs = 1588593939 // Monday, May 4, 2020 12:05:39 PM
    }

    private var submissionStatusCounter = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        if (BuildConfig.DEBUG) {
            response = transformResponse(chain)
        }

        if (response == null) {
            response = chain.proceed(chain.request())
        }

        return response
    }

    private fun transformResponse(chain: Interceptor.Chain): Response? {
        var asset = ""
        val responseCode = 200

        val requestUri = chain.request().url.toUri()
        val bodyJson = bodyToString(chain.request())

        Log.d("Uri: $requestUri")
        Log.d("Request body: $bodyJson")

        // Delay requests to be able to see waiting states
        //Thread.sleep(1000)

        val requestString = requestUri.toString()
        if (requestString.contains("ip_info")) {
            asset = "1.1.2.RX-backend_identity_provider_info.json"
        } else if (requestString.contains("global")) {
            asset = "2.1.2.RX-backend_global.json"
        } else if (requestString.contains("request_id")) {
            asset = "1.3.2.RX-backend_request_identity.json"
        } else if (requestString.contains("submitCredential")) {
            asset = "2.3.2.RX_backend_submitCredential.json"
        } else if (requestString.contains("submissionStatus")) {
            // Handles both account and transfer submissionStatus
            val index = requestString.lastIndexOf("/")
            val submissionId = requestString.substring(index + 1)
            asset = when (submissionId) {
                "a01" -> "2.4.2.RX_backend_submissionStatus.json"
                "a02" -> "2.4.2.RX_backend_submissionStatus_received.json"
                "a03" -> "2.4.2.RX_backend_submissionStatus_committed.json"
                "a04" -> "2.4.2.RX_backend_submissionStatus_absent.json"
                "t01" -> "3.4.2.RX_backend_submissionStatus_rec.json"
                "t02" -> "3.4.2.RX_backend_submissionStatus_com.json"
                "t03" -> "3.4.2.RX_backend_submissionStatus_com_amb.json"
                "t04" -> "3.4.2.RX_backend_submissionStatus_com_reject.json"
                "t05" -> "3.4.2.RX_backend_submissionStatus_abs.json"
                "t06" -> "3.4.2.RX_backend_submissionStatus_fin.json"
                "t07" -> "3.4.2.RX_backend_submissionStatus_fin_reject.json"
                else -> "2.4.2.RX_backend_submissionStatus.json"
            }

            submissionStatusCounter++

            // Test error
            //asset = "2.4.3.RX_backend_submissionStatus_error.json"
            //responseCode = 400

        } else if (requestString.contains("accNonce")) {
            asset = "3.1.2.RX_backend_accNonce.json"
        } else if (requestString.contains("submitTransfer")) {
            asset = "3.3.2.RX_backend_submitTransfer.json"
        } else if (requestString.contains("simpleTransferCost")) {
            asset = "RX_backend_simpleTransferCost.json"
        } else if (requestString.contains("accBalance")) {
            val index = requestString.lastIndexOf("/")
            val address = requestString.substring(index + 1)
            asset = when (address) {
                "a02" -> "RX_backend_accBalance.json"
                else -> "RX_backend_accBalance.json"
            }
        } else if (requestString.contains("accTransactions")) {
            //Thread.sleep(1000)
            // Comment this in to test with transactions with different attributes
            //asset = "RX_backend_accTransactions.json"
            // Comment this in to test paging - otherwise more different transactions will be returned
            return createResponse(createTransactionListResponse(requestString), chain, responseCode)
        } else if (requestString.contains("testnetGTUDrop")) {
            asset = "RX_backend_testnet_gtudrop.json.json"
        }

        if (asset == "") {
            return null
        }

        return createResponse(loadAsset(asset), chain, responseCode)
    }

    private fun loadAsset(asset: String): String {
        return AssetUtil.loadFromAsset(App.appContext, asset)
    }

    private fun createResponse(
        responseString: String,
        chain: Interceptor.Chain,
        code: Int = 200
    ): Response {
        return Response.Builder()
            .code(code)
            .message(responseString)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_0)
            .body(responseString.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }

    private fun bodyToString(request: Request): String {
        return try {
            val copy = request.newBuilder().build()
            val buffer = Buffer()
            copy.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: IOException) {
            ""
        }
    }

    private val ONE_HOUR_IN_SECS = 60 * 60
    private var transactionListRequestCount = 1
    private var transactionListBaselineTimestamp = initialTimestampSecs

    private fun createTransactionListResponse(requestString: String): String {

        val sanitizer = UrlQuerySanitizer(requestString)
        val fromValue: String? = sanitizer.getValue("from")

        if (fromValue == null) {
            transactionListRequestCount = 1
            transactionListBaselineTimestamp = initialTimestampSecs
        }

        val limit = 20
        var count = 20
        if (transactionListRequestCount > 3) {
            count = 5
        }
        transactionListRequestCount++
        return createTransactionListResponse(limit, count)
    }

    private fun createTransactionListResponse(limit: Int, count: Int): String {
        val list: MutableList<RemoteTransaction> = ArrayList()

        val origin = TransactionOrigin(TransactionOriginType.Self, null)
        val details = TransactionDetails(
            TransactionType.TRANSFER,
            "desc.",
            TransactionOutcome.Success,
            "reason",
            emptyList(),
            "source",
            "Remote",
            -10059,
            0,
            "",
            "",
            "",
            "",
        0,
        "",
        "")

        var timeStamp = transactionListBaselineTimestamp
        for (i in 1..count) {
            val transaction = RemoteTransaction(
                i,
                origin,
                "013c6d2dd67affd6f39b9a7b255d244055b53d68fe8b0add4839a20e911d04cb",
                timeStamp.toDouble(),
                "84bf1e2ef8d3af3063cdb681932990f71ddb3949655f55307a266e5d687b414f",
                -10000,
                59,
                -10059,
                59,
                details,
                null
            )
            list.add(transaction)
            timeStamp -= ONE_HOUR_IN_SECS
        }
        transactionListBaselineTimestamp -= ONE_HOUR_IN_SECS * 24

        val response = AccountTransactions("desc", 0, limit, count, list)

        return App.appCore.gson.toJson(response)
    }
}
