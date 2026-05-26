package com.chengqi.personalhealthnote.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageCompressUtils {

    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1920
    private const val TARGET_SIZE_KB = 500
    private const val QUALITY_STEP = 10

    fun compressToPrivateDir(
        context: Context,
        uri: Uri,
        destDir: String,
        fileName: String
    ): String? {
        return try {
            // 先读取图片尺寸
            val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(boundsStream, null, options)
            boundsStream.close()

            // 计算采样率
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

            // 解码图片
            val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()

            if (bitmap == null) return null

            // 旋转修正
            val rotatedBitmap = rotateIfRequired(context, uri, bitmap)
            val finalBitmap = if (rotatedBitmap != bitmap) {
                bitmap.recycle()
                rotatedBitmap
            } else {
                bitmap
            }

            // 缩放到目标尺寸
            val scaledBitmap = scaleBitmap(finalBitmap)
            if (scaledBitmap != finalBitmap) {
                finalBitmap.recycle()
            }

            // 保存压缩后的图片
            val privateDir = context.getDir(destDir, Context.MODE_PRIVATE)
            val destFile = File(privateDir, fileName)
            compressAndSave(scaledBitmap, destFile)

            scaledBitmap.recycle()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            val widthRatio = (width.toFloat() / MAX_WIDTH).toInt()
            val heightRatio = (height.toFloat() / MAX_HEIGHT).toInt()
            sampleSize = max(widthRatio, heightRatio).coerceAtLeast(1)
        }
        return sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH && bitmap.height <= MAX_HEIGHT) {
            return bitmap
        }
        val scale = min(MAX_WIDTH.toFloat() / bitmap.width, MAX_HEIGHT.toFloat() / bitmap.height)
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compressAndSave(bitmap: Bitmap, file: File) {
        var quality = 85
        do {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            if (file.length() / 1024 <= TARGET_SIZE_KB || quality <= 10) break
            quality -= QUALITY_STEP
        } while (true)
    }

    private fun rotateIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exifStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(exifStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            exifStream.close()
            val degree = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }
            val matrix = Matrix().apply { postRotate(degree) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
