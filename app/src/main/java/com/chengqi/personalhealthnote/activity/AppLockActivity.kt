package com.chengqi.personalhealthnote.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.databinding.ActivityAppLockBinding
import com.chengqi.personalhealthnote.utils.AppLockManager
import java.util.concurrent.Executor

class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private var isSettingPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isSettingPin = intent.getBooleanExtra("setting_pin", false)

        if (isSettingPin) {
            setupPinSetting()
        } else {
            setupUnlock()
        }
    }

    private fun setupUnlock() {
        binding.tvTitle.text = "请验证身份"
        binding.tvSubtitle.text = "使用生物识别或PIN码解锁"
        binding.btnConfirm.text = "解锁"

        // 尝试生物识别
        if (AppLockManager.isPinSet(this)) {
            tryBiometric()
        }

        binding.btnConfirm.setOnClickListener {
            val pin = binding.etPin.text.toString()
            if (pin.length != 4) {
                Toast.makeText(this, "请输入4位PIN码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (AppLockManager.verifyPin(this, pin)) {
                unlockSuccess()
            } else {
                Toast.makeText(this, "PIN码错误", Toast.LENGTH_SHORT).show()
                binding.etPin.text?.clear()
            }
        }
    }

    private fun tryBiometric() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockSuccess()
                }
                override fun onAuthenticationFailed() {
                    // 生物识别失败，用户可用PIN码
                }
            })

        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("健康小助手")
            .setSubtitle("请验证身份以解锁应用")
            .setAllowedAuthenticators(authenticators)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // 设备不支持生物识别，使用PIN码
        }
    }

    private fun setupPinSetting() {
        binding.tvTitle.text = "设置PIN码"
        binding.tvSubtitle.text = "请输入4位数字PIN码"
        binding.btnConfirm.text = "确定"

        binding.btnConfirm.setOnClickListener {
            val pin = binding.etPin.text.toString()
            if (pin.length != 4) {
                Toast.makeText(this, "请输入4位PIN码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppLockManager.setPin(this, pin)
            AppLockManager.setLockEnabled(this, true)
            Toast.makeText(this, "PIN码设置成功", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun unlockSuccess() {
        runOnUiThread {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onBackPressed() {
        // 解锁界面不允许返回
    }
}
