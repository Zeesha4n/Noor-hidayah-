package com.noorehidayah.quran.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    prefs: UserPreferences,
    strings: Strings,
    onLanguageChange: (AppLanguage) -> Unit,
    onFiqhChange: (FiqhSchool) -> Unit,
    onArabicSizeChange: (Float) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onPrayerMethodChange: (PrayerCalcMethod) -> Unit,
    onImportPack: (Uri) -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportPack(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingCard(title = strings.languageSetting) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = prefs.language == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.displayName) }
                        )
                    }
                }
            }
        }

        item {
            SettingCard(title = strings.defaultFiqhSetting) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiqhSchool.entries.forEach { fiqh ->
                        FilterChip(
                            selected = prefs.defaultFiqh == fiqh,
                            onClick = { onFiqhChange(fiqh) },
                            label = { Text(fiqh.displayName) }
                        )
                    }
                }
            }
        }

        item {
            SettingCard(title = "${strings.arabicFontSize}: ${prefs.arabicFontSizeSp.toInt()}sp") {
                Slider(
                    value = prefs.arabicFontSizeSp,
                    onValueChange = onArabicSizeChange,
                    valueRange = 20f..38f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.darkMode, style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.darkMode, onCheckedChange = onDarkModeChange)
                }
            }
        }

        item {
            SettingCard(title = "Prayer Calculation Method") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrayerCalcMethod.entries.forEach { method ->
                        FilterChip(
                            selected = prefs.prayerMethod == method,
                            onClick = { onPrayerMethodChange(method) },
                            label = { Text(method.displayName) }
                        )
                    }
                }
            }
        }

        item {
            SettingCard(title = strings.importOfflinePack) {
                Text(
                    text = strings.importPackDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Offline DB: ${uiState.loadedAyahCount} Ayahs • ${uiState.loadedTafseerCount} Tafseer entries loaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { filePicker.launch(arrayOf("application/json", "text/*")) }) {
                    Text("Select JSON Pack")
                }
                if (uiState.importStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.importStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
