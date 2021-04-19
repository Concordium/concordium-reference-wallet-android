package com.concordium.wallet.ui.account.accountqrcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.android.synthetic.main.activity_account_qr_code.*

class AccountQRCodeActivity :
    BaseActivity(R.layout.activity_account_qr_code, R.string.account_qr_code_title) {

    private lateinit var viewModel: AccountQRCodeViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras!!.getSerializable(SendFundsActivity.EXTRA_ACCOUNT) as Account
        initializeViewModel()
        viewModel.initialize(account)
        initViews()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AccountQRCodeViewModel::class.java)
    }

    private fun initViews() {
        val qrImage = generateQR("${viewModel.account.address}")
        if (qrImage != null) {
            address_qr_imageview.setImageBitmap(qrImage)
        }
        account_title_textview.text = viewModel.account.name + ":"
        address_qr_textview.text = viewModel.account.address
        share_layout.setOnClickListener {
            shareAddress()
        }
        copy_address_layout.setOnClickListener {
            copyAddress()
        }
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun shareAddress() {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, viewModel.account.address)
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun copyAddress() {
        val clipboardManager: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("txt", viewModel.account.address)
        clipboardManager.setPrimaryClip(clipData)
        popup.showSnackbar(root_layout, R.string.account_qr_code_copied)
    }

    fun generateQR(qrCodeContent: String): Bitmap? {
        try {
            var color = getColor(R.color.theme_component)
            val writer = QRCodeWriter()
            val bitMatrix: BitMatrix =
                writer.encode(qrCodeContent, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) color else Color.BLACK
                }
            }

            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }

    //endregion
}
