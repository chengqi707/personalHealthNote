package com.chengqi.personalhealthnote.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.BuildConfig
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.databinding.ActivitySettingsBinding
import com.chengqi.personalhealthnote.utils.DialogUtils
import com.chengqi.personalhealthnote.utils.TokenManager
import com.chengqi.personalhealthnote.utils.AppLockManager
import com.chengqi.personalhealthnote.utils.ToastUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"

        setupAccountSection()
        setupSecuritySection()
        setupDataSection()
        setupAboutSection()
    }

    private fun setupAccountSection() {
        updateAccountUI()

        binding.itemAccount.setOnClickListener {
            if (TokenManager.isLogin(this)) {
                DialogUtils.showConfirm(
                    this,
                    "退出登录",
                    "确定要退出登录吗？退出后云端同步功能将不可用",
                    "退出"
                ) {
                    TokenManager.logout(this)
                    updateAccountUI()
                    ToastUtils.show(this, "已退出登录")
                }
            } else {
                startActivityForResult(
                    Intent(this, LoginActivity::class.java),
                    REQUEST_LOGIN
                )
            }
        }
    }

    private fun updateAccountUI() {
        val isLogin = TokenManager.isLogin(this)
        binding.tvAccountTitle.text = if (isLogin) "已登录" else "登录账号"
        binding.tvAccountSummary.text = if (isLogin) {
            "用户ID: ${TokenManager.getUserId(this)}"
        } else {
            "登录后可使用云端同步功能"
        }
    }

    private fun setupSecuritySection() {
        updateSecurityUI()

        binding.itemAppLock.setOnClickListener {
            if (AppLockManager.isLockEnabled(this)) {
                DialogUtils.showConfirm(
                    this,
                    "关闭应用锁",
                    "关闭后任何人都可以直接进入应用查看健康数据，确定关闭吗？",
                    "关闭"
                ) {
                    AppLockManager.setLockEnabled(this, false)
                    AppLockManager.clearPin(this)
                    updateSecurityUI()
                    ToastUtils.show(this, "应用锁已关闭")
                }
            } else {
                startActivityForResult(
                    Intent(this, AppLockActivity::class.java).apply {
                        putExtra("setting_pin", true)
                    },
                    REQUEST_SET_PIN
                )
            }
        }
    }

    private fun updateSecurityUI() {
        val enabled = AppLockManager.isLockEnabled(this)
        binding.tvAppLockTitle.text = if (enabled) "应用锁已开启" else "应用锁"
        binding.tvAppLockSummary.text = if (enabled) "点击关闭" else "设置PIN码保护隐私数据"
    }

    private fun setupDataSection() {
        updateCacheSize()

        binding.itemClearCache.setOnClickListener {
            val cacheSize = getCacheSize()
            DialogUtils.showConfirm(
                this,
                "清除缓存",
                "当前缓存大小：$cacheSize\n确定要清除所有缓存吗？",
                "清除"
            ) {
                clearCache()
                updateCacheSize()
                ToastUtils.show(this, "缓存已清除")
            }
        }

        binding.itemBackupDatabase.setOnClickListener {
            backupDatabase()
        }

        binding.itemRestoreDatabase.setOnClickListener {
            restoreDatabase()
        }
    }

    private fun updateCacheSize() {
        binding.tvCacheSize.text = getCacheSize()
    }

    private fun getCacheSize(): String {
        val cacheDir = cacheDir
        val size = getDirSize(cacheDir)
        return formatFileSize(size)
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getDirSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun clearCache() {
        val cacheDir = cacheDir
        deleteDir(cacheDir)
    }

    private fun deleteDir(dir: File) {
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDir(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun backupDatabase() {
        try {
            val dbPath = getDatabasePath(DatabaseHelper.DATABASE_NAME)
            if (!dbPath.exists()) {
                ToastUtils.show(this, "数据库文件不存在")
                return
            }

            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val backupFile = File(downloadsDir, "health_note_backup_${dateFormat.format(Date())}.db")

            FileInputStream(dbPath).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            ToastUtils.show(this, "备份成功：${backupFile.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.show(this, "备份失败：${e.message}")
        }
    }

    private fun restoreDatabase() {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val backupFiles = downloadsDir.listFiles { _, name -> name.startsWith("health_note_backup_") && name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }

        if (backupFiles.isNullOrEmpty()) {
            ToastUtils.show(this, "Downloads目录中未找到备份文件")
            return
        }

        val fileNames = backupFiles.map { it.name }.toTypedArray()
        DialogUtils.showOptions(this, "选择备份文件", fileNames) { which ->
            val selectedFile = backupFiles[which]
            DialogUtils.showConfirm(
                this,
                "恢复确认",
                "将从 ${selectedFile.name} 恢复数据库，当前数据将被替换，确定继续吗？",
                "恢复"
            ) {
                doRestoreDatabase(selectedFile)
            }
        }
    }

    private fun doRestoreDatabase(backupFile: File) {
        try {
            dbHelper.close()
            val dbPath = getDatabasePath(DatabaseHelper.DATABASE_NAME)

            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }

            ToastUtils.show(this, "恢复成功，请重启应用")
            finishAffinity()
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.show(this, "恢复失败：${e.message}")
        }
    }

    private fun setupAboutSection() {
        binding.tvVersionValue.text = "v${BuildConfig.VERSION_NAME}"

        binding.itemPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }
    }

    private fun showPrivacyPolicy() {
        AlertDialog.Builder(this)
            .setTitle("隐私政策")
            .setMessage("""
健康小助手隐私政策

1. 数据存储：您的所有健康数据均存储在本地设备上，不会主动上传至任何服务器。

2. 云端同步：仅在您主动登录并触发同步时，数据才会传输至您配置的服务器。传输过程采用加密保护。

3. AI评估：健康评估功能通过第三方AI接口实现，评估时相关数据会发送至AI服务端处理。我们不会存储您的评估数据。

4. 日历权限：用药提醒功能需要日历权限以在系统日历中创建提醒事件，您可以随时在系统设置中撤销此权限。

5. 数据安全：我们采用行业标准的加密和存储措施保护您的数据安全。

6. 数据删除：您可以随时在APP内删除任何记录，删除后数据不可恢复。

7. 第三方服务：本应用使用以下第三方服务：
   - AI健康评估（豆包/Kimi等大模型API）
   - 图片加载（Glide）

如有任何疑问，请通过应用内反馈功能联系我们。
            """.trimIndent())
            .setPositiveButton("我知道了", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            updateAccountUI()
            ToastUtils.show(this, "登录成功")
        }
        if (requestCode == REQUEST_SET_PIN && resultCode == RESULT_OK) {
            updateSecurityUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    companion object {
        private const val REQUEST_LOGIN = 3001
        private const val REQUEST_SET_PIN = 3002
    }
}
