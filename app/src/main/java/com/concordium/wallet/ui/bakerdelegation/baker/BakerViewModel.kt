package com.concordium.wallet.ui.bakerdelegation.baker

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class BakerViewModel2(application: Application) : AndroidViewModel(application) {
/*
    lateinit var bakerData: BakerData
    // private val transferRepository: TransferRepository
    private val proxyRepository = ProxyRepository()

    companion object {
        const val FILE_NAME = "baker-credentials.json"
    }

    private val _bakerKeysLiveData = MutableLiveData<BakerKeys>()
    val bakerKeysLiveData: LiveData<BakerKeys>
        get() = _bakerKeysLiveData

    private val _fileSavedLiveData = MutableLiveData<Event<Int>>()
    val fileSavedLiveData: LiveData<Event<Int>>
        get() = _fileSavedLiveData

    init {
        // val transferDao = WalletDatabase.getDatabase(application).transferDao()
        // transferRepository = TransferRepository(transferDao)
    }

    fun initialize(bakerData: BakerData) {
        this.bakerData = bakerData
    }

    fun selectOpenBaker() {
        bakerData.isOpenBaker = true
        bakerData.isClosedBaker = false
    }

    fun selectClosedBaker() {
        bakerData.isOpenBaker = false
        bakerData.isClosedBaker = true
    }

    fun isOpenBaker(): Boolean {
        return bakerData.isOpenBaker // bakerData.account?.accountBaker?.bakerId == null
    }

    fun isClosedBaker(): Boolean {
        return bakerData.isClosedBaker // bakerData.account?.accountBaker?.bakerId != null
    }

    fun isUpdating(): Boolean {
        return false
    }

    fun markRestake(restake: Boolean) {
        this.bakerData.restake = restake
        // loadTransactionFee(true)
    }

    fun generateKeys() {
        viewModelScope.launch {
            val bakerKeys = App.appCore.cryptoLibrary.generateBakerKeys()
            if (bakerKeys == null) {
                _errorLiveData.value = Event(R.string.app_error_lib)
            } else {
                _bakerKeysLiveData.value = bakerKeys
            }
        }
    }

    fun saveFileToLocalFolder(destinationUri: Uri) {
        bakerKeysJson()?.let { bakerKeysJson ->
            viewModelScope.launch {
                FileUtil.writeFile(destinationUri, FILE_NAME, bakerKeysJson)
                _fileSavedLiveData.value = Event(R.string.export_backup_saved_local)
            }
        }
    }

    fun bakerKeysJson(): String? {
        _bakerKeysLiveData.value?.let { bakerKeys ->
            return if (bakerKeys.toString().isNotEmpty()) App.appCore.gson.toJson(bakerKeys) else null
        }
        return null
    }

    fun getTempFileWithPath(): Uri = Uri.parse("content://" + DataFileProvider.AUTHORITY + File.separator.toString() + FILE_NAME)

    fun loadTransactionFee(notifyObservers: Boolean) {

        val type = when(bakerData.type) {
            BakerData.TYPE_REGISTER_BAKER -> ProxyRepository.REGISTER_BAKER
            BakerData.TYPE_UPDATE_BAKER -> ProxyRepository.UPDATE_BAKER_STAKE
            BakerData.TYPE_REMOVE_BAKER -> ProxyRepository.REMOVE_BAKER
            else -> ProxyRepository.REGISTER_BAKER
        }

        val amount = when(bakerData.type) {
            DelegationData.TYPE_UPDATE_DELEGATION -> bakerData.amount
            else -> null
        }

        val restake = when(bakerData.type) {
            BakerData.TYPE_UPDATE_BAKER -> bakerData.restake
            else -> null
        }

        // val targetChange: Boolean? = if (poolHasChanged()) true else null
        val targetChange: Boolean? = null

        proxyRepository.getTransferCost(type,
            null,
            amount,
            restake,
            null,
            targetChange,
            {
                bakerData.energy = it.energy
                bakerData.cost = it.cost.toLong()
                if (notifyObservers)
                    _transactionFeeLiveData.value = bakerData.cost
            },
            {
                handleBackendError(it)
            }
        )
    }*/
}
