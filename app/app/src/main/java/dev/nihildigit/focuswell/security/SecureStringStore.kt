package dev.nihildigit.focuswell.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStringStore(
  context: Context,
  prefsName: String,
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

  fun getString(key: String, defaultValue: String? = null): String? {
    val stored = prefs.all[key] as? String ?: return defaultValue
    if (!stored.startsWith(VERSION_PREFIX)) {
      putString(key, stored)
      return stored
    }
    return decrypt(stored)
  }

  fun putString(key: String, value: String) {
    prefs.edit().putString(key, encrypt(value)).apply()
  }

  fun getLong(key: String, defaultValue: Long = 0L): Long =
    when (val stored = prefs.all[key]) {
      is Number -> {
        val value = stored.toLong()
        putLong(key, value)
        value
      }
      is String -> getString(key, null)?.toLongOrNull() ?: defaultValue
      else -> defaultValue
    }

  fun putLong(key: String, value: Long) {
    putString(key, value.toString())
  }

  fun remove(key: String) {
    prefs.edit().remove(key).apply()
  }

  fun clear() {
    prefs.edit().clear().apply()
  }

  private fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey())
    return listOf(
      VERSION,
      Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
      Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
    ).joinToString(":")
  }

  private fun decrypt(value: String): String? {
    val parts = value.split(":")
    if (parts.size != 3 || parts[0] != VERSION) return null
    return runCatching {
      val iv = Base64.decode(parts[1], Base64.NO_WRAP)
      val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
      cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrNull()
  }

  private fun secretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec =
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setUnlockedDeviceRequired(false)
          }
        }
        .build()
    generator.init(spec)
    return generator.generateKey()
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "focuswell-sensitive-prefs"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_BITS = 128
    const val VERSION = "v1"
    const val VERSION_PREFIX = "$VERSION:"
  }
}
