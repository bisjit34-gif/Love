package com.riptika.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Notes stored on-device, encrypted with an AES-GCM key held in Android Keystore. */
class SecureMemory(ctx: Context) {
    private val file = File(ctx.filesDir, "memory.bin")
    private val alias = "riptika_mem_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(alias)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }.generateKey()
        }
        return ks.getKey(alias, null) as SecretKey
    }

    @Synchronized
    fun all(): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readBytes()
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
            val arr = JSONArray(String(c.doFinal(raw, 12, raw.size - 12)))
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    @Synchronized
    fun add(note: String) {
        val list = all() + note
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val enc = c.doFinal(JSONArray(list).toString().toByteArray())
        file.writeBytes(c.iv + enc)
    }

    fun search(q: String): List<String> = all().filter { it.contains(q, ignoreCase = true) }
}
