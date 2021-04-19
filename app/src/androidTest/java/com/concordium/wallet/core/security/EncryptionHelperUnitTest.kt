package com.concordium.wallet.core.security

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionHelperUnitTest {
    @Test
    fun encryption() {
        val password = "123"
        val text = "some_text"

        val (salt, iv) = EncryptionHelper.createEncryptionData()
        val decodedEncrypted = EncryptionHelper.encrypt(password, salt, iv, text)

        val toBeDecryptedByteArray = Base64.decode(decodedEncrypted, Base64.DEFAULT)
        val decrypted = EncryptionHelper.decrypt(password, salt, iv, toBeDecryptedByteArray)

        // decrypting the encrypted gives the original
        assertEquals(text, decrypted)

        // Same result every time
        val decodedEncrypted2 = EncryptionHelper.encrypt(password, salt, iv, text)
        assertEquals(decodedEncrypted, decodedEncrypted2)
    }
}
