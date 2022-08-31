package com.concordium.wallet.ui.common.delegates

import android.app.AlertDialog
import android.content.Intent
import androidx.core.app.ComponentActivity
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

interface IdentityStatusDelegate {
    fun startCheckForPendingIdentity(activity: ComponentActivity?)
    fun identityDone(activity: ComponentActivity, identity: Identity)
    fun identityError(activity: ComponentActivity, identity: Identity)
    fun stopCheckForPendingIdentity()
}

class IdentityStatusDelegateImpl : IdentityStatusDelegate {
    private var job: Job? = null

    override fun startCheckForPendingIdentity(activity: ComponentActivity?) {
        App.appCore.newIdentityPending?.let { pendingIdentity ->
            CoroutineScope(Dispatchers.IO).launch {
                job = launch {
                    activity?.let {
                        val identityDao = WalletDatabase.getDatabase(activity).identityDao()
                        val identityRepository = IdentityRepository(identityDao)
                        val identity = identityRepository.findById(pendingIdentity.id)
                        identity?.let {
                            activity.runOnUiThread {
                                when (identity.status) {
                                    IdentityStatus.PENDING -> startCheckForPendingIdentity(activity)
                                    IdentityStatus.DONE -> identityDone(activity, identity)
                                    IdentityStatus.ERROR -> identityError(activity, identity)
                                }
                            }
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    override fun stopCheckForPendingIdentity() {
        job?.cancel()
    }

    override fun identityDone(activity: ComponentActivity, identity: Identity) {
        App.appCore.newIdentityPending = null
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.identities_overview_identity_verified_title)
        builder.setMessage(activity.getString(R.string.identities_overview_identity_verified_message, identity.id.toString()))
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_create_account_now)) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(activity, IdentityConfirmedActivity::class.java)
            intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
            intent.putExtra(IdentityConfirmedActivity.SHOW_FOR_CREATE_ACCOUNT, true)
            activity.startActivity(intent)
            EventBus.getDefault().post(MainViewModel.State.AccountOverview)
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_identity_later)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun identityError(activity: ComponentActivity, identity: Identity) {
        App.appCore.newIdentityPending = null
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.identities_overview_identity_rejected_title)
        builder.setMessage(activity.getString(R.string.identities_overview_identity_rejected_text, identity.id.toString()))
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_request_another)) { dialog, _ ->
            dialog.dismiss()
            activity.startActivity(Intent(activity, IdentityProviderListActivity::class.java))
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_identity_later)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}
