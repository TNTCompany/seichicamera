package com.tnt.seichicamera.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tnt.seichicamera.R
import com.tnt.seichicamera.domain.model.SacredPoint

@Composable
fun PointDetailSheet(
    point: SacredPoint,
    isCheckedIn: Boolean,
    onNavigate: () -> Unit,
    onShootWithImage: (imageIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = point.name ?: stringResource(R.string.unknown_point),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (isCheckedIn) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.checked_in),
                    tint = Color(0xFF4CAF50)
                )
            }
        }

        if (point.ep != null) {
            Text(
                text = point.ep,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        // Reference images
        if (point.imageUrls.isNotEmpty()) {
            Text(stringResource(R.string.reference_images), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                itemsIndexed(point.imageUrls) { index, url ->
                    AsyncImage(
                        model = url.replace("h360", "h160"),
                        contentDescription = "Reference image ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp, 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShootWithImage(index) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onNavigate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.navigate_to))
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { onShootWithImage(0) },
                modifier = Modifier.weight(1f),
                enabled = point.imageUrls.isNotEmpty()
            ) {
                Text(stringResource(R.string.shoot_with_image))
            }
        }

        // Attribution
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        val targetUrl = point.originUrl?.takeIf { it.isNotBlank() } ?: "https://anitabi.cn"
        Text(
            text = if (!point.originUrl.isNullOrBlank()) {
                stringResource(R.string.data_source_view_source)
            } else {
                stringResource(R.string.data_source_anitabi)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = TextDecoration.Underline
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Ignore if no browser application is available
                }
            }
        )
    }
}
