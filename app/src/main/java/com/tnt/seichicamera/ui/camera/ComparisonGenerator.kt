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

    suspend fun generate(
        context: Context,
        referenceImageSource: Any, // Uri or URL String
        photoUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(context)

            // Load reference image
            val refRequest = ImageRequest.Builder(context).data(referenceImageSource).build()
            val refResult = imageLoader.execute(refRequest)
            val refBitmap = (refResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Load photo
            val photoRequest = ImageRequest.Builder(context).data(photoUri).build()
            val photoResult = imageLoader.execute(photoRequest)
            val photoBitmap = (photoResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Create side-by-side comparison
            val width = maxOf(refBitmap.width, photoBitmap.width)
            val height = maxOf(refBitmap.height, photoBitmap.height)
            val watermarkHeight = 48
            val totalWidth = width * 2
            val totalHeight = height + watermarkHeight

            val comparison = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(comparison)
            canvas.drawColor(Color.BLACK)

            // Draw reference image (left)
            val refScaled = Bitmap.createScaledBitmap(refBitmap, width, height, true)
            canvas.drawBitmap(refScaled, 0f, 0f, null)

            // Draw photo (right)
            val photoScaled = Bitmap.createScaledBitmap(photoBitmap, width, height, true)
            canvas.drawBitmap(photoScaled, width.toFloat(), 0f, null)

            // Draw watermark
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            canvas.drawText(
                "Data: Anitabi · Photo: SeichiCamera",
                16f,
                height + watermarkHeight - 12f,
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

            // Cleanup
            refScaled.recycle()
            photoScaled.recycle()
            comparison.recycle()

            uri
        } catch (e: Exception) {
            null
        }
    }
}
