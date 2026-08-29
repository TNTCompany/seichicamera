package com.tnt.seichicamera.ui.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

object ComparisonGenerator {

    private const val MAX_SINGLE_DIMENSION = 1280

    suspend fun generate(
        context: Context,
        referenceImageSource: Any, // Uri or URL String
        photoUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        var refBitmap: Bitmap? = null
        var photoBitmap: Bitmap? = null
        var refScaled: Bitmap? = null
        var photoScaled: Bitmap? = null
        var comparison: Bitmap? = null

        try {
            val imageLoader = ImageLoader(context)

            // Load reference image (bounded size)
            val refRequest = ImageRequest.Builder(context)
                .data(referenceImageSource)
                .size(MAX_SINGLE_DIMENSION, MAX_SINGLE_DIMENSION)
                .build()
            val refResult = imageLoader.execute(refRequest)
            refBitmap = (refResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Load photo (bounded size)
            val photoRequest = ImageRequest.Builder(context)
                .data(photoUri)
                .size(MAX_SINGLE_DIMENSION, MAX_SINGLE_DIMENSION)
                .build()
            val photoResult = imageLoader.execute(photoRequest)
            photoBitmap = (photoResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Create side-by-side comparison with normalized dimensions
            var targetWidth = maxOf(refBitmap.width, photoBitmap.width)
            var targetHeight = maxOf(refBitmap.height, photoBitmap.height)

            // Limit target dimensions to prevent OOM
            if (targetWidth > MAX_SINGLE_DIMENSION || targetHeight > MAX_SINGLE_DIMENSION) {
                val scale = minOf(
                    MAX_SINGLE_DIMENSION.toFloat() / targetWidth,
                    MAX_SINGLE_DIMENSION.toFloat() / targetHeight
                )
                targetWidth = (targetWidth * scale).toInt().coerceAtLeast(1)
                targetHeight = (targetHeight * scale).toInt().coerceAtLeast(1)
            }

            val watermarkHeight = (targetHeight * 0.05f).toInt().coerceIn(36, 72)
            val totalWidth = targetWidth * 2
            val totalHeight = targetHeight + watermarkHeight

            comparison = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(comparison)
            canvas.drawColor(Color.BLACK)

            // Draw reference image (left)
            refScaled = Bitmap.createScaledBitmap(refBitmap, targetWidth, targetHeight, true)
            canvas.drawBitmap(refScaled, 0f, 0f, null)

            // Draw photo (right)
            photoScaled = Bitmap.createScaledBitmap(photoBitmap, targetWidth, targetHeight, true)
            canvas.drawBitmap(photoScaled, targetWidth.toFloat(), 0f, null)

            // Draw watermark
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = (watermarkHeight * 0.45f).coerceIn(16f, 32f)
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            canvas.drawText(
                "Data: Anitabi · Photo: SeichiCamera",
                16f,
                targetHeight + watermarkHeight - (watermarkHeight * 0.25f),
                paint
            )

            // Save to MediaStore
            val name = "comparison_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera/Comparisons")
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                comparison.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }

            uri
        } catch (e: Exception) {
            null
        } finally {
            try {
                if (refScaled != null && refScaled !== refBitmap) refScaled.recycle()
                if (photoScaled != null && photoScaled !== photoBitmap) photoScaled.recycle()
                refBitmap?.recycle()
                photoBitmap?.recycle()
                comparison?.recycle()
            } catch (_: Exception) {}
        }
    }
}
