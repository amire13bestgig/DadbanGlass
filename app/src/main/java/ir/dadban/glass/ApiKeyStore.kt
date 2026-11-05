package ir.dadban.glass

import android.content.Context
import ir.dadban.glass.security.AuthStore

/**
 * شیم سازگاری رو به عقب برای ApiKeyStore که به AuthStore متصل است.
 */
class ApiKeyStore(context: Context) {
    private val authStore = AuthStore(context)

    fun get(): String = authStore.getApiKey()

    fun save(value: String) = authStore.saveApiKey(value)

    fun clear() = authStore.saveApiKey("")
}
