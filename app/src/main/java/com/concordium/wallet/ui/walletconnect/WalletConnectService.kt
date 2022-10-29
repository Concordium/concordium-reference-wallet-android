package com.concordium.wallet.ui.walletconnect

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.concordium.wallet.data.walletconnect.Params
import com.google.gson.Gson
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import org.greenrobot.eventbus.EventBus

class WalletConnectService : Service(), SignClient.WalletDelegate {
    private val binder = LocalBinder()
    private var sessionProposal: Sign.Model.SessionProposal? = null
    private var settledSessionResponseResult: Sign.Model.SettledSessionResponse.Result? = null
    private var settledSessionResponseError: Sign.Model.SettledSessionResponse.Error? = null
    private var sessionRequest: Sign.Model.SessionRequest? = null

    inner class LocalBinder : Binder() {
        fun pair(wcUri: String) {
            pairWC(wcUri)
        }
        fun ping() {
            pingWC()
        }
        fun rejectSession() {
            rejectSessionWC()
        }
        fun approveSession(accountAddress: String) {
            approveSessionWC(accountAddress)
        }
        fun approveTransaction(jsonSigned: String) {
            approveTransactionWC(jsonSigned)
        }
        fun rejectTransaction() {
            rejectTransactionWC()
        }
        fun disconnect() {
            disconnectWC()
        }
        fun getSessionName(): String {
            return sessionProposal?.name ?: ""
        }
        fun getSessionTopic(): String {
            return settledSessionResponseResult?.session?.topic ?: ""
        }
        fun getSessionRequestParamsAsString(): String? {
            return sessionRequest?.request?.params
        }
        fun getSessionRequestParams(): Params? {
            return try {
                Gson().fromJson(sessionRequest?.request?.params, Params::class.java)
            } catch (ex: Exception) {
                println("LC -> getSessionRequestParams ERROR ${ex.stackTraceToString()}")
                null
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        println("LC -> onDestroy Service")
    }

    private fun pairWC(wcUri: String) {
        SignClient.setWalletDelegate(this)
        println("LC -> CALL PAIR $wcUri")
        val pairingParams = Core.Params.Pair(wcUri)
        CoreClient.Pairing.pair(pairingParams) { modelError ->
            println("LC -> PAIR ERROR ${modelError.throwable.stackTraceToString()}")
            EventBus.getDefault().post(PairError(modelError.throwable.message ?: ""))
        }
    }

    private fun pingWC() {
        println("LC -> CALL PING ${binder.getSessionTopic()}")
        val pingParams = Sign.Params.Ping(binder.getSessionTopic())
        SignClient.ping(pingParams, object : Sign.Listeners.SessionPing {
            override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                println("LC -> PING SUCCESS ${pingSuccess.topic}")
                EventBus.getDefault().post(ConnectionState(true))
            }
            override fun onError(pingError: Sign.Model.Ping.Error) {
                println("LC -> PING ERROR ${pingError.error.stackTraceToString()}")
            }
        })
    }

    private fun rejectSessionWC() {
        println("LC -> CALL REJECT SESSION ${binder.getSessionTopic()}")
        val rejectParams = Sign.Params.Reject(sessionProposal?.proposerPublicKey ?: "", "")
        SignClient.rejectSession(rejectParams) { modelError ->
            println("LC -> REJECT SESSION ERROR ${modelError.throwable.stackTraceToString()}")
            EventBus.getDefault().post(RejectError(modelError.throwable.message ?: ""))
        }
    }

    private fun approveSessionWC(accountAddress: String) {
        println("LC -> CALL APPROVE SESSION")
        val firstNameSpace = sessionProposal?.requiredNamespaces?.entries?.firstOrNull()
        if (firstNameSpace != null) {
            val firstNameSpaceKey = firstNameSpace.key
            val firstChain = firstNameSpace.value.chains.firstOrNull()
            val methods = firstNameSpace.value.methods
            val events = firstNameSpace.value.events
            val accounts = listOf( "$firstChain:$accountAddress" )
            val namespaceValue = Sign.Model.Namespace.Session(accounts = accounts, methods = methods, events = events, extensions = null)
            val sessionNamespaces = mapOf(Pair(firstNameSpaceKey, namespaceValue))
            val approveParams = Sign.Params.Approve(
                proposerPublicKey = sessionProposal?.proposerPublicKey ?: "",
                namespaces = sessionNamespaces
            )
            SignClient.approveSession(approveParams) { modelError ->
                println("LC -> APPROVE SESSION ERROR ${modelError.throwable.stackTraceToString()}")
                EventBus.getDefault().post(ApproveError(modelError.throwable.message ?: ""))
            }
        }
    }

    private fun approveTransactionWC(jsonSigned: String) {
        println("LC -> CALL APPROVE TRANSACTION")
        val response = Sign.Params.Response(
            sessionTopic = sessionRequest?.topic ?: "",
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
                sessionRequest?.request?.id ?: -1,
                jsonSigned
            )
        )
        SignClient.respond(response) { modelError ->
            println("LC -> APPROVE TRANSACTION ERROR ${modelError.throwable.stackTraceToString()}")
        }
    }

