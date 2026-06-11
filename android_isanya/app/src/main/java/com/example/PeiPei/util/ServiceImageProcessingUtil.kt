// 文件说明：服务配图裁剪、压缩或校验等业务图像处理。

package com.example.Lulu.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ServiceImageProcessingUtil {

    fun loadBitmap(context: Context, uri: Uri, maxLongSide: Int = 2160): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        val srcLongSide = max(srcWidth, srcHeight)
        val sampleSize = if (srcLongSide <= maxLongSide) 1 else {
            var sample = 1
            while (srcLongSide / sample > maxLongSide) sample *= 2
            sample
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    fun saveHighQualityJpeg(
        context: Context,
        bitmap: Bitmap,
        quality: Int = 90
    ): Uri? {
        return try {
            val file = File(
                context.cacheDir,
                "service_img_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }
}
