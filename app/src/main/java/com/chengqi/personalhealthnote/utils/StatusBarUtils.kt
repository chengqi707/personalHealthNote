package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat

/**
 * 状态栏适配工具类
 * 自动适配所有机型，解决布局与状态栏重叠问题
 */
object StatusBarUtils {

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            // 默认24dp
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                context.resources.displayMetrics
            ).toInt()
        }
    }

    /**
     * 适配状态栏：给View添加顶部padding，高度等于状态栏高度
     * 适用于页面的顶部布局（比如Toolbar、标题栏等）
     */
    fun adaptStatusBar(view: View) {
        val statusBarHeight = getStatusBarHeight(view.context)
        view.setPadding(
            view.paddingLeft,
            view.paddingTop + statusBarHeight,
            view.paddingRight,
            view.paddingBottom
        )
    }

    /**
     * 给根布局适配状态栏，添加顶部margin，避免内容被状态栏挡住
     * 适用于页面的根布局
     */
    fun adaptRootLayout(view: View) {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin += getStatusBarHeight(view.context)
        view.layoutParams = layoutParams
    }

    /**
     * 设置状态栏文字颜色
     * @param isDark 是否为深色文字（true=黑色文字，适合浅色背景；false=白色文字，适合深色背景）
     */
    fun setStatusBarTextColor(window: Window, isDark: Boolean) {
        val insetsController = window.insetsController
        if (isDark) {
            // 深色文字
            insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            // 浅色文字
            insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    /**
     * 自动根据当前深色模式设置状态栏文字颜色
     */
    fun autoAdaptStatusBarTextColor(window: Window, context: Context) {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES
        setStatusBarTextColor(window, !isDarkMode)
    }

    /**
     * 设置状态栏颜色
     */
    fun setStatusBarColor(window: Window, color: Int) {
        window.statusBarColor = color
    }

    /**
     * 全屏沉浸式，隐藏状态栏和导航栏
     */
    fun setFullScreen(window: Window) {
        window.insetsController?.hide(
            android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
        )
    }

    /**
     * 退出全屏，显示状态栏和导航栏
     */
    fun exitFullScreen(window: Window) {
        window.insetsController?.show(
            android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
        )
    }

    /**
     * 判断当前是否为深色模式
     */
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}