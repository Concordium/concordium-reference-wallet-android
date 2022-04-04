package com.concordium.wallet.ui.more.moreoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dev.DevActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import kotlinx.android.synthetic.main.fragment_more_overview.view.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.progress.view.*

class MoreOverviewFragment : BaseFragment() {

    private lateinit var viewModel: MoreOverviewViewModel
    private lateinit var mainViewModel: MainViewModel

    private var versionNumberPressedCount = 0

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_more_overview, container, false)
        initializeViews(rootView)
        return rootView
    }

    override fun onResume() {
        super.onResume()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(MoreOverviewViewModel::class.java)
        mainViewModel = ViewModelProvider(
            activity!!,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        ).get(MainViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
    }

    private fun initializeViews(view: View) {
        view.progress_layout.visibility = View.GONE
        mainViewModel.setTitle(getString(R.string.more_overview_title))

        view.dev_layout.visibility = View.GONE
        view.dev_layout.setOnClickListener {
            gotoDevConfig()
        }
        if (BuildConfig.INCL_DEV_OPTIONS) {
            view.dev_layout.visibility = View.VISIBLE
        }


        view.identities.setOnClickListener {
            gotoIdentities()
        }

        view.address_book_layout.setOnClickListener {
            gotoAddressBook()
        }

        view.export_layout.setOnClickListener {
            gotoExport()
        }

        view.import_layout.setOnClickListener {
            import()
        }

        view.about_layout.setOnClickListener {
            about()
        }

        view.alter_layout.setOnClickListener {
            alterPassword()
        }


        initializeAppVersion(view)
    }

    private fun initializeAppVersion(view: View) {
        view.version_textview.text = getString(R.string.app_version, AppConfig.appVersion)
        view.version_textview.setOnClickListener {
            versionNumberPressedCount++
            if (versionNumberPressedCount >= 5) {
                Toast.makeText(
                    activity,
                    "Build " + BuildConfig.BUILD_NUMBER + "." + BuildConfig.BUILD_TIME_TICKS,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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

    private fun gotoDevConfig() {
        val intent = Intent(activity, DevActivity::class.java)
        startActivity(intent)
    }

    private fun gotoIdentities() {
        val intent = Intent(activity, IdentitiesOverviewActivity::class.java)
        startActivity(intent)
    }

    private fun gotoAddressBook() {
        val intent = Intent(activity, RecipientListActivity::class.java)
        startActivity(intent)
    }

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun import() {
        val intent = Intent(activity, ImportActivity::class.java)
        startActivity(intent)
    }

    private fun about() {
        val intent = Intent(activity, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun alterPassword() {
        val intent = Intent(activity, AlterPasswordActivity::class.java)
        startActivity(intent)
    }


    //endregion

}