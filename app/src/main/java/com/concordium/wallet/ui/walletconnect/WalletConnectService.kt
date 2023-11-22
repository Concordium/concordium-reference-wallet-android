package com.concordium.wallet.ui.walletconnect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.walletconnect.Params
import com.concordium.wallet.data.walletconnect.ParamsDeserializer
import com.google.gson.GsonBuilder
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import org.greenrobot.eventbus.EventBus


class WalletConnectService : Service(), SignClient.WalletDelegate, CoreClient.CoreDelegate {
    private val binder = LocalBinder()
    private var sessionProposal: Sign.Model.SessionProposal? = null
    private var settledSessionResponseResult: Sign.Model.SettledSessionResponse.Result? = null
    private var settledSessionResponseError: Sign.Model.SettledSessionResponse.Error? = null
    private var sessionRequest: Sign.Model.SessionRequest? = null

    companion object {
        const val FOREGROUND_SERVICE = 101
        const val START_FOREGROUND_ACTION = "START_FOREGROUND_ACTION"
        const val STOP_FOREGROUND_ACTION = "STOP_FOREGROUND_ACTION"
    }

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

        fun respondSuccess(message: String) {
            respondSuccessWC(message)
        }

        fun respondError(message: String) {
            respondErrorWC(message)
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

        fun getSessionRequestParams(): Params? {
            return try {
                val jsonBuilder = GsonBuilder()
                jsonBuilder.registerTypeAdapter(
                    Params::class.java,
                    ParamsDeserializer()
                )
                return jsonBuilder.create()
                    .fromJson(sessionRequest?.request?.params, Params::class.java)
            } catch (ex: Exception) {
                println("LC -> getSessionRequestParams ERROR ${throwableRemoveLineBreaks(ex)}")
                null
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent!!.action == START_FOREGROUND_ACTION) {
            val channel = NotificationChannel(
                "WalletConnect",
                "WalletConnect Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification: Notification = NotificationCompat.Builder(this, "WalletConnect")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.wallet_connect_service_running))
                .setSmallIcon(R.drawable.ic_service_notification)
                .setOngoing(true)
                .build()

            startForeground(FOREGROUND_SERVICE, notification)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        CoreClient.setDelegate(this)
        SignClient.setWalletDelegate(this)

        CoreClient.Pairing.getPairings().forEach { pairing ->

            CoreClient.Pairing.disconnect(Core.Params.Disconnect(pairing.topic)) { modelError ->
                println("LC -> DISCONNECT ERROR in Service ${modelError.throwable.stackTraceToString()}")
            }
        }
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        println("LC -> onDestroy Service")
        disconnectWC()
    }

    private fun pairWC(wcUri: String) {
        if (matchesURLScheme(wcUri).not()) {
            EventBus.getDefault()
                .post(RejectError("Provided parameters do not match the expected URL, please restart the connection."))
            return
        }
        val uriPrams = getConnectionParams(wcUri).ifEmpty {
            EventBus.getDefault()
                .post(RejectError("Provided parameters are empty, please restart the connection.\n"))
            return
        }
        println("LC -> CALL PAIR params:$uriPrams")
        if (CoreClient.Pairing.getPairings().isEmpty()) {
            val pairingParams = Core.Params.Pair(uriPrams)
            println("LC -> PAIR IS EMPTY")

            CoreClient.Pairing.pair(pairingParams) { error ->
                println("LC -> PAIR ERROR ${throwableRemoveLineBreaks(error.throwable)}")
                EventBus.getDefault().post(PairError(error.throwable.message ?: ""))
            }
        } else {
            println("LC -> PAIR NOT EMPTY")
            CoreClient.Pairing.getPairings().forEach {
                println("LC -> PAIR EXPIRE TIME ${it.expiry}")

                CoreClient.Pairing.disconnect(Core.Params.Disconnect(it.topic)) { error ->
                    println("LC -> ERROR DISCONECCTING ${throwableRemoveLineBreaks(error.throwable)}")
                }
            }

            val pairingParams = Core.Params.Pair(wcUri)

            println("LC -> PAIR RETRY")

            CoreClient.Pairing.pair(pairingParams) { error ->
                println("LC -> PAIR ERROR ${throwableRemoveLineBreaks(error.throwable)}")
                EventBus.getDefault().post(PairError(error.throwable.message ?: ""))
            }
        }
    }

    private fun matchesURLScheme(wcUri: String): Boolean =
        Regex("wc|concordiumwallet://wc").containsMatchIn(wcUri)

    private fun getConnectionParams(wcUri: String): String {
        return when {
            wcUri.startsWith("wc:") -> wcUri
            else -> {
                try {
                    Uri.parse(wcUri)
                        .getQueryParameters("uri").first()
                } catch (e: RuntimeException) {
                    ""
                }
            }
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
                println("LC -> PING ERROR ${throwableRemoveLineBreaks(pingError.error)}")
            }
        })
    }

    private fun rejectSessionWC() {
        println("LC -> CALL REJECT SESSION ${binder.getSessionTopic()}")
        val rejectParams = Sign.Params.Reject(sessionProposal?.proposerPublicKey ?: "", "")
        SignClient.rejectSession(rejectParams) { error ->
            println("LC -> REJECT SESSION ERROR ${throwableRemoveLineBreaks(error.throwable)}")
            EventBus.getDefault().post(RejectError(error.throwable.message ?: ""))
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
            val accounts = listOf("$firstChain:$accountAddress")
            val namespaceValue = Sign.Model.Namespace.Session(
                accounts = accounts,
                methods = methods,
                events = events,
                extensions = null
            )
            val sessionNamespaces = mapOf(Pair(firstNameSpaceKey, namespaceValue))
            val approveParams = Sign.Params.Approve(
                proposerPublicKey = sessionProposal?.proposerPublicKey ?: "",
                namespaces = sessionNamespaces
            )

            SignClient.approveSession(approveParams) { error ->
                println("LC -> APPROVE SESSION ERROR ${throwableRemoveLineBreaks(error.throwable)}")
                EventBus.getDefault().post(ApproveError(error.throwable.message ?: ""))
            }
        } else {
            println("LC -> FIRST NAMESPACE IS NULL")
        }
    }

    private fun respondSuccessWC(result: String) {
        println("LC -> CALL RESPOND SUCCESS   $result")
        val response = Sign.Params.Response(
            sessionTopic = sessionRequest?.topic ?: "",
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
                sessionRequest?.request?.id ?: -1,
                result
            )
        )
        SignClient.respond(response) { error ->
            println("LC -> RESPOND ERROR ${throwableRemoveLineBreaks(error.throwable)}")
        }
    }

