package com.noorehidayah.quran.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.utils.PrayerTimeCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    duas: List<DuaItem>,
    prefs: UserPreferences,
    strings: Strings,
    onIncrementTasbih: () -> Unit,
    onResetTasbih: () -> Unit,
    onSelectTasbihPreset: (String, Int) -> Unit,
    onSelectCity: (Int) -> Unit,
    onSelectAsrMethod: (AsrMethod) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(strings.prayerTimes, strings.tasbih, strings.duas)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> PrayerTimesPanel(prefs, onSelectCity, onSelectAsrMethod)
            1 -> TasbihPanel(prefs, strings, onIncrementTasbih, onResetTasbih, onSelectTasbihPreset)
            2 -> DuasPanel(duas, prefs)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerTimesPanel(
    prefs: UserPreferences,
    onSelectCity: (Int) -> Unit,
    onSelectAsrMethod: (AsrMethod) -> Unit
) {
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]
    val times = remember(city, prefs.prayerMethod, prefs.asrMethod) {
        PrayerTimeCalculator.calculate(city, prefs.prayerMethod, prefs.asrMethod)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${city.name} • ${prefs.prayerMethod.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsrMethod.entries.forEach { method ->
                    FilterChip(
                        selected = prefs.asrMethod == method,
                        onClick = { onSelectAsrMethod(method) },
                        label = { Text(method.displayName, fontSize = 12.sp) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val rows = listOf(
                    "Fajr" to times.fajr,
                    "Sunrise" to times.sunrise,
                    "Dhuhr" to times.dhuhr,
                    "Asr" to times.asr,
                    "Maghrib" to times.maghrib,
                    "Isha" to times.isha
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    rows.forEachIndexed { idx, (name, time) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                time,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (idx < rows.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        item {
            Text("Offline City Selector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cities.forEachIndexed { idx, c ->
                    FilterChip(
                        selected = idx == prefs.selectedCityIndex,
                        onClick = { onSelectCity(idx) },
                        label = { Text(c.name, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasbihPanel(
    prefs: UserPreferences,
    strings: Strings,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onSelectPreset: (String, Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val presets = listOf(
        "SubhanAllah" to 33,
        "Alhamdulillah" to 33,
        "Allahu Akbar" to 34,
        "Astaghfirullah" to 100
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (label, target) ->
                FilterChip(
                    selected = prefs.tasbihLabel == label,
                    onClick = { onSelectPreset(label, target) },
                    label = { Text("$label ($target)", fontSize = 12.sp) }
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = prefs.tasbihLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${prefs.tasbihCount} / ${prefs.tasbihTarget}",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIncrement()
                },
                shape = CircleShape,
                modifier = Modifier.size(160.dp)
            ) {
                Text(strings.tapToCount, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(onClick = onReset) {
            Text(strings.reset)
        }
    }
}

@Composable
private fun DuasPanel(duas: List<DuaItem>, prefs: UserPreferences) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(duas, key = { it.id }) { dua ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = dua.titleFor(prefs.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = dua.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = prefs.arabicFontSizeSp.sp,
                            lineHeight = (prefs.arabicFontSizeSp * 1.6f).sp,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dua.transliteration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dua.translationFor(prefs.language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dua.reference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
