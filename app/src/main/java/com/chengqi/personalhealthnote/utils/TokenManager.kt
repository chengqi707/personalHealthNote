package com.chengqi.personalhealthnote.utils
import android.content.Context
import android.content.SharedPreferences
/**
 * Token管理工具类
 * 负责保存和读取登录信息、同步时间
 */
object TokenManager {
    private const val PREF_NAME = "user_pref"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    // 直接通过Context获取SharedPreferences，不需要初始化
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    /**
     * 是否登录
     */
    fun isLogin(context: Context): Boolean {
        return getToken(context).isNotEmpty()
    }
    /**
     * 获取Token
     */
    fun getToken(context: Context): String {
        return getSharedPreferences(context).getString(KEY_TOKEN, "") ?: ""
    }
    /**
     * 获取用户ID
     */
    fun getUserId(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_USER_ID, 0)
    }
    /**
     * 保存登录信息
     */
    fun saveLoginInfo(context: Context, token: String, userId: Long) {
        getSharedPreferences(context).edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .apply()
    }
    /**
     * 获取最后同步时间
     */
    fun getLastSyncTime(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_LAST_SYNC_TIME, 0)
    }
    /**
     * 更新最后同步时间
     */
    fun updateLastSyncTime(context: Context) {
        getSharedPreferences(context).edit()
            .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            .apply()
    }
    /**
     * 退出登录
     */
    fun logout(context: Context) {
        getSharedPreferences(context).edit()
            .clear()
            .apply()
    }
}