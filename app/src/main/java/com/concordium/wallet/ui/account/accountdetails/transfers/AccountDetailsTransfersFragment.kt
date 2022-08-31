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

    //region Lifecycle
    //************************************************************

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

    //endregion

    //region Initialize
    //************************************************************

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
                val filteredList = transferList.filter {
                        var result: Boolean = true
                        if(it.getItemType() == AdapterItem.ItemType.Item){
                            val item = it as TransactionItem
                            item.transaction?.let {

                                val transaction: Transaction = it

                                if (accountDetailsViewModel.isShielded) { // shielded balance
                                    /*
                                        local (unfinalized outgoing) transactions:
                                            simpleTransfer - NOT shown
                                            transferToSecret - NOT shown
                                    */
                                    if (!transaction.isRemoteTransaction()) {
                                        if (transaction.details != null) {
                                            if (transaction.details.type == TransactionType.TRANSFER || transaction.details.type == TransactionType.TRANSFERTOENCRYPTED) {
                                                result = false
                                            }
                                            else{}
                                        }
                                        else{}
                                    }
                                    /*
                                        remote transactions
                                            simpleTransfer - NOT shown
                                    */
                                    else {
                                        if (transaction.details != null) {
                                            if (transaction.details.type != TransactionType.TRANSFERTOENCRYPTED &&
                                                transaction.details.type != TransactionType.TRANSFERTOPUBLIC &&
                                                transaction.details.type != TransactionType.ENCRYPTEDAMOUNTTRANSFER &&
                                                transaction.details.type != TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO) {
                                                result = false
                                            }
                                            else{}
                                        }
                                        else{}
                                    }
                                } else {  // unshielded balance
                                    if (transaction.isRemoteTransaction()) {
                                        if (transaction.origin != null && transaction.details != null) {
                                            if (transaction.origin.type != TransactionOriginType.Self && (transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || transaction.details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO)) {
                                                result = false
                                            }
                                            else{
                                            }
                                        }
                                        else{}
                                    }
                                    else{
                                        result = true
                                    }
                                }

                            }
                        }
                        result
                }

                transactionAdapter.setData(filteredList)

                transactionAdapter.removeFooter()
                if (accountDetailsViewModel.hasMoreRemoteTransactionsToLoad) {
                    transactionAdapter.addFooter()
                }
                transactionAdapter.notifyDataSetChanged()
                if (transferList.isEmpty()) {
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

        /* Because of pinned headers, we will not use divider item decorations - reasons:
            1) The pinned header will not have a divider at the bottom
            2) The extra pixels between items is not handled in the pinned headers, so there will be a gap between pinned header and actual header
        */

        // Divider
        /*
        val dividerItemDecoration =
            DividerItemDecoration(view.recyclerview.context, linearLayoutManager.orientation)
        val listDividerDrawable = ContextCompat.getDrawable(context!!, R.drawable.list_divider)
        listDividerDrawable?.let {
            dividerItemDecoration.setDrawable(listDividerDrawable)
        }
        view.recyclerview.addItemDecoration(dividerItemDecoration)
        */

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

        // Scroll
        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (accountDetailsViewModel.allowScrollToLoadMore) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        accountDetailsViewModel.loadMoreRemoteTransactions()
                    }
                }
            }
        })
    }

    //endregion

    //region Control/UI
    //************************************************************

    //endregion
}
