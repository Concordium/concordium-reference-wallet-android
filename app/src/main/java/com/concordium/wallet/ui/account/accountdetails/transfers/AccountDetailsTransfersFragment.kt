package com.concordium.wallet.ui.account.accountdetails.transfers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.databinding.FragmentAccountDetailsTransfersBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.recyclerview.pinnedheader.PinnedHeaderItemDecoration

class AccountDetailsTransfersFragment : Fragment() {
    private var _binding: FragmentAccountDetailsTransfersBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountDetailsViewModel: AccountDetailsViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailsTransfersBinding.inflate(inflater, container, false)
        initializeViews()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        accountDetailsViewModel.populateTransferList()
    }

    private fun initializeViewModel() {
        accountDetailsViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountDetailsViewModel::class.java]

        accountDetailsViewModel.totalBalanceLiveData.observe(this, Observer {
            transactionAdapter.notifyDataSetChanged()
        })

        accountDetailsViewModel.transferListLiveData.observe(this, Observer { transferList ->
            transferList?.let {
                transactionAdapter.setIsShielded(accountDetailsViewModel.isShielded)
                
                val filteredList = transferList.filterIndexed { index, currentItem ->
                        var result = true
                        if (currentItem.getItemType() == AdapterItem.ItemType.Header)
                            result = showHeader(transferList, index)
                        else if (currentItem.getItemType() == AdapterItem.ItemType.Item)
                            result = showItem(currentItem)
                        result
                }

                if (filteredList.isNotEmpty()) {
                    transactionAdapter.setData(filteredList)
                    transactionAdapter.removeFooter()
                    if (accountDetailsViewModel.hasMoreRemoteTransactionsToLoad) {
                        transactionAdapter.addFooter()
                    }
                    transactionAdapter.notifyDataSetChanged()
                }

                if (filteredList.isEmpty()) {
                    binding.noTransfersTextview.visibility = View.VISIBLE
                } else {
                    binding.noTransfersTextview.visibility = View.GONE
                }

                accountDetailsViewModel.allowScrollToLoadMore = true
            }
        })

        accountDetailsViewModel.showGTUDropLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let { show ->
                if (show) {
                    binding.gtuDropLayout.visibility = View.VISIBLE
                    binding.gtuDropButton.isEnabled = true
                } else {
                    binding.gtuDropLayout.visibility = View.GONE
                }
            }
        })
    }

    private fun showHeader(transferList: List<AdapterItem>, currentIndex: Int): Boolean {
        var show = false
        var index = currentIndex
        do {
            index++
            var nextItem: AdapterItem? = null
            if (transferList.size > index)
                nextItem = transferList[index]
            if (nextItem != null && nextItem.getItemType() == AdapterItem.ItemType.Item)
                show = showItem(nextItem)
        } while (nextItem != null && nextItem.getItemType() == AdapterItem.ItemType.Item && !show)
        return show
    }

    private fun showItem(currentItem: AdapterItem): Boolean {
        var result = true
        val item = currentItem as TransactionItem
        item.transaction?.let {
            val transaction: Transaction = it
            if (accountDetailsViewModel.isShielded) {
                if (!transaction.isRemoteTransaction()) {
                    if (transaction.details != null) {
                        if (transaction.details.type == TransactionType.TRANSFER || transaction.details.type == TransactionType.TRANSFERTOENCRYPTED || transaction.details.type == TransactionType.UPDATE) {
                            result = false
                        }
                    }
                }
                else {
                    if (transaction.details != null) {
                        if (transaction.details.type != TransactionType.TRANSFERTOENCRYPTED &&
                            transaction.details.type != TransactionType.TRANSFERTOPUBLIC &&
                            transaction.details.type != TransactionType.ENCRYPTEDAMOUNTTRANSFER &&
                            transaction.details.type != TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO) {
                            result = false
                        }
                    }
                }
            } else {
                if (transaction.isRemoteTransaction()) {
                    if (transaction.origin != null && transaction.details != null) {
                        if (transaction.origin.type != TransactionOriginType.Self && (transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO)) {
                            result = false
                        }
                    }
                }
            }
        }
        return result
    }

    private fun initializeViews() {
        binding.noTransfersTextview.visibility = View.GONE
        binding.gtuDropLayout.visibility = View.GONE

        binding.gtuDropButton.setOnClickListener {
            binding.gtuDropButton.isEnabled = false
            accountDetailsViewModel.requestGTUDrop()
        }

        transactionAdapter = TransactionAdapter(requireContext(), accountDetailsViewModel.viewModelScope, AccountUpdater(requireActivity().application, accountDetailsViewModel.viewModelScope), mutableListOf())
        transactionAdapter.setOnDecryptListener(object : TransactionAdapter.OnDecryptClickListenerInterface {
            override fun onDecrypt(ta: Transaction) {
                accountDetailsViewModel.selectTransactionForDecryption(ta)

            }
        })

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerview.setHasFixedSize(true)
        binding.recyclerview.adapter = transactionAdapter
        binding.recyclerview.layoutManager = linearLayoutManager

        // Pinned Header
        val headerItemDecoration = PinnedHeaderItemDecoration(transactionAdapter)
        binding.recyclerview.addItemDecoration(headerItemDecoration)

        // Click
        transactionAdapter.setOnItemClickListener(object :
            TransactionAdapter.OnItemClickListener {
            override fun onItemClicked(item: Transaction) {
                val intent = Intent(activity, TransactionDetailsActivity::class.java)
                intent.putExtra(TransactionDetailsActivity.EXTRA_ACCOUNT, accountDetailsViewModel.account)
                intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION, item)
                intent.putExtra(TransactionDetailsActivity.EXTRA_ISSHIELDED, accountDetailsViewModel.isShielded)
                startActivity(intent)
            }
        })

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (accountDetailsViewModel.allowScrollToLoadMore) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                        accountDetailsViewModel.loadMoreRemoteTransactions()
                    }
                }
            }
        })
    }
}
