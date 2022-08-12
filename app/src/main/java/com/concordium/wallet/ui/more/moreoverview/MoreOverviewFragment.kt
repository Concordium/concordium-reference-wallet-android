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
import com.concordium.wallet.databinding.FragmentMoreOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dev.DevActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity

class MoreOverviewFragment : BaseFragment() {
    private var _binding: FragmentMoreOverviewBinding? = null
    private val binding get() = _binding!!
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreOverviewBinding.inflate(inflater, container, false)
        initializeViews()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MoreOverviewViewModel::class.java]
        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MainViewModel::class.java]

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })
    }

    private fun initializeViews() {
        binding.includeProgress.progressLayout.visibility = View.GONE
        mainViewModel.setTitle(getString(R.string.more_overview_title))

        binding.devLayout.visibility = View.GONE
        binding.devLayout.setOnClickListener {
            gotoDevConfig()
        }

        if (BuildConfig.INCL_DEV_OPTIONS) {
            binding.devLayout.visibility = View.VISIBLE
        }

        binding.identities.setOnClickListener {
            mainViewModel.setState(MainViewModel.State.IdentitiesOverview)
            //gotoIdentities()
        }

        binding.addressBookLayout.setOnClickListener {
            gotoAddressBook()
        }

        binding.aboutLayout.setOnClickListener {
            about()
        }

        binding.alterLayout.setOnClickListener {
            alterPassword()
        }

        initializeAppVersion()
    }

    private fun initializeAppVersion() {
        binding.versionTextview.text = getString(R.string.app_version, AppConfig.appVersion)
        binding.versionTextview.setOnClickListener {
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
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoDevConfig() {
        val intent = Intent(activity, DevActivity::class.java)
        startActivity(intent)
    }

    private fun gotoAddressBook() {
        val intent = Intent(activity, RecipientListActivity::class.java)
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
