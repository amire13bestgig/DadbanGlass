package ir.dadban.glass.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ir.dadban.glass.model.UserDto
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * مدیریت احراز هویت دوگانه (Dual-Auth) با رمزنگاری سخت‌افزاری Android Keystore AES-GCM:
 * پشتیبانی از JWT Bearer Token (ورود با OTP موبایل) و X-API-Key (کلید توسعه‌دهنده/ادمین).
 */
class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("dadban_secure_auth", Context.MODE_PRIVATE)
    private val alias = "dadban_master_key_aes"
    private val androidKeyStore = "AndroidKeyStore"

    fun getJwtToken(): String = decryptField("jwt_token")
    fun saveJwtToken(token: String) = encryptField("jwt_token", token)

    fun getApiKey(): String = decryptField("api_key")
    fun saveApiKey(key: String) = encryptField("api_key", key)

    fun hasValidAuth(): Boolean = getJwtToken().isNotBlank() || getApiKey().isNotBlank()

    /**
     * هدر احراز هویت را برمی‌گرداند (اولویت با JWT Bearer و سپس X-API-Key).
     */
    fun getAuthHeader(): Pair<String, String>? {
        val jwt = getJwtToken()
        if (jwt.isNotBlank()) {
            return "Authorization" to "Bearer $jwt"
        }
        val api = getApiKey()
        if (api.isNotBlank()) {
            return "X-API-Key" to api
        }
        return null
    }

    fun saveUser(user: UserDto) {
        val obj = JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("mobile", user.mobile)
            put("email", user.email)
            put("national_code", user.nationalCode)
            put("has_active_subscription", user.hasActiveSubscription)
            put("subscription_plan", user.subscriptionPlan)
        }
        prefs.edit().putString("cached_user", obj.toString()).apply()
    }

    fun getCachedUser(): UserDto? {
        val raw = prefs.getString("cached_user", null) ?: return null
        return try {
            UserDto.fromJson(JSONObject(raw))
        } catch (_: Exception) { null }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    private fun encryptField(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove("${key}_iv").remove("${key}_val").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("${key}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${key}_val", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    private fun decryptField(key: String): String {
        return try {
            val iv = prefs.getString("${key}_iv", null) ?: return ""
            val ciphertext = prefs.getString("${key}_val", null) ?: return ""
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
