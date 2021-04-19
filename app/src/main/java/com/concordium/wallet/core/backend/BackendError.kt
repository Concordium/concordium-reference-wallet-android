package com.concordium.wallet.core.backend

import java.io.Serializable

class BackendError : Serializable {
    var error: Int = 0
        private set
    var errorMessage: String? = null
        private set

    constructor() {
        this.error = -1
        this.errorMessage = ""
    }

    constructor(code: Int, message: String) {
        this.error = code
        this.errorMessage = message
    }

    override fun toString(): String {
        return "[$error: $errorMessage]"
    }
}
