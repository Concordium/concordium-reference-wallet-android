package com.concordium.wallet.ui.more.import

import android.content.Context
import android.net.Uri
import com.concordium.wallet.data.util.StorageAccessFrameworkUtil
import java.io.IOException

data class ImportFile(
    val uri: Uri
) {
    @Throws(IOException::class)
    suspend fun getContentAsString(context: Context): String {
        return StorageAccessFrameworkUtil.readFileContent(context, uri)
    }
}