package com.concordium.wallet.ui.identity.identityproviderwebview

//import com.concordium.mobile_wallet_lib.id_object_response
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_identity_provider_webview.*
import kotlinx.android.synthetic.main.progress.*
import java.util.*


class IdentityProviderWebviewActivity : BaseActivity(
    R.layout.activity_identity_provider_webview,
    R.string.identity_provider_webview_title
) {

    private lateinit var viewModel: IdentityProviderWebviewViewModel

    companion object {
        const val EXTRA_IDENTITY_CREATION_DATA = "EXTRA_IDENTITY_CREATION_DATA"
        const val KEY_IDENTITY_CREATION_DATA = "KEY_IDENTITY_CREATION_DATA"

        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_DATAWEREAVAILABLE = 1 // data were available
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_VALIDCALLBACKURI = 2 // valid callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOIDENTITYDATA = 3 // identityCreationData was not null
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_MODELSINITIALISED = 4 // models initialised correctly
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_INCORRECTCALLBACKURI = 5 // incorrect callback URL
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_CODEURISET = 6 // code_uri is set in callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_TOKENSET = 7 // token is set in callback uri
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_ERRORSET = 8 // error is set (given other errors)
        const val IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOTHINGSET = 9 // nothing is set in the callback uri
    }

    // Have to keep this intent data in case the Activity is force killed while on the IdentityProvider website
    class IdentityDataPreferences(context: Context, preferenceName: String, preferenceMode: Int) :
        Preferences(context, preferenceName, preferenceMode) {
        fun getIdentityCreationData(): IdentityCreationData? {
            val json = getString(KEY_IDENTITY_CREATION_DATA)
            if(json == null) {
                return null
            }
            else {
                return Gson().fromJson(json, IdentityCreationData::class.java)
            }
        }
        fun setIdentityCreationData(data: IdentityCreationData?) {
            setString(KEY_IDENTITY_CREATION_DATA, if(data == null){ null } else { Gson().toJson(data) })
        }
    }


    private val preferences: IdentityDataPreferences
        get() {
            return IdentityDataPreferences(getApplication(),
                KEY_IDENTITY_CREATION_DATA, Context.MODE_PRIVATE)
        }



    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var handled = false

        var supportCode = BitSet(10) // room for 10 flags
        var identityCreationData = preferences.getIdentityCreationData()

        // In the case where the activity has been force closed, onCreate needs
        // to handle to receive the callbacl uri
        intent.data?.let { uri ->

            supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_DATAWEREAVAILABLE) // data were available

            if (hasValidCallbackUri(uri)){
                supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_VALIDCALLBACKURI) // valid callback
                if(identityCreationData != null) {
                    supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOIDENTITYDATA) // identityCreationData was not null
                    handled = true
                    initializeViewModel()
                    viewModel.initialize(identityCreationData)
                    // Clear the temp data, now that we have set in on the view model
                    preferences.setIdentityCreationData(null)
                    initViews()
                    supportCode.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_MODELSINITIALISED) // models initialised correctly
                    handleNewIntentData(uri, supportCode)
                }
            }
        }
        if (!handled) {
            // Initial case, where we come from the previous page
            val tempData =
                intent.extras!!.getSerializable(EXTRA_IDENTITY_CREATION_DATA) as IdentityCreationData?
            if (tempData != null) {
                preferences.setIdentityCreationData(tempData)
                handled = true
                initializeViewModel()
                viewModel.initialize(tempData)
                initViews()
                if (!viewModel.useTemporaryBackend) {
                    showChromeCustomTab(viewModel.getIdentityProviderUrl())
                }
            }
        }


        if (!handled) {
            Log.e("No IdentityCreationData saved - this should not happen")

            val emailFlow = IdentityErrorDialogHelper.canOpenSupportEmail(this)
            var dialogMsgAction = R.string.dialog_support
            var dialogMsg = R.string.dialog_popup_support_creation_flow_with_email_client_text

            var errorText = getString(R.string.dialog_support_text, supportCode.toLongArray()[0].toString(), BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)

            if(!emailFlow){
                dialogMsgAction = R.string.dialog_copy
                dialogMsg = R.string.dialog_popup_support_creation_flow_without_email_client_text
            }
            AlertDialog.Builder(this)
                .setMessage(dialogMsg)
                .setPositiveButton(dialogMsgAction) { dialog, which ->
                    if(emailFlow){
                        IdentityErrorDialogHelper.openGenericSupportEmail(this, resources,getString(R.string.dialog_initial_account_error_title), errorText)
                    }
                    else{
                        IdentityErrorDialogHelper.copyToClipboard(this, getString(R.string.contact_issuance_hash_value_copied), "")
                    }
                    finish()
                }
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, which ->
                    finish()
                }
                .show()

            //showError(R.string.dialog_support_creation_data_reference)
            //finish()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Clear the temp data, we already have it in the view model
        // preferences.setIdentityCreationData(null)
        Log.d("New intent")
        intent?.data?.let {
            handleNewIntentData(it, null)
        }
    }



    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(IdentityProviderWebviewViewModel::class.java)

        viewModel.waitingLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        })

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })

        viewModel.identityCreationError.observe(this, object : EventObserver<String>() {
            override fun onUnhandledEvent(value: String) {
                val error = BackendError(0, value)
                gotoFailed(error)
            }
        })

        viewModel.identityCreationUserCancel.observe(this, object : EventObserver<String>() {
            override fun onUnhandledEvent(value: String) {
                onBackPressed()
            }
        })

        viewModel.gotoIdentityConfirmedLiveData.observe(this, object : EventObserver<Identity>() {
            override fun onUnhandledEvent(value: Identity) {
                gotoIdentityConfirmed(value)
            }
        })
        viewModel.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
    }

    fun initViews() {
        val title =
            viewModel.identityCreationData.identityProvider.ipInfo.ipDescription.name + " " +
                    getString(R.string.identity_provider_webview_title)
        setActionBarTitle(title)
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun gotoIdentityConfirmed(identity: Identity) {
        finish()
        val intent = Intent(this, IdentityConfirmedActivity::class.java)
        intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
        startActivity(intent)
    }

    private fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Identity)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(root_layout, stringRes)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            progress_layout.visibility = View.VISIBLE
        } else {
            progress_layout.visibility = View.GONE
        }
    }

    private fun hasValidCallbackUri(uri: Uri): Boolean {
        if (uri.toString().startsWith(IdentityProviderWebviewViewModel.CALLBACK_URL)) {
            return true
        }
        return false
    }

    private fun handleNewIntentData(uri: Uri, supportCode: BitSet?): Boolean {
        if (uri.toString().startsWith(IdentityProviderWebviewViewModel.CALLBACK_URL)) {
            // The fragment is the part after # and is specified by the issuer to be one
            // key/value pair: token=[identityObject]
            val fragment = uri.fragment
            val fragmentParts = fragment?.split("=")

            /*
            concordiumwallet-staging://identity-issuer/callback#code_uri=https://idiss.notabene.id/idiss/token?code={"check_id":"JTdCJTIyZmlyc3ROYW1lJTIyJTNBJTIySm9obiUyMiUyQyUyMmxhc3ROYW1lJTIyJTNBJTIyRG9lJTIyJTJDJTIyc2V4JTIyJTNBJTIyMCUyMiUyQyUyMmRvYiUyMiUzQSUyMjE5MDAwMTAxJTIyJTJDJTIyY291bnRyeU9mUmVzaWRlbmNlJTIyJTNBJTIyREslMjIlMkMlMjJuYXRpb25hbGl0eSUyMiUzQSUyMkRLJTIyJTJDJTIyaWREb2NUeXBlJTIyJTNBJTIyMSUyMiUyQyUyMmlkRG9jTm8lMjIlM0ElMjIxMjM0NSUyMiUyQyUyMmlkRG9jSXNzdWVyJTIyJTNBJTIyREslMjIlMkMlMjJpZERvY0lzc3VlZEF0JTIyJTNBJTIyMjAyMDAxMDElMjIlMkMlMjJpZERvY0V4cGlyZXNBdCUyMiUzQSUyMjIwMjAxMjMxJTIyJTJDJTIybmF0aW9uYWxJZE5vJTIyJTNBJTIyTi0xMjM0JTIyJTJDJTIydGF4SWRObyUyMiUzQSUyMlQtMTIzNCUyMiU3RA==","idv":"dev"}&state={"identityObjectRequest":{"idObjectRequest":{"choiceArData":{"arIdentities":[0,1,2],"threshold":2},"idCredPub":"b2944e9718fe4e27bde69cdd94782057b93ed6d98b93b04a87a9c1655e0113134e25443dbb5ba128a47dc7528259912d","idCredSecCommitment":"8a1114a993fb920b3243e6915298abafe612da9dcde814c13dc4ca96f09f104f5fa09ca1ed234293614c65148b6e911e","ipArData":[{"arIdentity":0,"encPrfKeyShare":"a5233f2b6255621642046c79923d348b1402ac0650ab98ef1c7f90b3bb4e92b9e298a8314b03a9790ca675d320ec5d16b28b32a32831373e082fddadeebbc0620801cb67c5791c7f4c84414ebf90ddd178cc9897c41aca5eba75653111ea2b6b","prfKeyShareNumber":1,"proofComEncEq":"2fe004ddde630749b6136f4675a616adeb80b0a48c9fe07fce22b67ae3e954a2634a86bab7ed15749e770a45edf21aa479987696cd74e0aae0d84cb37f64d34f1f47a5a8f4d35a4ecd92c485bb943ae0ae260379e1c3190c53c99b6e04d28a3145ceb53f5b3b38266acc1fabd577b7487791ae6225e944b07acc99e02da39549"},{"arIdentity":1,"encPrfKeyShare":"93a7012c3a184181628ffb83a830fa75d080a7ae23f59d609be3327a5a04ae3c9109deadacde0bb5b90801c95f86593992e8db5c0a12f0e76fa5043dc37d8ad54f2314d6d109cadb51e96a0c39b7113a387db4eb59db4f59e70d3e5416619158","prfKeyShareNumber":2,"proofComEncEq":"033260e87e04c35c58d40b6ca1b1e26b34bdb2cc3c5a40db5beac7c6c09995d839de6d911ffb9974fa7ef71b6d4973328585a1c38b826d8e666f2f0215a820a112ebbe76d5ebf4c7b67d00ed048cbb4d9836abbdb257e2f91c78030e98b5192b506db6caafe63c21997e692dcbbed57bbd5c7bb06117a1d98d7c748c4e1a44ae"},{"arIdentity":2,"encPrfKeyShare":"97f30e411f3624bea406556a1dc1beba5e73ee8ce543421e5a177afd0b11df018fa40c805acfc5145820b1a12b8f3f17a5e65551a89808e6d924eff4044739cd65d230e16de5f46120caa0091b1d4c24386eef89b1a7db049417e79f0548ac86","prfKeyShareNumber":3,"proofComEncEq":"30b3f0bf21cf4ef76f9d26ee00c5e6407d861a37eef65316482e66bdc75f4e3f0088c5a0cecf818df53f0c1b0cd05c2aedba31f3378b88fe3101fc6af7f511884d1908721d770e3f1a4ea6f22d3255356bd85024e65bc887921b8226d2dd209e28ad47bc0cfc889530ff718f7a99ba6b2e23bf5e3a62384e99f80feca9b0ce82"}],"pokSecCred":"3ebcb11316c1c5c202779f8eab4a9a95212c6f39eba3ee397fe151fb72502cbd5fa5cdba3a995b8dd0db7c0a8f19d48dc2414ac6fe2b156f39909286132b9b0c","prfKeyCommitmentWithIP":"988abd5557d85afe7e6d9870a24653b447d6dfac4be1099fd2fb1614f1fc80afecb4f6a59577f685841263999ab91175","prfKeySharingCoeffCommitments":["a797a6c014e27667b25363af5c40a1c78eb08ec63d2debba3c930b47d1d0b21bc24adc23f069d3e9e1f32341c64cdaf3","a0d721fe09c5b0dad39e61d0578c03ce5103801175c2231b9c556a2f3b0195d34b6032490cdd24edb4ffd30d16660927"],"proofCommitmentsSame":"6e9b7bff60a7ac56e7018ed2e878733515f4c288035c1f6f479179e029b5f31d68d9670d7b4c64b1c79e2fce856cb4a3d22d939df17c3cb57e3c093b538fcde170bab6b21bbcf5acc4c9cae5c22fc1ae582ce67aeccb0e0e6963e4dc0444912e32d296e495ab8d0cdc25976049d3526623426babf532779bf4fd93cbf1b19a6f","proofCommitmentsToIdCredSecSame":"20824ba0cdfd3fa2e66607389834638d56e72af6a2da700873092adc58f18faf0000000122353c8e213201280fd33945a669e5e5cf40815436b93f1b9c17320dc606ed913a55283492d2e451c67780755531a6771edbb6bd97c209b8d8cb3c634ed88197"}},"redirect_uri":"concordiumwallet-staging://identity-issuer/callback"}
            */

            /*
            concordiumwallet-staging://identity-issuer/callback#token={"identityObject":{"preIdentityObject":{"idCredPub":"b7c3e9d5b2783a8632e10bbed78b434e8b1ee58392201c1220d5b2eb6eac3c8d107bff44cc75c86880a0971a5818ab3e","ipArData":[{"arIdentity":0,"encPrfKeyShare":"b8a71321f8f72cea933d3534290c2528c92a1fccedce99dab91d4c5fa8d27a37e73a07be51d78645a2c2232aef1147f6b847232450bca54c161300c58f544f585ac91c08d05749fbb000b10cc0ef3a605bba6bc77862f067d786859bc8674326","prfKeyShareNumber":1,"proofComEncEq":"0246f8d49188292a3d986045b875906fdc5b10e66a28f411a7d0212ea1a17162068c4f85f315c05069c1ab7152cde497e373780cd9a38ed3292b5f348405322e6d66387aa41696e42d0802a7c67b2e7a507323d5c8bebbbd27ef4df53a2a5e62312431327234d5cc1f661bd0ab03186fdb30870b2707f90a1de8cbea4ed2f2a1"},{"arIdentity":1,"encPrfKeyShare":"8dd16dc491a5425e9551799202aeb5cef55d6b8fb9b39b0e1031d2a656fea8825415954f826909706c7a5f2a3e11b09b85c3c9daefe8364be40636974412a6c50c22fe8c7c4d6443b84c367c41f477badc7eb352758fbe5768eb2928bc255213","prfKeyShareNumber":2,"proofComEncEq":"58a2ff07ef247e6f28c6cdaca66e662e6a8f5d2014c6000cc97d395017d0282556c8af820e370dd8675535749fc662d24e516d7e4e647fbb9280405bbf5802260fce9bc51bea2ed9a2f8b6714130eeca05b5ef2127f1ac9c42b7ff5d211893eb2d234f52d77f74dad0cbea29bfaf7006653ee18ddb35f629ff2d5fc1319202f1"},{"arIdentity":2,"encPrfKeyShare":"a3918b4660805a003780dbfaae1ef537066508982befb635b9672242ea0b7801598d37c9e0f2e347c2ab5b80091ae2ef86acc28c4ad7a33f6894ca3d20ad9730204f00904e1ef7f2ae3989e031c32863fd23b61d7160d024884c6e23cd528147","prfKeyShareNumber":3,"proofComEncEq":"668cb263af8b42b3540a466dd8775f36825e09e9b672aa3713af7c46bc3f65eb5c573a41c4becaf722ab63ffe7e909b39a82116ef51a7ce3a3d3acb0cf9a3cb96a9939fee74ada7982b2666b74547c18d129d72ecbc64484bddd209cdb7921fb6d2bd750db06a09de68eb130503e3f7201e9ebed45921742e4004ec27eb1131b"}],"choiceArData":{"arIdentities":[0,1,2],"threshold":2},"pokSecCred":"326891e4964e6eae1c51d1daed12df11f547c61765f2cb7d0fa75ba6bcc5dad01c5cbf2dc8c2494079e495e6b6e1e59fece941d6bef155387c6881cdf220d09f","idCredSecCommitment":"abcba07733a0c28305280b61b97feb894918e150d2357dd40ad99bf3287a95dbec04f675ed49dcd8bf45c2229f76b5b0","proofCommitmentsToIdCredSecSame":"3833ad1f7c6c67e6bdc087408dffd482afd3a01eff50530dea791c6763f1a505000000015f75c398f6fe7a6b4674c6370585b83d0787ca5452c123a470f689533fd6a9b7563dbc15e280da73100d5bfb204cfa450d2475e0a0e4fd2a8f685dae46d8f8ae","prfKeyCommitmentWithIP":"8570a5fdfef2f60772e7e791d49aae9b7dc1936f2df99836134651e94e94823a97654ab04ae2da9db00a6655b82d6605","prfKeySharingCoeffCommitments":["8a69b8d451f0e6ec9be55badc64564e417d3cab7502dbba259e4139259828fd8c877d954893738bec805a3b7417f5140","987488bd8a7aacf9055f7e0aa1e31604b4c39b17ebb22f8ada6a4ed5922602285c8d229e3a7470c60db14c185be9a953"],"proofCommitmentsSame":"2c862a7cc838f51d7a5fab2ac66f8d02807c64fb0c760a720e0d11c2de34a84e06e77475643e32aadc6b407e761a9df0e0acb52ed12b69cb11c9ec0b3c4bf3045b60285cc225cf5cf747a283530728167dc1d97468cab32587876e70df20079e12648f5ebf70cc6d4521db3d1271c34ed48ee60e048d1ce27dc8d54718c06c94"},"attributeList":{"validTo":"202105","createdAt":"202005","maxAccounts":238,"chosenAttributes":{"firstName":"John","lastName":"Doe","sex":"0","dob":"19000101","countryOfResidence":"DK","nationality":"DK","idDocType":"1","idDocNo":"12345","idDocIssuer":"DK","idDocIssuedAt":"20200101","idDocExpiresAt":"20201231","nationalIdNo":"N-1234","taxIdNo":"T-1234"}},"signature":"b6203f6e14e5647c8f68e2ae9a2634269c9f7ae630ca9347e615b8e578bc412074f2fae7005b3b483a49f5a90aed84e9a3d8fa66b0904cbc6817883656b61e4b093424ac09080be4ec7ad6c4a0e210c675e2196b06965a433991279a282b8623"}}
            */

            if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "code_uri") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_CODEURISET) // code_uri is set
                viewModel.parseIdentityAndSavePending(uri.toString().split("#code_uri=").last())
            }
            else
            if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "token") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_TOKENSET) // token is set
                val identity = fragmentParts[1]
                viewModel.parseIdentityAndSave(identity)
            }
            else
            if (!fragmentParts.isNullOrEmpty() && fragmentParts[0] == "error") {
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_ERRORSET) // error is set
                viewModel.parseIdentityError(fragmentParts[1]);
            }
            else{
                supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_NOTHINGSET) // nothing is set
            }
            return true
        }
        supportCode?.set(IDENTITY_CALLBACK_ERROR_BIT_INDEX_INCORRECTCALLBACKURI) // incorrect callback URL
        return false
    }

    //endregion

    //region (Chrome) Custom Tab
    //************************************************************

    private fun showChromeCustomTab(url: String) {

        //if(BuildConfig.USE_LIBRARY_PROVIDED_IDENTITIES){
            //val uri = Uri.parse(url)
            //val state = uri.getQueryParameter("state")
            //val res = state?.let { id_object_response(it) }
            //handleNewIntentData(Uri.parse(IdentityProviderWebviewViewModel.CALLBACK_URL+"#token="+ res!!.output))
            //return
        //}


        // Try to use Chrome browser to show url
        // If Chrome is not installed an ActivityNotFoundException will be thrown
        try {
            launchChromeCustomTab(url, true)
            return
        } catch (e: ActivityNotFoundException) {
        }
        // If not Chrome let the default browser with Custom Tabs handle this
        try {
            launchChromeCustomTab(url)
            return
        } catch (e: ActivityNotFoundException) {
            showError(R.string.app_error_general)
        }
    }

    private fun launchChromeCustomTab(url: String, forceChromeBrowser: Boolean = false) {
        val customTabBuilder = CustomTabsIntent.Builder()
        customTabBuilder.setToolbarColor(getColor(R.color.theme_white))
        val customTabsIntent = customTabBuilder.build()
        if (forceChromeBrowser) {
            customTabsIntent.intent.setPackage("com.android.chrome")
        }
        customTabsIntent.launchUrl(this, Uri.parse(url))
        finish()
    }

    //endregion

}

