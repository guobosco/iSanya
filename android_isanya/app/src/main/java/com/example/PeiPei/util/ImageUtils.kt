// 文件说明：图片加载、缩放、格式等通用图像工具。

package com.example.Lulu.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Picture
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream

fun createBitmapFromPicture(picture: Picture): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawPicture(picture)
    return bitmap
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String? = null): Uri? {
    val filename = if (title != null) {
        "$title-${System.currentTimeMillis()}.png"
    } else {
        "FeiLing_Share_${System.currentTimeMillis()}.png"
    }
    
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        var stream: OutputStream? = null
        try {
            stream = resolver.openOutputStream(it)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            
            // Show toast on success
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            }
            
            return it
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(it, null, null)
            
            // Show toast on failure
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            stream?.close()
        }
    }
    return null
}
