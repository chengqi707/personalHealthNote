package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.SharedPreferences

object AppLockManager {
    private const val PREF_NAME = "app_lock_pref"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_PIN_CODE = "pin_code"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).getString(KEY_PIN_CODE, null) != null
    }

    fun setPin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN_CODE, pin).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val saved = getPrefs(context).getString(KEY_PIN_CODE, null) ?: return false
        return saved == pin
    }

    fun clearPin(context: Context) {
        getPrefs(context).edit().remove(KEY_PIN_CODE).apply()
    }
}
