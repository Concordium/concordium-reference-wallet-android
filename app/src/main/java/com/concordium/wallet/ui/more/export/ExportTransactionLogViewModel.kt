package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Url

interface FileDownloadApi {
    @Streaming
    @GET suspend fun downloadZipFile(@Url url: String?): ResponseBody
}

sealed class FileDownloadScreenState {
    object Idle : FileDownloadScreenState()
    data class Downloading(val progress: Int, val bytesProgress: Long, val bytesTotal: Long) : FileDownloadScreenState()
    data class Failed(val error: Throwable? = null) : FileDownloadScreenState()
    object Downloaded : FileDownloadScreenState()
}

class ExportTransactionLogViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    private lateinit var api: FileDownloadApi

    val textResourceInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    private sealed class DownloadState {
        data class Downloading(val progress: Int, val bytesProgress: Long, val bytesTotal: Long) : DownloadState()
        object Finished : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    val downloadState: MutableLiveData<FileDownloadScreenState> by lazy { MutableLiveData<FileDownloadScreenState>() }

    fun onIdleRequested() {
        downloadState.value = FileDownloadScreenState.Idle
    }

    fun downloadFile(destinationFolder: Uri) {
        val downloadFile = "statement?accountAddress=${account.address}"
        //val downloadFile = "100MB.bin"
        viewModelScope.launch(Dispatchers.IO) {
            api.downloadZipFile(downloadFile)
                .saveFile(destinationFolder)
                .collect { downloadState ->
                    this@ExportTransactionLogViewModel.downloadState.postValue(when (downloadState) {
                        is DownloadState.Downloading -> {
                            FileDownloadScreenState.Downloading(progress = downloadState.progress, downloadState.bytesProgress, downloadState.bytesTotal)
                        }
                        is DownloadState.Failed -> {
                            FileDownloadScreenState.Failed(error = downloadState.error)
                        }
                        DownloadState.Finished -> {
                            FileDownloadScreenState.Downloaded
                        }
                    })
                }
        }
    }

    private fun ResponseBody.saveFile(destinationFolder: Uri): Flow<DownloadState> {
        return flow {
            emit(DownloadState.Downloading(0, 0, 0))
            val destinationFileName = "${account.address}.csv"
            val doc = DocumentFile.fromTreeUri(App.appContext, destinationFolder)
            val file = doc?.createFile("csv", destinationFileName)
            try {
                byteStream().use { inputStream ->
                    App.appContext.contentResolver.openOutputStream(file!!.uri).use { outputStream ->
                        val totalBytes = contentLength()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream?.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt(), progressBytes, totalBytes))
                        }
                    }
                }
                emit(DownloadState.Finished)
            } catch (e: Exception) {
                emit(DownloadState.Failed(e))
            }
        }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
    }

    fun createRetrofitApi(baseUrl: String) {
        val loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
        api = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .readTimeout(30L, TimeUnit.SECONDS)
                    .writeTimeout(30L, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build()
            )
            .baseUrl(baseUrl)
            .build()
            .create(FileDownloadApi::class.java)
    }
}
