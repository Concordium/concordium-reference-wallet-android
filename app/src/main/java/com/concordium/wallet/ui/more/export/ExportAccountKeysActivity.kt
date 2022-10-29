package com.concordium.wallet.ui.more.export

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.data.model.AccountDataKeys
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityExportAccountKeysBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable
import com.google.gson.Gson
import javax.crypto.Cipher

class ExportAccountKeysActivity : BaseActivity() {
    private lateinit var binding: ActivityExportAccountKeysBinding
    private val viewModel: ExportAccountKeysViewModel by viewModels()

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportAccountKeysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbarLayout.toolbar, binding.toolbarLayout.toolbarTitle, R.string.export_account_keys_title)
        viewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.textTap.text = getString(R.string.export_account_keys_tap, viewModel.account.name)
        binding.hidden.setOnClickListener {
            reveal()
        }
        binding.copy.setOnClickListener {
            copyToClipboard()
        }
        binding.exportToFile.setOnClickListener {
            openFolderPicker(getResultFolderPicker)
        }
        binding.done.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initObservers() {
        viewModel.textResourceInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
        viewModel.accountData.observe(this) {
            binding.hidden.visibility = View.GONE
            binding.revealed.visibility = View.VISIBLE
            viewModel.accountDataKeys = Gson().fromJson(it.keys.json, AccountDataKeys::class.java)
            val signKey = viewModel.accountDataKeys.level0.keys.keys.signKey
            if (signKey.isNotBlank() && signKey.length == 64) {
                binding.key.text = "${signKey.substring(0, 32)}\n${signKey.substring(32, 64)}"
            }
        }
    }

    private fun reveal() {
        showAuthentication(authenticateText(), object : AuthenticationCallback {
            override fun getCipherForBiometrics() : Cipher? {
                return viewModel.getCipherForBiometrics()
            }
            override fun onCorrectPassword(password: String) {
                viewModel.continueWithPassword(password)
            }
            override fun onCipher(cipher: Cipher) {
                viewModel.checkLogin(cipher)
            }
            override fun onCancelled() {
            }
        })
    }

    private fun copyToClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("key", viewModel.accountDataKeys.level0.keys.keys.signKey)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, R.string.export_account_keys_copied, Toast.LENGTH_SHORT).show()
    }

    private val getResultFolderPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { uri ->
                    viewModel.saveFileToLocalFolder(uri)
                }
            }
        }
}
