package com.concordium.wallet.ui.common.delegates

import android.app.AlertDialog
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import org.greenrobot.eventbus.EventBus

interface IdentityStatusDelegate {
    fun identityDone(activity: FragmentActivity, identity: Identity)
    fun identityError(activity: FragmentActivity, identity: Identity)
}

class IdentityStatusDelegateImpl : IdentityStatusDelegate {
    override fun identityDone(activity: FragmentActivity, identity: Identity) {
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

    override fun identityError(activity: FragmentActivity, identity: Identity) {

    }
}
