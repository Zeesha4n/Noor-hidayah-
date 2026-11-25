package com.noorehidayah.quran.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.components.AyahCard
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.ui.viewmodel.MainUiState
import com.noorehidayah.quran.utils.QuranAudioPlayer
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(
    uiState: MainUiState,
    prefs: UserPreferences,
    strings: Strings,
    onOpenSurah: (Int) -> Unit,
    onCloseReader: () -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloadingSurah by remember { mutableStateOf(false) }

    val activeSurah = uiState.selectedSurah
    if (activeSurah != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCloseReader) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeSurah.surahNumber}. ${activeSurah.nameEnglish}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeSurah.revelationType} • ${activeSurah.ayahCount} ${strings.ayahsLabel}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (uiState.selectedSurahAyahs.isNotEmpty()) {
                        IconButton(
                            enabled = !isDownloadingSurah,
                            onClick = {
                                isDownloadingSurah = true
                                scope.launch {
                                    val nums = uiState.selectedSurahAyahs.map { it.first.ayahNumber }
                                    val count = QuranAudioPlayer.downloadSurahAudio(
                                        context = context,
                                        surahNumber = activeSurah.surahNumber,
                                        ayahNumbers = nums
                                    )
                                    isDownloadingSurah = false
                                    Toast.makeText(
                                        context,
                                        "$count/${nums.size} Ayah audio offline save ho gayi!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            if (isDownloadingSurah) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = "Download Surah Audio")
                            }
                        }
                    }
                    Text(
                        text = activeSurah.nameArabic,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            if (uiState.selectedSurahAyahs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${activeSurah.nameEnglish} (${activeSurah.nameArabic})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Is Surah ki mukammal ayats offline add karne ke liye Settings > Full Quran Pack Import use karein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.selectedSurahAyahs, key = { it.first.id }) { (ayah, tafseers) ->
                        AyahCard(
                            ayah = ayah,
                            surah = activeSurah,
                            tafseers = tafseers,
                            globalLanguage = prefs.language,
                            defaultFiqh = prefs.defaultFiqh,
                            arabicFontSizeSp = prefs.arabicFontSizeSp,
                            isBookmarked = prefs.bookmarks.contains(ayah.id),
                            strings = strings,
                            onToggleBookmark = { onToggleBookmark(ayah.id) }
                        )
                    }
                }
            }
        }
    } else {
        var filterText by remember { mutableStateOf("") }
        val filteredSurahs = remember(uiState.surahs, filterText) {
            if (filterText.isBlank()) uiState.surahs
            else {
                val q = filterText.trim().lowercase()
                uiState.surahs.filter {
                    it.surahNumber.toString() == q ||
                        it.nameEnglish.lowercase().contains(q) ||
                        it.nameHinglish.lowercase().contains(q) ||
                        it.nameHindi.contains(q) ||
                        it.nameArabic.contains(q)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(strings.filterSurahHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSurahs, key = { it.surahNumber }) { surah ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(surah.surahNumber) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = surah.surahNumber.toString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val title = when (prefs.language) {
                                    AppLanguage.HINDI -> surah.nameHindi
                                    AppLanguage.HINGLISH -> surah.nameHinglish
                                    else -> surah.nameEnglish
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val status = if (surah.loadedAyahCount > 0) {
                                    "${surah.revelationType} • ${surah.ayahCount} ${strings.ayahsLabel} • ${strings.offlineReady}"
                                } else {
                                    "${surah.revelationType} • ${surah.ayahCount} ${strings.ayahsLabel}"
                                }
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (surah.loadedAyahCount > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = surah.nameArabic,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
