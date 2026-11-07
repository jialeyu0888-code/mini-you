package com.miniyou.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object ImageStore {

    private fun imagesDir(context: Context) = File(context.filesDir, "images").apply { mkdirs() }

    /** 为相机拍照准备一个临时输出地址。 */
    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** 把相册/相机图片压缩后保存到应用私有目录，返回文件绝对路径。 */
    suspend fun importImage(context: Context, uri: Uri, maxSide: Int = 2048): String? = withContext(Dispatchers.IO) {
        val bytes = loadJpeg(context, uri, maxSide, 88) ?: return@withContext null
        val file = File(imagesDir(context), "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.parentFile?.name == "images" }?.delete() }
    }

    /**
     * 读取图片、按 EXIF 纠正方向并缩放到长边不超过 [maxSide]，输出 JPEG。
     * 智谱接口要求单图 < 5MB、像素不超过 6000×6000，这里会继续降质量直到满足大小限制。
     */
    suspend fun loadJpeg(context: Context, uri: Uri, maxSide: Int, quality: Int = 85, maxBytes: Int = 4_000_000): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0) return@runCatching null
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                val decoded = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@runCatching null
                val rotation = resolver.openInputStream(uri)?.use {
                    when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
                val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
                val matrix = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    if (rotation != 0f) postRotate(rotation)
                }
                val bmp = if (scale < 1f || rotation != 0f) {
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                } else decoded
                var q = quality
                var out: ByteArray
                do {
                    out = ByteArrayOutputStream().use { bos ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, q, bos); bos.toByteArray()
                    }
                    q -= 10
                } while (out.size > maxBytes && q > 30)
                out
            }.getOrNull()
        }
}