    private fun rejectTransactionWC() {
        println("LC -> REJECT TRANSACTION")
        val responseParams = Sign.Params.Response(
            sessionTopic = sessionRequest?.topic ?: "",
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcError(
                id = sessionRequest?.request?.id ?: -1,
                code = 500,
                message = "User reject"
            )
        )
        SignClient.respond(responseParams) { modelError ->
            println("LC -> REJECT TRANSACTION ERROR ${modelError.throwable.stackTraceToString()}")
        }
    }

    private fun disconnectWC() {
        val sessionTopic = binder.getSessionTopic()
        if (sessionTopic.isNotBlank()) {
            println("LC -> CALL DISCONNECT $sessionTopic")
            val disconnectParams = Sign.Params.Disconnect(sessionTopic)
            SignClient.disconnect(disconnectParams) { modelError ->
                println("LC -> DISCONNECT ERROR ${modelError.throwable.stackTraceToString()}")
            }
        }
        sessionProposal = null
        settledSessionResponseResult = null
        settledSessionResponseError = null
        sessionRequest = null
    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        println("LC -> onConnectionStateChange ${state.isAvailable}")
    }

    override fun onError(error: Sign.Model.Error) {
        println("LC -> onError ${error.throwable.stackTraceToString()}")
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        println("LC -> onSessionDelete")
        EventBus.getDefault().post(ConnectionState(false))
    }

    override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
        println("LC -> onSessionProposal")
        this.sessionProposal = sessionProposal
        val firstNameSpace = sessionProposal.requiredNamespaces.entries.firstOrNull()
        if (firstNameSpace != null) {
            println("LC -> onSessionProposal ${sessionProposal.name}")
            EventBus.getDefault().post(sessionProposal)
        }
    }

    override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
        println("LC -> onSessionRequest ${sessionRequest.request}")
        this.sessionRequest = sessionRequest
        EventBus.getDefault().post(sessionRequest)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
        if (settleSessionResponse is Sign.Model.SettledSessionResponse.Result) {
            println("LC -> onSessionSettleResponse SUCCESS -> ${settleSessionResponse.session.topic}")
            settledSessionResponseResult = settleSessionResponse
            EventBus.getDefault().post(ConnectionState(true))
        }
        else if (settleSessionResponse is Sign.Model.SettledSessionResponse.Error) {
            println("LC -> onSessionSettleResponse ERROR -> ${settleSessionResponse.errorMessage}")
            settledSessionResponseError = settleSessionResponse
            EventBus.getDefault().post(ConnectionState(false))
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
        println("LC -> onSessionUpdateResponse $sessionUpdateResponse")
    }
}

data class PairError(
    val message: String
)

data class ConnectionState(
    val isConnected: Boolean
)

data class RejectError(
    val message: String
)

data class ApproveError(
    val message: String
)
