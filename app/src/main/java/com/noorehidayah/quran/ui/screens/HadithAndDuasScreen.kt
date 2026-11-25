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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.MaslaItem
import com.noorehidayah.quran.data.model.UserPreferences

@Composable
fun HadithAndDuasScreen(
    hadiths: List<HadithItem>,
    duas: List<DuaItem>,
    masail: List<MaslaItem>,
    prefs: UserPreferences,
    isDownloadingHadith: Boolean,
    hadithDownloadStatus: String?,
    onDownloadHadithPack: (String, String, Int) -> Unit,
    onToggleHadithBookmark: (Int) -> Unit,
    onToggleDuaBookmark: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hadith (${hadiths.size})", "Masail (Fiqh)", "Duas & Azkar")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> HadithListPanel(
                hadiths = hadiths,
                prefs = prefs,
                isDownloading = isDownloadingHadith,
                downloadStatus = hadithDownloadStatus,
                onDownloadPack = onDownloadHadithPack,
                onToggleBookmark = onToggleHadithBookmark
            )
            1 -> MasailPanel(masail, prefs)
            2 -> DuasCategorizedPanel(duas, prefs, onToggleDuaBookmark)
        }
    }
}

@Composable
fun HadithCard(
    hadith: HadithItem,
    prefs: UserPreferences,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    val context = LocalContext.current
    val isSahih = hadith.grading.contains("Sahih", ignoreCase = true) ||
        hadith.collection.contains("Sahih", ignoreCase = true)
    val isDaif = hadith.grading.contains("Da'if", ignoreCase = true) ||
        hadith.grading.contains("Weak", ignoreCase = true)

    val badgeColor = when {
        isDaif -> Color(0xFFB71C1C)
        isSahih -> Color(0xFF0D6E52)
        else -> Color(0xFF8C6D1F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Prominent Authenticity Grading Banner at the VERY TOP
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSahih) "AUTHENTIC • ${hadith.grading}" else hadith.grading,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${hadith.collection} #${hadith.hadithNumber}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val shareText = "[${hadith.grading}]\n${hadith.arabic}\n\n${hadith.translationFor(prefs.language)}\n\n— ${hadith.collection} (${hadith.bookNumber}, #${hadith.hadithNumber})"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Hadith"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hadith.arabic.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = hadith.arabic,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = (prefs.arabicFontSizeSp - 2f).coerceAtLeast(20f).sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Right
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (hadith.transliteration.isNotBlank()) {
                Text(
                    text = hadith.transliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = hadith.translationFor(prefs.language),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${hadith.bookNumber} • Topic: ${hadith.topic}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HadithListPanel(
    hadiths: List<HadithItem>,
    prefs: UserPreferences,
    isDownloading: Boolean,
    downloadStatus: String?,
    onDownloadPack: (String, String, Int) -> Unit,
    onToggleBookmark: (Int) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(hadiths, filter) {
        if (filter.isBlank()) hadiths.take(100)
        else {
            val q = filter.trim().lowercase()
            hadiths.filter {
                it.collection.lowercase().contains(q) ||
                    it.hadithNumber.lowercase().contains(q) ||
                    it.topic.lowercase().contains(q) ||
                    it.hinglish.lowercase().contains(q) ||
                    it.english.lowercase().contains(q)
            }.take(100)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1-Tap Full Hadith Collection Downloader (Online -> Offline forever)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download Full Hadith Books (Offline)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = { onDownloadPack("eng-bukhari", "Sahih al-Bukhari", 10000) },
                            label = { Text("Sahih Bukhari (7,563)", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = { onDownloadPack("eng-muslim", "Sahih Muslim", 20000) },
                            label = { Text("Sahih Muslim (7,563)", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = { onDownloadPack("eng-tirmidhi", "Jami' at-Tirmidhi", 30000) },
                            label = { Text("Tirmidhi (3,956)", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = { onDownloadPack("eng-abudawud", "Sunan Abi Dawud", 40000) },
                            label = { Text("Abu Dawud (5,274)", fontSize = 11.sp) }
                        )
                    }
                    if (!downloadStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(downloadStatus, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Filter Hadith (e.g. khwab, bukhari, 2261, roza)...") },
                singleLine = true
            )
        }

        items(filtered, key = { it.id }) { hadith ->
            HadithCard(
                hadith = hadith,
                prefs = prefs,
                isBookmarked = prefs.bookmarkedHadiths.contains(hadith.id.toString()),
                onToggleBookmark = { onToggleBookmark(hadith.id) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasailPanel(masail: List<MaslaItem>, prefs: UserPreferences) {
    var selectedFiqh by remember(prefs.defaultFiqh) { mutableStateOf(prefs.defaultFiqh) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Fiqh Chunein (Har Masle ka Jawab Hadith Reference ke Saath):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FiqhSchool.entries.forEach { school ->
                    FilterChip(
                        selected = selectedFiqh == school,
                        onClick = { selectedFiqh = school },
                        label = { Text(school.displayName, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(masail, key = { it.id }) { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = m.topic,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = m.questionFor(prefs.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${selectedFiqh.displayName} Fiqh:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = m.rulingFor(selectedFiqh),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 21.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reference: ${m.hadithReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuasCategorizedPanel(
    duas: List<DuaItem>,
    prefs: UserPreferences,
    onToggleBookmark: (Int) -> Unit
) {
    val categories = remember(duas) { listOf("All") + duas.map { it.category }.distinct() }
    var selectedCategory by remember { mutableStateOf("All") }
    val filtered = remember(duas, selectedCategory) {
        if (selectedCategory == "All") duas else duas.filter { it.category == selectedCategory }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { dua ->
            val isBookmarked = prefs.bookmarkedDuas.contains(dua.id.toString())
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dua.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dua.titleFor(prefs.language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onToggleBookmark(dua.id) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
