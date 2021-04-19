package com.concordium.wallet.ui.recipient.recipientlist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.concordium.wallet.uicore.recyclerview.touchlistener.RecyclerTouchListener
import com.concordium.wallet.util.Log
import kotlinx.android.synthetic.main.activity_recipient_list.*
import kotlinx.android.synthetic.main.progress.*


class RecipientListActivity :
    BaseActivity(R.layout.activity_recipient_list, R.string.recipient_list_default_title) {

    companion object {
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    private lateinit var viewModel: RecipientListViewModel
    private lateinit var recipientAdapter: RecipientAdapter


    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectRecipientMode = intent.getBooleanExtra(EXTRA_SELECT_RECIPIENT_MODE, false)
        val isShielded = intent.getBooleanExtra(EXTRA_SHIELDED, false)
        val account = intent.getSerializableExtra(EXTRA_ACCOUNT) as? Account

        initializeViewModel()
        viewModel.initialize(selectRecipientMode, isShielded, account)
        initializeViews()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.add_item_menu -> {
                gotoNewRecipient()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(RecipientListViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.recipientListLiveData.observe(this, Observer {
            it.let {
                recipientAdapter.setData(it)
                showWaiting(false)
            }
        })
    }

    private fun initializeViews() {
        showWaiting(true)
        if (viewModel.selectRecipientMode) {
            setActionBarTitle(R.string.recipient_list_select_title)
        } else {
            // Hide shield/unshield function in address book
            recipient_shield_container.visibility = View.GONE
        }
        scan_qr_imageview.setOnClickListener {
            gotoScanBarCode()
        }

        recipient_searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(txt: String?): Boolean {
                recipientAdapter.filter(txt)
                return true
            }

            override fun onQueryTextChange(txt: String?): Boolean {
                recipientAdapter.filter(txt)
                return true
            }
        })
        initializeList()
        initializeListSwipe()

        /*
        viewModel.account?.let {
            val shieldHeaderTextRes = if (viewModel.isShielded) R.string.recipient_list_unshield_amount else R.string.recipient_list_shield_amount
            recipient_own_account.findViewById<TextView>(R.id.recipient_name_textview).setText(shieldHeaderTextRes)
            recipient_own_account.findViewById<TextView>(R.id.recipient_address_textview).setText(it.address)
        }
        recipient_own_account.setOnClickListener(View.OnClickListener {
            viewModel.account?.let {
                goBackToSendFunds(Recipient(it.id, it.name, it.address))
            }
        })*/
    }

    private fun initializeList() {
        recipientAdapter = RecipientAdapter()
        recyclerview.setHasFixedSize(true)
        recyclerview.adapter = recipientAdapter

        recipientAdapter.setOnItemClickListener(object :
            RecipientAdapter.OnItemClickListener {
            override fun onItemClicked(item: Recipient) {
                if (viewModel.selectRecipientMode) {
                    goBackToSendFunds(item)
                } else {
                    gotoEditRecipient(item)
                }

            }
        })

        //val swipeController = SwipeController()
        //val itemTouchhelper = ItemTouchHelper(swipeController)
        //itemTouchhelper.attachToRecyclerView(recyclerview)
    }

    private fun initializeListSwipe() {
        val touchListener = RecyclerTouchListener(this, recyclerview)
        touchListener
            .setSwipeOptionViews(R.id.delete_item_layout)
            .setSwipeable(
                R.id.foreground_root,
                R.id.background_root,
                resources.getDimension(R.dimen.item_recipient_delete_width).toInt(),
                object : RecyclerTouchListener.OnSwipeOptionsClickListener {
                    override fun onSwipeOptionClicked(viewID: Int, position: Int) {
                        when (viewID) {
                            R.id.delete_item_layout -> {
                                Log.d("Delete")
                                viewModel.deleteRecipient(recipientAdapter.get(position))
                            }
                        }
                    }
                })
        recyclerview.addOnItemTouchListener(touchListener)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun gotoScanBarCode() {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(RecipientActivity.EXTRA_GOTO_SCAN_QR, true)
        startActivity(intent)
    }

    private fun gotoNewRecipient() {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(
            RecipientActivity.EXTRA_SELECT_RECIPIENT_MODE,
            viewModel.selectRecipientMode
        )
        startActivity(intent)
    }

    private fun gotoEditRecipient(recipient: Recipient) {
        val intent = Intent(this, RecipientActivity::class.java)
        intent.putExtra(RecipientActivity.EXTRA_RECIPIENT, recipient)
        startActivity(intent)
    }

    private fun goBackToSendFunds(recipient: Recipient) {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_RECIPIENT, recipient)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    //endregion
}
