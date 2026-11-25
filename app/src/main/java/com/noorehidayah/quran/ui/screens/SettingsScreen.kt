package com.noorehidayah.quran.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.ui.viewmodel.MainUiState
import com.noorehidayah.quran.utils.ReminderScheduler

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
    onRemindersChange: (Boolean) -> Unit,
    onImportPack: (Uri) -> Unit
) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportPack(uri)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRemindersChange(granted)
        if (granted) ReminderScheduler.scheduleDailyReminder(context, true)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Islamic Reminders (7:00 AM)", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.remindersEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                onRemindersChange(false)
                                ReminderScheduler.scheduleDailyReminder(context, false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onRemindersChange(true)
                                ReminderScheduler.scheduleDailyReminder(context, true)
                            }
                        }
                    )
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
                    text = "Offline DB: ${uiState.loadedAyahCount} Ayahs • ${uiState.loadedTafseerCount} Tafseers • ${uiState.loadedHadithCount} Hadiths",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { filePicker.launch(arrayOf("*/*")) }) {
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

        item {
            SettingCard(title = "Privacy & Offline Guarantee") {
                Text(
                    text = "Noor-e-Hidayah aapka koi personal data collect ya track nahi karta. Sabhi bookmarks, search history, aur settings sirf aapke phone par local rehti hain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
