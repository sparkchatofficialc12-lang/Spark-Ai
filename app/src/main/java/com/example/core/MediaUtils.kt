package com.example.core

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object MediaUtils {

    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun createTempCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "spark_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun uriToBitmap(context: Context, uri: Uri, maxDim: Int = 720): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                var halfW = bounds.outWidth / 2
                var halfH = bounds.outHeight / 2
                while ((halfW / sampleSize) >= maxDim && (halfH / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream, null, decodeOpts) ?: return null
                scaleBitmap(raw, maxDim)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uriToBitmapAsync(context: Context, uri: Uri, maxDim: Int = 720): Bitmap? =
        withContext(Dispatchers.IO) {
            uriToBitmap(context, uri, maxDim)
        }

    fun scaleBitmap(bitmap: Bitmap, maxDim: Int = 512): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (ratio >= 1f) {
            targetW = maxDim
            targetH = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDim
            targetW = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    fun bitmapToCompactBase64(bitmap: Bitmap, maxChars: Int = 220_000): String {
        val scaled = scaleBitmap(bitmap, 480)
        var quality = 80
        var encoded = ""
        while (quality >= 35) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length <= maxChars) break
            quality -= 15
        }
        return encoded
    }

    suspend fun bitmapToCompactBase64Async(bitmap: Bitmap, maxChars: Int = 220_000): String =
        withContext(Dispatchers.Default) {
            bitmapToCompactBase64(bitmap, maxChars)
        }

    fun base64ToBitmap(data: String): Bitmap? {
        if (data.isBlank()) return null
        return try {
            val clean = data.substringAfter("base64,", data)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadMp4ToDevice(
        context: Context,
        sourceUrl: String,
        movieTitle: String,
        resolution: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (sourceUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Movie video URL is empty."))
            }
            val safeName = movieTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
            val fileName = "SparkAI_${safeName}_${resolution}_${System.currentTimeMillis()}.mp4"

            val inputBytesStream = if (sourceUrl.startsWith("content://") || sourceUrl.startsWith("file://")) {
                val uri = Uri.parse(sourceUrl)
                context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("Could not open local video URI."))
            } else {
                val req = Request.Builder().url(sourceUrl).build()
                val resp = downloadClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Download failed with HTTP ${resp.code}"))
                }
                resp.body?.byteStream()
                    ?: return@withContext Result.failure(IllegalStateException("Empty video response body."))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SparkAI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Unable to create MediaStore video entry."))

                resolver.openOutputStream(itemUri)?.use { out ->
                    inputBytesStream.use { input ->
                        val buffer = ByteArray(16 * 1024)
                        var totalRead = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            totalRead += read
                            onProgress((totalRead / 5_000_000f).coerceIn(0.1f, 0.95f))
                        }
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                onProgress(1f)
                Result.success(itemUri.toString())
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                dir.mkdirs()
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    inputBytesStream.use { input ->
                        input.copyTo(out)
                    }
                }
                onProgress(1f)
                Result.success(Uri.fromFile(outFile).toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareMovieTextOrLink(context: Context, title: String, videoUrl: String, scriptSummary: String = "") {
        val body = buildString {
            appendLine("Watch \"$title\" created with Spark AI — Imagine It. Create It. Watch It.")
            if (videoUrl.isNotBlank()) {
                appendLine("Video: $videoUrl")
            }
            if (scriptSummary.isNotBlank()) {
                appendLine()
                appendLine(scriptSummary.take(400))
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Spark AI Movie: $title")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share Spark AI Movie"))
    }

    fun calculateLocalCacheSizeBytes(context: Context): Long {
        fun folderSize(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return folderSize(context.cacheDir) + folderSize(File(context.filesDir, "movie_exports"))
    }

    fun clearTemporaryCache(context: Context): Long {
        val before = calculateLocalCacheSizeBytes(context)
        context.cacheDir?.deleteRecursively()
        context.cacheDir?.mkdirs()
        return before
    }
}