    private fun respondErrorWC(message: String) {
        println("LC -> CALL RESPOND ERROR   $message")
        val responseParams = Sign.Params.Response(
            sessionTopic = sessionRequest?.topic ?: "",
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcError(
                id = sessionRequest?.request?.id ?: -1,
                code = 500,
                message = message
            )
        )
        SignClient.respond(responseParams) { error ->
            println("LC -> REJECT ERROR ${throwableRemoveLineBreaks(error.throwable)}")
        }
    }

    private fun disconnectWC() {
        settledSessionResponseResult?.session?.topic?.let {
            println("LC -> SignClient disconnect from topic $it")
            SignClient.disconnect(Sign.Params.Disconnect(it)) { modelError ->
                println("LC -> SignClient disconnect ERROR ${modelError.throwable.stackTraceToString()}")
            }
        }
        val pairings: List<Core.Model.Pairing> = CoreClient.Pairing.getPairings()
        println("LC -> EXISTING PAIRINGS in Service = ${pairings.count()}")
        pairings.forEach { pairing ->
            CoreClient.Pairing.disconnect(Core.Params.Disconnect(pairing.topic)) { modelError ->
                println("LC -> DISCONNECT ERROR in Service ${modelError.throwable.stackTraceToString()}")
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
        println("LC -> onError ${throwableRemoveLineBreaks(error.throwable)}")
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        println("LC -> onSessionDelete $deletedSession")
        EventBus.getDefault().post(ConnectionState(false))
    }


    private val expectedNetworkKey = "ccd"
    private val supportedChains = listOf("$expectedNetworkKey:${BuildConfig.EXPORT_CHAIN}")
    private val supportedEvents = listOf("accounts_changed", "chain_changed")
    private val supportedMethods = listOf("sign_and_send_transaction", "sign_message")
    override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
        val ccdNetwork = sessionProposal.requiredNamespaces[expectedNetworkKey]
        if (ccdNetwork?.chains != supportedChains) {
            EventBus.getDefault()
                .post(RejectError("Expected Network:${supportedChains} but got ${ccdNetwork?.chains}"))
            return
        }
        if (supportedEvents.containsAll(ccdNetwork.events).not()) {
            EventBus.getDefault()
                .post(RejectError("Expected subset of events ${supportedEvents} but got ${ccdNetwork.events}"))
            return
        }
        if (supportedMethods.containsAll(ccdNetwork.methods).not()) {
            EventBus.getDefault()
                .post(RejectError("Expected subset of methods ${supportedMethods} but got ${ccdNetwork.methods}"))
            return
        }

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
        println("LC -> onSessionSettleResponse")
        if (settleSessionResponse is Sign.Model.SettledSessionResponse.Result) {
            println("LC -> onSessionSettleResponse SUCCESS -> ${settleSessionResponse.session.topic}")
            settledSessionResponseResult = settleSessionResponse
            EventBus.getDefault().post(ConnectionState(true))
        } else if (settleSessionResponse is Sign.Model.SettledSessionResponse.Error) {
            println("LC -> onSessionSettleResponse ERROR -> ${settleSessionResponse.errorMessage}")
            settledSessionResponseError = settleSessionResponse
            EventBus.getDefault().post(ConnectionState(false))
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
        println("LC -> onSessionUpdateResponse $sessionUpdateResponse")
    }

    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        println("LC -> onPairingDelete $deletedPairing")
        EventBus.getDefault().post(ConnectionState(false))
    }

    private fun throwableRemoveLineBreaks(throwable: Throwable): String {
        return throwable.stackTraceToString().replace("\n", " LINEBREAK ")
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
