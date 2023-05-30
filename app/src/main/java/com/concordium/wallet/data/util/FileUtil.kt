package com.concordium.wallet.data.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.concordium.wallet.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtil {

    /**
     * @exception FileNotFoundException
     */
    suspend fun saveFile(context: Context, filename: String, content: String) =
        withContext(Dispatchers.IO) {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        }

    suspend fun writeFile(destinationUri: Uri, destinationFileName: String, fileContent: String) {
        withContext(Dispatchers.IO) {
            val doc = DocumentFile.fromTreeUri(App.appContext, destinationUri)
            val file = doc?.createFile("txt", destinationFileName)
            file?.let {
                val outputStream = App.appContext.contentResolver.openOutputStream(it.uri)
                outputStream?.write(fileContent.toByteArray())
                outputStream?.close()
            }
        }
    }
}
