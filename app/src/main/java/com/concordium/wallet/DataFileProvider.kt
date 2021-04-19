package com.concordium.wallet

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

/**
 * This class provides access for other apps to the shared exported favorites
 */
class DataFileProvider : ContentProvider() {

    companion object {
        private const val TAG = "DataFileProvider"
        private const val MIME_TYPE = "application/json"
        const val AUTHORITY = BuildConfig.PROVIDER_AUTHORITY
    }

    private var uriMatcher: UriMatcher? = null

    override fun onCreate(): Boolean {
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        uriMatcher!!.addURI(AUTHORITY, "*", 1)
        return true
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        Log.v(TAG, "Called with uri: '" + uri)
        return when (uriMatcher!!.match(uri)) {
            1 -> { //Success
                val fileLocation = (context!!.filesDir.toString() + File.separator + uri.lastPathSegment)
                ParcelFileDescriptor.open(File(fileLocation), ParcelFileDescriptor.MODE_READ_ONLY)
            }
            else -> {
                Log.v(TAG, "Unsupported uri: '$uri'.")
                throw FileNotFoundException("Unsupported uri: " + uri.toString())
            }
        }
    }

    // not used
    override fun update(uri: Uri, contentvalues: ContentValues?, s: String?, `as`: Array<String>?): Int {
        return 0
    }

    // not used
    override fun delete(uri: Uri, s: String?, `as`: Array<String>?): Int {
        return 0
    }

    // not used
    override fun insert(uri: Uri, contentvalues: ContentValues?): Uri? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return MIME_TYPE
    }

    /**
     * This is std way of making a query about the available content.
     * Other apps will ask for certain parameters which the query will respond to.
     * Purely for presentation in other apps.
     * @param uri
     * @param projectionArg
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    override fun query(uri: Uri, projectionArg: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        var projection = projectionArg
        if (projection == null) {
            projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.MIME_TYPE
            )
        }
        val fileLocation = context!!.filesDir.toString() + File.separator + uri.lastPathSegment
        val file = File(fileLocation)
        val result = MatrixCursor(projection)
        val row = arrayOfNulls<Any>(projection.size)
        for (i in projection.indices) {
            if (projection[i].compareTo(MediaStore.MediaColumns.DISPLAY_NAME, ignoreCase = true) == 0
            ) {
                row[i] = uri.lastPathSegment
            } else if (projection[i].compareTo(MediaStore.MediaColumns.SIZE, ignoreCase = true) == 0
            ) {
                row[i] = file.length()
            } else if (projection[i].compareTo(MediaStore.MediaColumns.MIME_TYPE, ignoreCase = true) == 0
            ) {
                row[i] = MIME_TYPE
            } else if (projection[i].compareTo(MediaStore.MediaColumns._ID, ignoreCase = true) == 0
            ) {
                row[i] = 0
            } else if (projection[i].compareTo("orientation", ignoreCase = true) == 0) {
                row[i] = "vertical"
            }
        }
        result.addRow(row)
        return result
    }

}