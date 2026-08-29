package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tnt.seichicamera.R

@Composable
fun PostCaptureSheet(
    photoUri: Uri,
    pointId: String?,
    onCheckIn: () -> Unit,
    onGenerateComparison: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.photo_saved), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        AsyncImage(
            model = photoUri,
            contentDescription = "Captured photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Check-in button (only if from map with pointId)
        if (!pointId.isNullOrBlank()) {
            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.check_in))
            }
            Spacer(Modifier.height(8.dp))
        }

        // Generate comparison
        OutlinedButton(
            onClick = onGenerateComparison,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Compare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.generate_comparison))
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.close))
        }
    }
}
