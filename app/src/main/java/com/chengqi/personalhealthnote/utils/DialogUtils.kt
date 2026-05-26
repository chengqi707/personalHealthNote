package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

/**
 * 弹窗统一工具类
 * 黑色半透明背景，按钮横向排列（取消/确定）
 */
object DialogUtils {

    /**
     * 显示确认弹窗
     * @param title 标题
     * @param message 内容
     * @param positiveText 确定按钮文字
     * @param onPositive 确定回调
     * @param negativeText 取消按钮文字（默认"取消"）
     * @param onNegative 取消回调（默认null）
     */
    fun showConfirm(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "确定",
        negativeText: String = "取消",
        onNegative: ((DialogInterface) -> Unit)? = null,
        onPositive: ((DialogInterface) -> Unit)? = null
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { dialog, which ->
                onPositive?.invoke(dialog)
            }
            .setNegativeButton(negativeText) { dialog, which ->
                onNegative?.invoke(dialog)
            }
            .setCancelable(true)
            .show()
    }

    /**
     * 显示选项列表弹窗
     */
    fun showOptions(
        context: Context,
        title: String,
        options: Array<String>,
        onSelect: (Int) -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options) { _, which ->
                onSelect(which)
            }
            .show()
    }
}
