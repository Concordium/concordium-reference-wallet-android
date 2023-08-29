package com.concordium.wallet.core.backend

class BackendErrorException(val error: BackendError) : Exception("Response error") {
    override fun toString(): String {
        return "Response error: $error"
    }
}

class TransactionSimulationException() :
    Exception("Simulation of transfer failed, unable to proceed.")