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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.components.AyahCard
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeSearchScreen(
    uiState: MainUiState,
    prefs: UserPreferences,
    strings: Strings,
    onQueryChange: (String) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    val quickTopics = listOf(
        "Mushkil / Aasani" to "mushkil",
        "Sabr & Waqt" to "sabr",
        "Hifazat / Nazar" to "hifazat",
        "Maafi / Tauba" to "maafi",
        "Waswasa / Sukoon" to "waswasa",
        "Tauheed" to "tauheed",
        "Ayat al-Kursi" to "kursi",
        "Shukr / Dua" to "bhalai"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.searchHint,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
        }

        if (uiState.searchQuery.isBlank()) {
            // Continue Reading Card
            item {
                val lastSurah = uiState.surahs.firstOrNull { it.surahNumber == prefs.lastReadSurah }
                if (lastSurah != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.continueReading,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${lastSurah.surahNumber}. ${lastSurah.nameEnglish} (${lastSurah.nameArabic})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            TextButton(onClick = { onOpenSurah(lastSurah.surahNumber) }) {
                                Text(strings.openSurah, fontWeight = FontWeight.Bold)
                            }
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
            if (uiState.searchResults.isEmpty()) {
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
                items(uiState.searchResults, key = { it.ayah.id }) { res ->
                    AyahCard(
                        ayah = res.ayah,
                        surah = res.surah,
                        tafseers = res.tafseers,
                        globalLanguage = prefs.language,
                        defaultFiqh = prefs.defaultFiqh,
                        arabicFontSizeSp = prefs.arabicFontSizeSp,
                        isBookmarked = prefs.bookmarks.contains(res.ayah.id),
                        strings = strings,
                        onToggleBookmark = { onToggleBookmark(res.ayah.id) },
                        onOpenSurah = { onOpenSurah(res.ayah.surahNumber) }
                    )
                }
            }
        }
    }
}
