package com.noorehidayah.quran.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.SearchCategory
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.components.AyahCard
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.ui.viewmodel.MainUiState
import com.noorehidayah.quran.utils.HijriCalendarHelper
import com.noorehidayah.quran.utils.PrayerTimeCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeSearchScreen(
    uiState: MainUiState,
    prefs: UserPreferences,
    strings: Strings,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (SearchCategory) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onToggleHadithBookmark: (Int) -> Unit
) {
    val context = LocalContext.current
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]
    val prayerTimes = remember(city, prefs.prayerMethod, prefs.asrMethod) {
        PrayerTimeCalculator.calculate(city, prefs.prayerMethod, prefs.asrMethod)
    }
    val hijriToday = remember { HijriCalendarHelper.todayHijri() }
    val gregorianToday = remember {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())
    }

    val quickTopics = listOf(
        "Mushkil / Aasani" to "mushkil",
        "Sabr & Waqt" to "sabr",
        "Hifazat / Nazar" to "hifazat",
        "Maafi / Tauba" to "maafi",
        "Walidain" to "walidain",
        "Rizq & Barkat" to "rizq",
        "Ayat al-Kursi" to "kursi",
        "Niyat" to "niyat"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Smart Search Header + Input
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.searchHint,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "FTS4 • Not GenAI",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SearchCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.searchCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat.label, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (uiState.searchQuery.isBlank()) {
            // Hijri Date + Next Prayer Countdown Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${hijriToday.day} ${hijriToday.monthName} ${hijriToday.year} AH • $gregorianToday",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Next: ${prayerTimes.nextPrayerName} (${prayerTimes.nextPrayerTime})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${city.name} • ${prayerTimes.nextPrayerCountdown}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${uiState.loadedAyahCount} Ayahs Offline",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Continue Reading Card
            item {
                val lastSurah = uiState.surahs.firstOrNull { it.surahNumber == prefs.lastReadSurah }
                if (lastSurah != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.continueReading,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${lastSurah.surahNumber}. ${lastSurah.nameEnglish} (${lastSurah.nameArabic})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = { onOpenSurah(lastSurah.surahNumber) }) {
                                Text(strings.openSurah, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Daily Reminder Card
            item {
                val dailyHadith = uiState.hadiths.firstOrNull()
                if (dailyHadith != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Islamic Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = {
                                    val txt = "${dailyHadith.arabic}\n\n${dailyHadith.translationFor(prefs.language)}\n(${dailyHadith.collection} #${dailyHadith.hadithNumber})"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, txt)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Reminder"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                            Text(
                                text = dailyHadith.translationFor(prefs.language),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dailyHadith.collection} #${dailyHadith.hadithNumber} • ${dailyHadith.grading}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search History (if any)
            if (prefs.searchHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prefs.searchHistory.forEach { term ->
                            AssistChip(onClick = { onQueryChange(term) }, label = { Text(term) })
                        }
                    }
                }
            }

            // Quick Topic Chips
            item {
                Text(
                    text = strings.quickTopics,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTopics.forEach { (label, query) ->
                        AssistChip(
                            onClick = { onQueryChange(query) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Bookmarks preview
            if (uiState.bookmarkedResults.isNotEmpty()) {
                item {
                    Text(
                        text = strings.bookmarksTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.bookmarkedResults, key = { "bm_${it.ayah.id}" }) { item ->
                    AyahCard(
                        ayah = item.ayah,
                        surah = item.surah,
                        tafseers = item.tafseers,
                        globalLanguage = prefs.language,
                        defaultFiqh = prefs.defaultFiqh,
                        arabicFontSizeSp = prefs.arabicFontSizeSp,
                        isBookmarked = true,
                        strings = strings,
                        onToggleBookmark = { onToggleBookmark(item.ayah.id) },
                        onOpenSurah = { onOpenSurah(item.ayah.surahNumber) }
                    )
                }
            }
        } else {
            val res = uiState.searchResults
            if (!res.detectedIntent.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res.detectedIntent,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            val totalCount = res.ayahs.size + res.hadiths.size + res.duas.size
            if (totalCount == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = strings.noResults,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                if (res.ayahs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Quran (${res.ayahs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(res.ayahs, key = { "q_${it.ayah.id}" }) { item ->
                        AyahCard(
                            ayah = item.ayah,
                            surah = item.surah,
                            tafseers = item.tafseers,
                            globalLanguage = prefs.language,
                            defaultFiqh = prefs.defaultFiqh,
                            arabicFontSizeSp = prefs.arabicFontSizeSp,
                            isBookmarked = prefs.bookmarks.contains(item.ayah.id),
                            strings = strings,
                            onToggleBookmark = { onToggleBookmark(item.ayah.id) },
                            onOpenSurah = { onOpenSurah(item.ayah.surahNumber) }
                        )
                    }
                }

                if (res.hadiths.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hadith (${res.hadiths.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(res.hadiths, key = { "h_${it.id}" }) { h ->
                        HadithCard(
                            hadith = h,
                            prefs = prefs,
                            isBookmarked = prefs.bookmarkedHadiths.contains(h.id.toString()),
                            onToggleBookmark = { onToggleHadithBookmark(h.id) }
                        )
                    }
                }

                if (res.duas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Duas & Azkar (${res.duas.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(res.duas, key = { "d_${it.id}" }) { d ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${d.category} • ${d.titleFor(prefs.language)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = d.arabic, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = d.translationFor(prefs.language), style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = d.reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
