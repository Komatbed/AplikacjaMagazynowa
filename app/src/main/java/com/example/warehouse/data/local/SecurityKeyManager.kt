package com.example.warehouse.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.util.Base64
import java.security.SecureRandom

class SecurityKeyManager(context: Context) {

    private val sharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        EncryptedSharedPreferences.create(
            "warehouse_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Returns the encryption key for the database.
     * Generates a new random 256-bit key if one doesn't exist.
     */
    fun getDatabasePassphrase(): ByteArray {
        val storedKey = sharedPreferences.getString("db_key", null)
        if (storedKey != null) {
            return Base64.decode(storedKey, Base64.DEFAULT)
        }

        // Generate new key
        val random = SecureRandom()
        val keyBytes = ByteArray(32) // 256 bits
        random.nextBytes(keyBytes)
        
        val encodedKey = Base64.encodeToString(keyBytes, Base64.DEFAULT)
        sharedPreferences.edit().putString("db_key", encodedKey).apply()
        
        return keyBytes
    }
}
