package com.concordium.wallet.ui.account.accountsoverview

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.AppSettings
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.util.CryptoX
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountsOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private var _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private var _identityLiveData = MutableLiveData<State>()
    val identityLiveData: LiveData<State>
        get() = _identityLiveData

    private var _totalBalanceLiveData = MutableLiveData<TotalBalancesData>()
    val totalBalanceLiveData: LiveData<TotalBalancesData>
        get() = _totalBalanceLiveData

    private val _pendingIdentityForWarningLiveData = MutableLiveData<Identity?>()
    val pendingIdentityForWarningLiveData: MutableLiveData<Identity?>
        get() = _pendingIdentityForWarningLiveData

    private val _poolStatusesLiveData = MutableLiveData<List<Pair<String, String>>>()
    val poolStatusesLiveData: LiveData<List<Pair<String, String>>>
        get() = _poolStatusesLiveData

    private val _showDialogLiveData = MutableLiveData<Event<out DialogToShow>>()
    val showDialogLiveData: LiveData<Event<out DialogToShow>>
        get() = _showDialogLiveData

    val localTransfersLoaded: MutableLiveData<Account> by lazy { MutableLiveData<Account>() }

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val proxyRepository = ProxyRepository()
    private var isMigrationNoticeShown = false
    var hasPendingDelegationTransactions: Boolean = false
    var hasPendingBakingTransactions: Boolean = false

    val accountListLiveData: LiveData<List<AccountWithIdentity>>
    val identityListLiveData: LiveData<List<Identity>>

    enum class State {
        NO_IDENTITIES, NO_ACCOUNTS, DEFAULT, VALID_IDENTITIES
    }

    sealed interface DialogToShow {
        class Shielding(
            val cryptoXUrl: String,
        ) : DialogToShow

        class Sunsetting(
            val cryptoXUrl: String,
            val isForced: Boolean,
        ) : DialogToShow
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        accountListLiveData = accountRepository.allAccountsWithIdentity
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _waitingLiveData.value = false
                _totalBalanceLiveData.value = totalBalances
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
                    _newFinalizedAccountLiveData.value = accountName
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.value = Event(stringRes)
            }
        })
        identityListLiveData = identityRepository.allIdentities
    }

    fun loadPoolStatuses(poolIds: List<String>) {
        val poolStatuses: MutableList<Pair<String, String>> = mutableListOf()
        viewModelScope.launch {
            poolIds.forEach { poolId ->
                proxyRepository.getBakerPool(poolId,
                    { bakerPoolStatus ->
                        poolStatuses.add(
                            Pair(
                                poolId,
                                bakerPoolStatus.bakerStakePendingChange.pendingChangeType
                            )
                        )
                        if (poolStatuses.count() == poolIds.count())
                            _poolStatusesLiveData.value = poolStatuses
                    }, {
                        poolStatuses.add(Pair(poolId, ""))
                        if (poolStatuses.count() == poolIds.count())
                            _poolStatusesLiveData.value = poolStatuses
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }

    fun updateState(notifyWaitingLiveData: Boolean = true) {
        // Decide what state to show (visible buttons based on if there is any identities and accounts)
        // Also update all accounts (and set the overall balance) if any exists.
        viewModelScope.launch {

            val doneIdentityCount = identityRepository.getAllDone().count()
            if (doneIdentityCount > 0) {
                _identityLiveData.value = State.VALID_IDENTITIES
            } else {
                _identityLiveData.value = State.NO_IDENTITIES
            }

            val identitiesPending = identityRepository.getAllPending()
            if (!identitiesPending.isNullOrEmpty()) {
                _pendingIdentityForWarningLiveData.value = identitiesPending.first()
            } else {
                _pendingIdentityForWarningLiveData.value = null
            }

            val identityCount = identityRepository.getNonFailedCount()
            if (identityCount == 0) {
                _stateLiveData.value = State.NO_IDENTITIES
                // Set balance, because we know it will be 0
                _totalBalanceLiveData.value =
                    TotalBalancesData(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, false)
                if (notifyWaitingLiveData) {
                    _waitingLiveData.value = false
                }
            } else {
                val accountCount = accountRepository.getCount()
                if (accountCount == 0) {
                    _stateLiveData.value = State.NO_ACCOUNTS
                    // Set balance, because we know it will be 0
                    _totalBalanceLiveData.value =
                        TotalBalancesData(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, false)
                    if (notifyWaitingLiveData) {
                        _waitingLiveData.value = false
                    }
                } else {
                    _stateLiveData.value = State.DEFAULT
                    if (notifyWaitingLiveData) {
                        _waitingLiveData.value = false
                    }
                    updateSubmissionStatesAndBalances(notifyWaitingLiveData)
                    showMigrationNoticeOnceIfNeeded()
                }
            }
        }
    }

    fun loadLocalTransfers(account: Account) {
        viewModelScope.launch {
            hasPendingDelegationTransactions = false
            hasPendingBakingTransactions = false
            val transferDao = WalletDatabase.getDatabase(getApplication()).transferDao()
            val transferRepository = TransferRepository(transferDao)
            val transferList = transferRepository.getAllByAccountId(account.id)
            for (transfer in transferList) {
                if (transfer.transactionType == TransactionType.LOCAL_DELEGATION) hasPendingDelegationTransactions =
                    true
                if (transfer.transactionType == TransactionType.LOCAL_BAKER) hasPendingBakingTransactions =
                    true
            }
            localTransfersLoaded.value = account
        }
    }

    private fun updateSubmissionStatesAndBalances(notifyWaitingLiveData: Boolean = true) {
        if (notifyWaitingLiveData) {
            _waitingLiveData.value = true
        }
        accountUpdater.updateForAllAccounts()
    }

    fun initiateFrequentUpdater() {
        updater.cancel()
        updater.start()
    }

    fun stopFrequentUpdater() {
        updater.cancel()
    }

    var updater =
        object : CountDownTimer(Long.MAX_VALUE, BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC * 1000) {
            private var first = true
            override fun onTick(millisUntilFinished: Long) {
                if (first) { //ignore first tick
                    first = false
                    return
                }
                if (isRegularUpdateNeeded()) {
                    updateState(false)
                    // Log.d("Tick.....")
                }
            }

            override fun onFinish() {

            }
        }

    private fun isRegularUpdateNeeded(): Boolean {
        this.accountRepository.allAccountsWithIdentity.value?.forEach { accountWithIdentity ->
            if (accountWithIdentity.account.transactionStatus != TransactionStatus.FINALIZED) {
                return true
            }
        }
        return false
    }

    private fun showMigrationNoticeOnceIfNeeded() = viewModelScope.launch {
        if (isMigrationNoticeShown) {
            return@launch
        }

        val appSettings = runCatching {
            proxyRepository.getAppSettings(App.appCore.getAppVersion())
        }.getOrNull()

        when (appSettings?.status) {
            null,
            AppSettings.APP_VERSION_STATUS_OK,
            AppSettings.APP_VERSION_STATUS_WARNING -> {
                if (!App.appCore.session.isSunsettingNoticeShown()) {
                    _showDialogLiveData.postValue(
                        Event(
                            DialogToShow.Sunsetting(
                                cryptoXUrl = appSettings?.url ?: CryptoX.marketWebUrl,
                                isForced = false,
                            )
                        )
                    )
                } else if (!App.appCore.session.isShieldingNoticeShown()
                    && accountRepository.getAll().any(Account::mayNeedUnshielding)
                ) {
                    _showDialogLiveData.postValue(
                        Event(
                            DialogToShow.Shielding(
                                cryptoXUrl = appSettings?.url ?: CryptoX.marketWebUrl,
                            )
                        )
                    )
                }
            }

            AppSettings.APP_VERSION_STATUS_NEEDS_UPDATE -> {
                _showDialogLiveData.postValue(
                    Event(
                        DialogToShow.Sunsetting(
                            cryptoXUrl = appSettings.url ?: CryptoX.marketWebUrl,
                            isForced = true,
                        )
                    )
                )
            }
        }

        isMigrationNoticeShown = true
    }
}
