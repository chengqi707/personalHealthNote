package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.widget.Toast

/**
 * Toast统一工具类
 * 所有操作结果Toast底部显示，时长1.5s
 */
object ToastUtils {

    private var toast: Toast? = null

    fun show(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }
}
