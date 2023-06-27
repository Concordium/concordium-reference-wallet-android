package com.concordium.wallet.util
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class HashUtilUnitTest {

    fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    @Test
    fun sha256Tests() {
        // Samples from https://www.dlitz.net/crypto/shad256-test-vectors/SHAd256_Test_Vectors.txt
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HashUtil.sha256(hexToBytes("616263")))
        assertEquals("4f8b42c22dd3729b519ba6f68d2da7cc5b2d606d05daed5ad5128cc03e6c6358", HashUtil.sha256(hexToBytes("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")))
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1", HashUtil.sha256(hexToBytes("6162636462636465636465666465666765666768666768696768696a68696a6b696a6b6c6a6b6c6d6b6c6d6e6c6d6e6f6d6e6f706e6f7071")))
        assertEquals("e668668fa102ccd4d3cf50e4472be8426e7dc52d6d355bee8c5e2177d1de81f7", HashUtil.sha256(hexToBytes("7941cb07924fdc7b710e11d98d82850e89566e1c3cb980517ffe4b430f86dfd5")))
        assertEquals("cacbf92792bf39aeea435f03bfd793779aee60949355081b19b51d5d32f4812e", HashUtil.sha256(hexToBytes("768f34d28be76a3ccf7da37007ab1b8227779b96bf141ecf2102f29b09258197")))
        assertEquals("d4fffb75f0bba678960a31c3ce52782939470e409b0b9b648303f1ebd1e17295", HashUtil.sha256(hexToBytes("aa8cbafe1ca69bcfdcc2fd345be49c4556a0dc8c6beddf72c9ac2e65df20a4f8")))
    }
}
