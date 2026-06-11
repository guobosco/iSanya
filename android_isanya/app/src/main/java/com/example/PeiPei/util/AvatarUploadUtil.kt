// 文件说明：头像选择与上传到服务器的工具方法。

package com.example.Lulu.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.Lulu.data.repository.LuluRepository
import com.example.Lulu.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object AvatarUploadUtil {

    /**
     * 将 Uri 的图片压缩保存到本地临时文件，然后上传到服务器
     * 返回服务器的网络 URL，如果失败返回 null
     */
    suspend fun processAndUploadAvatar(
        context: Context,
        uri: Uri,
        repository: LuluRepository
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 保存并压缩到本地文件
            val tempFile = saveAvatarToInternalStorage(context, uri) ?: return@withContext null

            // 2. 调用网络接口上传
            val serverUrl = repository.uploadAvatar(tempFile)
            
            // 可选：上传成功后可以删除临时文件，不过保留也可以用于本地缓存
            // tempFile.delete()

            if (serverUrl != null) {
                // 直接返回相对路径，由 Coil Mapper 在加载时自动拼接 BASE_URL
                // 这样可以确保数据库中保存的是相对路径，在不同网络环境下（如真机和模拟器）都能正常工作
                return@withContext serverUrl
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveAvatarToInternalStorage(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Crop to square (center crop)
            val size = Math.min(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)

            // Resize to 500x500
            val targetSize = 500
            val finalBitmap = if (squaredBitmap.width != targetSize) {
                Bitmap.createScaledBitmap(squaredBitmap, targetSize, targetSize, true)
            } else {
                squaredBitmap
            }

            // Save to internal storage
            val filename = "avatar_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            val outputStream = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
