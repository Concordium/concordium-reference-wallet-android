// This is suppress the Camera deprecation warnings.
// Update to use of Camera2 could be considered, but event the ML Kit barcode demo still uses
// the old Camera.
// An update will also require major changes in CameraSource and CameraSourcePreviews.
@file:Suppress("DEPRECATION")

package com.concordium.wallet.ui.recipient.scanqr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.ui.RequestCodes
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_scan_qr.*
import java.io.IOException

class ScanQRActivity : BaseActivity(R.layout.activity_scan_qr, R.string.scan_qr_title),
    Dialogs.DialogFragmentListener {

    companion object {
        // intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001
        private const val INVALID_QR_STATE_TIMER = 500L
        const val EXTRA_BARCODE = "EXTRA_BARCODE"
    }

    private lateinit var viewModel: ScanQRViewModel

    private var mCameraSource: CameraSource? = null
    private val autoFocus = true
    private val useFlash = false

    private var latestInvalidBarcodeShownTime: Long = 0
    private var latestObservedInvalidBarcode: Long = 0
    private var hasFoundValidBarcode = false

    private val invalidQRStateHandler = Handler()

    /**
     * This runnable has the responsibility to deactivate the invalid QR state.
     * And to not do it if new invalid detections has been found after the first.
     * Thereby avoiding switching back and forth between states to fast.
     * (But the detections are not always received from the BarcodeDetector and therefore switches in states will happen).
     */
    private val invalidQRStateRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (now - latestInvalidBarcodeShownTime > 2000) {
                viewModel.setStateDefaultIfAllowed();
            }

            invalidQRStateHandler.postDelayed(this, INVALID_QR_STATE_TIMER);
        }
    }

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
        viewModel.initialize()
        initializeViews()

        // Check for the camera permission before accessing the camera.
        // If the permission is not granted yet, request permission.
        if (hasCameraPermission()) {
            createCameraSource(autoFocus, useFlash)
        } else {
            requestCameraPermission()
        }
        invalidQRStateHandler.postDelayed(
            invalidQRStateRunnable,
            INVALID_QR_STATE_TIMER.toLong()
        )
    }

    override fun onResume() {
        super.onResume()
        resetScanner()
    }

    override fun onPause() {
        super.onPause()
        camera_preview?.let {
            camera_preview.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camera_preview?.let {
            try {
                camera_preview.release()
            }
            catch (e: Exception){
                Log.e("Failed releasing camera: "+e)
            }
        }
        invalidQRStateHandler.removeCallbacks(invalidQRStateRunnable)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                onCameraPermissionRationaleDialogOkClicked()
            }
        }
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(ScanQRViewModel::class.java)

        viewModel.stateLiveData.observe(this, Observer { state ->
            onStateChanged(state)
        })
    }

    private fun initializeViews() {

    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onStateChanged(state: ScanQRViewModel.State) {
        when (state) {
            ScanQRViewModel.State.DEFAULT -> showDefaultUI()
            ScanQRViewModel.State.NOT_VALID_QR -> showNotValidQRUI()
        }
    }

    private fun showDefaultUI() {
        overlay_imageview.setImageResource(R.drawable.ic_qr_overlay)
    }

    private fun showNotValidQRUI() {
        overlay_imageview.setImageResource(R.drawable.ic_qr_overlay_bad)
    }

    private fun goBackWithBarcode(barcode: String) {
        val intent = Intent()
        intent.putExtra(EXTRA_BARCODE, barcode)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    //endregion

    //region Permission
    //************************************************************

    private fun hasCameraPermission(): Boolean {
        val permission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w("Camera permission is not granted. Requesting permission")
        // The rationale will be shown if the user rejects the permission a second time
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            )
        ) {
            dialogs.showOkDialog(
                (this as AppCompatActivity),
                RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_DIALOG,
                getString(R.string.scan_qr_permission_camera_rationale_title),
                getString(R.string.scan_qr_permission_camera_rationale)
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA), RequestCodes.REQUEST_PERMISSION_CAMERA
            )
        }
    }

    private fun onCameraPermissionRationaleDialogOkClicked() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            RequestCodes.REQUEST_PERMISSION_CAMERA
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == RequestCodes.REQUEST_PERMISSION_CAMERA) {
            if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Camera permission granted - initialize the camera source")
                // We have permission, so create the camerasource
                createCameraSource(autoFocus, useFlash)
                // Start CameraSource here (it was not ready in onResume)
                startCameraSource()
                return
            }
            Log.d(
                "Permission not granted: results len = " + grantResults.size +
                        " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)"
            )
            dialogs.showOkDialog(
                this, RequestCodes.REQUEST_CODE_CAMERA_PERMISSION_NOT_AVAILABLE_DIALOG,
                R.string.scan_qr_error_no_camera_permission_title,
                R.string.scan_qr_error_no_camera_permission
            )
        }
    }

    //endregion

    //region Camera/Scanning
    //************************************************************

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @SuppressWarnings("MissingPermission")// Camera permission is checked in sub function
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        mCameraSource?.let { cameraSource ->
            try {
                if (hasCameraPermission()) {
                    camera_preview.start(cameraSource)
                }
            } catch (e: IOException) {
                Log.e("Unable to start camera source.", e)
                cameraSource.release()
                mCameraSource = null
            }

        }
    }

    /**
     * Creates and starts the camera.
     *
     */
    private fun createCameraSource(autoFocus: Boolean, useFlash: Boolean) {
        // A barcode detector is created to track barcodes.
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {}
            override fun receiveDetections(detections: Detections<Barcode?>) {
                // This is not on the main thread
                val barcodes = detections.detectedItems
                for (i in 0 until barcodes.size()) {
                    val barcode = barcodes.valueAt(i)
                    if (barcode != null) {
                        Log.d("Barcode found " + System.currentTimeMillis())
                        if (barcodeFound(barcode)) {
                            return
                        }
                    }
                }
            }
        })
        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.

            //isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w("Detector dependencies are not yet available.")
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowstorageFilter) != null
            if (hasLowStorage) {
                Toast.makeText(this, R.string.scan_qr_error_low_storage, Toast.LENGTH_LONG).show()
                Log.w(getString(R.string.scan_qr_error_low_storage))
            }
        }
        // Creates and starts the camera.  Note that this uses a higher resolution to enable
        // the barcode detector to detect small barcodes at long distances.
        var builder =
            CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
        // Make sure that auto focus is an available option
        builder =
            builder.setFocusMode(if (autoFocus) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else null)
        mCameraSource = builder
            .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else null)
            .build()
    }

    /**
     * This is not called on the Main thread
     * @param barcode
     * @return
     */
    private fun barcodeFound(barcode: Barcode): Boolean {
        if (hasFoundValidBarcode) {
            return false
        }
        hasFoundValidBarcode = viewModel.checkQrCode(barcode.displayValue)
        if (hasFoundValidBarcode) {
            val runnable = Runnable {
                Log.d("Valid barCode found")
                camera_preview.stop()
                overlay_imageview.setImageResource(R.drawable.ic_qr_overlay_good)
                goBackWithBarcode(barcode.displayValue)
            }
            runOnUiThread(runnable)
            return true
        } else { // Avoid spamming by checking time
            val now = System.currentTimeMillis()
            latestObservedInvalidBarcode = now
            if (now - latestInvalidBarcodeShownTime > 2000) {
                val text = barcode.displayValue
                Log.d("Scanned code is not valid: $text")
                val runnable = Runnable {
                    viewModel.setStateQRNotValid()
                    popup.showSnackbar(root_layout, R.string.scan_qr_error_invalid_qr_code)
                }
                runOnUiThread(runnable)
                latestInvalidBarcodeShownTime = now
            }
        }
        return false
    }

    private fun resetScanner() {
        hasFoundValidBarcode = false
        startCameraSource()
        viewModel.setStateDefault()
    }

    //endregion
}