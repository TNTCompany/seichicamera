package com.tnt.seichicamera.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tnt.seichicamera.R
import com.tnt.seichicamera.util.LocaleHelper

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {
        // Language section
        item {
            Text(
                stringResource(R.string.pref_header_general),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            val currentLang = LocaleHelper.languages.find { it.tag == uiState.currentLocaleTag }
            val currentLangDisplayName = if (uiState.currentLocaleTag.isEmpty()) {
                stringResource(R.string.pref_lang_default)
            } else {
                currentLang?.displayName ?: stringResource(R.string.pref_lang_default)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_title_language)) },
                supportingContent = { Text(currentLangDisplayName) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            HorizontalDivider()
        }

        // Cache section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.offline_cache),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.cachedBangumis.isNotEmpty()) {
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.clear_all_cache)
                        )
                    }
                }
            }
        }

        if (uiState.cachedBangumis.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_cached_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(uiState.cachedBangumis) { bangumi ->
                ListItem(
                    headlineContent = { Text(bangumi.title) },
                    supportingContent = { Text("ID: ${bangumi.id}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.clearCache(bangumi.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_cache)
                            )
                        }
                    }
                )
            }
        }

        // About section
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.pref_header_about),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_title_version)) },
                supportingContent = { Text(stringResource(R.string.version_number)) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.data_source)) },
                supportingContent = { Text(stringResource(R.string.data_source_anitabi)) }
            )
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.pref_dialog_title_language)) },
            text = {
                Column {
                    LocaleHelper.languages.forEach { lang ->
                        val displayName = if (lang.tag.isEmpty()) {
                            stringResource(R.string.pref_lang_default)
                        } else {
                            lang.displayName
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang.tag)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lang.tag == uiState.currentLocaleTag,
                                onClick = {
                                    viewModel.setLanguage(lang.tag)
                                    showLanguageDialog = false
                                }
                            )
                            Text(displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear all confirmation
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.clear_all_cache_confirm_title)) },
            text = { Text(stringResource(R.string.clear_all_cache_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllCache(); showClearAllDialog = false }) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
