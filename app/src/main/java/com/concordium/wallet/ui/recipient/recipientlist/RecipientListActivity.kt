package com.concordium.wallet.ui.recipient.recipientlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.databinding.ActivityRecipientListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.recipient.recipient.RecipientActivity
import com.concordium.wallet.uicore.recyclerview.touchlistener.RecyclerTouchListener
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.getSerializable

class RecipientListActivity : BaseActivity() {
    companion object {
        const val EXTRA_SELECT_RECIPIENT_MODE = "EXTRA_SELECT_RECIPIENT_MODE"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private lateinit var binding: ActivityRecipientListBinding
    private lateinit var viewModel: RecipientListViewModel
    private lateinit var recipientAdapter: RecipientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipientListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(
            binding.toolbarLayout.toolbar,
            binding.toolbarLayout.toolbarTitle,
            R.string.recipient_list_default_title
        )

        val selectRecipientMode = intent.getBooleanExtra(EXTRA_SELECT_RECIPIENT_MODE, false)
        val isShielded = intent.getBooleanExtra(EXTRA_SHIELDED, false)

        val account = if (intent.hasExtra(EXTRA_ACCOUNT)) intent.getSerializable(EXTRA_ACCOUNT, Account::class.java) else null

        initializeViewModel()
        viewModel.initialize(selectRecipientMode, isShielded, account)
        initializeViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[RecipientListViewModel::class.java]
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.recipientListLiveData.observe(this) {
            it.let {
                recipientAdapter.setData(it)
                showWaiting(false)
            }
        }
    }

    private fun initializeViews() {
        showWaiting(true)
        if (viewModel.selectRecipientMode) {
            setActionBarTitle(R.string.recipient_list_select_title)
        } else {
            // Hide shield/unshield function in address book
            binding.recipientShieldContainer.visibility = View.GONE
        }
        binding.scanQrImageview.setOnClickListener {
            gotoScanBarCode()
        }

        binding.recipientSearchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
    }

    private fun initializeList() {
        recipientAdapter = RecipientAdapter()
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = recipientAdapter

        recipientAdapter.setOnItemClickListener(object :
            RecipientAdapter.OnItemClickListener {
            override fun onItemClicked(item: Recipient) {
                if (viewModel.selectRecipientMode) {
                    goBackWithRecipient(item)
                } else {
                    gotoEditRecipient(item)
                }

            }
        })
    }

    private fun initializeListSwipe() {
        val touchListener = RecyclerTouchListener(this, binding.recyclerview)
        touchListener
            .setSwipeOptionViews(R.id.delete_item_layout)
            .setSwipeable(
                R.id.foreground_root,
                R.id.background_root,
                resources.getDimension(R.dimen.item_recipient_delete_width).toInt()) { viewID, position ->
                when (viewID) {
                    R.id.delete_item_layout -> {
                        Log.d("Delete")
                        viewModel.deleteRecipient(recipientAdapter.get(position))
                    }
                }
            }
        binding.recyclerview.addOnItemTouchListener(touchListener)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
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

    private fun goBackWithRecipient(recipient: Recipient) {
        val intent = Intent()
        intent.putExtra(EXTRA_RECIPIENT, recipient)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
