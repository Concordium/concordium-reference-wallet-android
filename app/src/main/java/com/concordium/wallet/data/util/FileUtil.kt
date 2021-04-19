package com.concordium.wallet.data.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtil {

    /**
     * @exception FileNotFoundException
     */
    suspend fun saveFile(context: Context, filename: String, content: String) = withContext(Dispatchers.IO){
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray())
        }
    }

}