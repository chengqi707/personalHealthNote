package com.chengqi.personalhealthnote.activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chengqi.personalhealthnote.databinding.ActivityLoginBinding
import com.chengqi.personalhealthnote.network.ApiService
import com.chengqi.personalhealthnote.utils.TokenManager
/**
 * 登录注册页面
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var isRegisterMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        setupListeners()
    }
    private fun initViews() {
        supportActionBar?.title = if (isRegisterMode) "注册" else "登录"
        binding.btnSubmit.text = if (isRegisterMode) "注册" else "登录"
        binding.btnToggleMode.text = if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册"
    }
    private fun setupListeners() {
        binding.btnToggleMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            initViews()
        }
        binding.btnSubmit.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnSubmit.isEnabled = false
            if (isRegisterMode) {
                register(username, password)
            } else {
                login(username, password)
            }
        }
    }
    private fun login(username: String, password: String) {
        ApiService.login(username, password) { success, message, userId, token ->
            runOnUiThread {
                binding.btnSubmit.isEnabled = true
                if (success && userId != null && token != null) {
                    TokenManager.saveLoginInfo(this, token, userId)
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, message ?: "登录失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun register(username: String, password: String) {
        ApiService.register(username, password) { success, message, userId, token ->
            runOnUiThread {
                binding.btnSubmit.isEnabled = true
                if (success && userId != null && token != null) {
                    TokenManager.saveLoginInfo(this, token, userId)
                    Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, message ?: "注册失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}