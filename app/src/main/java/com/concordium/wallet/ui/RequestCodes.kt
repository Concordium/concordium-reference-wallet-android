package com.concordium.wallet.ui

class RequestCodes {
    companion object {
        // Permission request codes need to be < 256
        const val REQUEST_PERMISSION_CAMERA = 51

        const val REQUEST_CODE_CAMERA_PERMISSION_DIALOG = 301
        const val REQUEST_CODE_CAMERA_PERMISSION_NOT_AVAILABLE_DIALOG = 302

        const val REQUEST_IDENTITY_ERROR_DIALOG = 401

    }
}
