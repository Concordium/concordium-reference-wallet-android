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
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.concurrent.schedule

interface IdentityStatusDelegate {
    fun startCheckForPendingIdentity(activity: ComponentActivity?, specificIdentityId: Int?, showForFirstIdentity: Boolean, statusChanged: (Identity) -> Unit)
    fun identityDone(activity: ComponentActivity, identity: Identity, statusChanged: (Identity) -> Unit)
    fun identityError(activity: ComponentActivity, identity: Identity, statusChanged: (Identity) -> Unit)
    fun stopCheckForPendingIdentity()
}

class IdentityStatusDelegateImpl : IdentityStatusDelegate {
    private var job: Job? = null
    private var showForFirstIdentity = false

    override fun startCheckForPendingIdentity(activity: ComponentActivity?, specificIdentityId: Int?, showForFirstIdentity: Boolean, statusChanged: (Identity) -> Unit) {
        this.showForFirstIdentity = showForFirstIdentity
        if (activity == null || activity.isFinishing || activity.isDestroyed)
            return
        if (App.appCore.newIdentities.isNotEmpty()) {
            for (newIdentity in App.appCore.newIdentities) {
                if (specificIdentityId == null || specificIdentityId == newIdentity.key) {
                    CoroutineScope(Dispatchers.IO).launch {
                        job = launch {
                            val identityRepository = IdentityRepository(WalletDatabase.getDatabase(activity).identityDao())
                            val identity = identityRepository.findById(newIdentity.key)
                            identity?.let {
                                activity.runOnUiThread {
                                    if ((activity as BaseActivity).isActive) {
                                        when (identity.status) {
                                            IdentityStatus.DONE -> identityDone(activity, identity, statusChanged)
                                            IdentityStatus.ERROR -> identityError(activity, identity, statusChanged)
                                        }
                                    }
                                    startCheckForPendingIdentity(activity, specificIdentityId, showForFirstIdentity, statusChanged)
                                }
                            }
                            delay(1000)
                        }
                    }
                }
            }
        } else {
            Timer().schedule(1000) {
                startCheckForPendingIdentity(activity, specificIdentityId, showForFirstIdentity, statusChanged)
            }
        }
    }

    override fun stopCheckForPendingIdentity() {
        job?.cancel()
    }

    override fun identityDone(activity: ComponentActivity, identity: Identity, statusChanged: (Identity) -> Unit) {
        if (App.appCore.newIdentities[identity.id] == null)
            return
        App.appCore.newIdentities.remove(identity.id)

        if (showForFirstIdentity) {
            statusChanged(identity)
            return
        }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.identities_overview_identity_verified_title)
        builder.setMessage(activity.getString(R.string.identities_overview_identity_verified_message, identity.name))
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
        statusChanged(identity)
        dialog.show()
    }

    override fun identityError(activity: ComponentActivity, identity: Identity, statusChanged: (Identity) -> Unit) {
        if (App.appCore.newIdentities[identity.id] == null)
            return
        App.appCore.newIdentities.remove(identity.id)

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.identities_overview_identity_rejected_title)

        if (showForFirstIdentity)
            identityErrorFirstIdentity(activity, identity, builder)
        else
            identityErrorNextIdentity(activity, identity, builder, statusChanged)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        statusChanged(identity)
        dialog.show()
    }

    private fun identityErrorNextIdentity(activity: ComponentActivity, identity: Identity, builder: AlertDialog.Builder, statusChanged: (Identity) -> Unit) {
        builder.setMessage(activity.getString(R.string.identities_overview_identity_rejected_text, "${identity.name}\n${identity.detail ?: ""}"))
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_request_another)) { dialog, _ ->
            dialog.dismiss()
            activity.startActivity(Intent(activity, IdentityProviderListActivity::class.java))
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_identity_later)) { dialog, _ ->
            dialog.dismiss()
            statusChanged(identity)
        }
    }

    private fun identityErrorFirstIdentity(activity: ComponentActivity, identity: Identity, builder: AlertDialog.Builder) {
        builder.setMessage(activity.getString(R.string.identities_overview_identity_rejected_first_text, "${identity.name}\n${identity.detail ?: ""}"))
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_make_new)) { dialog, _ ->
            dialog.dismiss()
            activity.finish()
            val intent = Intent(activity, IdentityProviderListActivity::class.java)
            intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
            activity.startActivity(intent)
        }
    }
}
