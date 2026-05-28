package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    /**
     * 将详情内容生成图片并分享
     * @param context 上下文
     * @param title 分享标题
     * @param lines 文本内容行
     */
    fun shareAsImage(context: Context, title: String, lines: List<String>) {
        try {
            val bitmap = generateTextImage(context, title, lines)
            val imageFile = saveBitmapToCache(context, bitmap)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "分享到"))
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.show(context, "分享失败：${e.message}")
        }
    }

    private fun generateTextImage(context: Context, title: String, lines: List<String>): Bitmap {
        val padding = 48
        val lineHeight = 56
        val titleLineHeight = 72
        val maxWidth = 800

        // 计算画布高度
        val titleHeight = titleLineHeight + 24
        val contentHeight = lines.size * lineHeight
        val totalHeight = padding + titleHeight + contentHeight + padding

        val bitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景
        canvas.drawColor(Color.WHITE)

        // 标题
        val titlePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(title, padding.toFloat(), (padding + titleLineHeight).toFloat(), titlePaint)

        // 分割线
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 2f
        }
        val dividerY = padding + titleHeight - 8f
        canvas.drawLine(padding.toFloat(), dividerY, (maxWidth - padding).toFloat(), dividerY, linePaint)

        // 内容
        val contentPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 28f
            isAntiAlias = true
        }

        var y = padding + titleHeight + lineHeight.toFloat()
        for (line in lines) {
            // 自动换行
            val maxWidthForText = maxWidth - 2 * padding
            var start = 0
            while (start < line.length) {
                val chars = contentPaint.breakText(line, start, line.length, true, maxWidthForText.toFloat(), null)
                canvas.drawText(line, start, start + chars, padding.toFloat(), y, contentPaint)
                y += lineHeight.toFloat()
                start += chars
            }
        }

        // 底部水印
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 20f
            isAntiAlias = true
        }
        val watermarkText = "由健康小助手生成"
        canvas.drawText(watermarkText, (maxWidth - padding - watermarkPaint.measureText(watermarkText)).toFloat(),
            (totalHeight - 24).toFloat(), watermarkPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val shareDir = File(context.cacheDir, "share")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val file = File(shareDir, "health_share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
