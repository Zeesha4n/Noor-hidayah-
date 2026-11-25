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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.UserPreferences

@Composable
fun HadithAndDuasScreen(
    hadiths: List<HadithItem>,
    duas: List<DuaItem>,
    prefs: UserPreferences,
    onToggleHadithBookmark: (Int) -> Unit,
    onToggleDuaBookmark: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hadith Library", "Duas & Azkar")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> HadithListPanel(hadiths, prefs, onToggleHadithBookmark)
            1 -> DuasCategorizedPanel(duas, prefs, onToggleDuaBookmark)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${hadith.collection} #${hadith.hadithNumber}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = hadith.grading,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = {
                        val shareText = "${hadith.arabic}\n\n${hadith.translationFor(prefs.language)}\n\n— ${hadith.collection} (${hadith.bookNumber}, #${hadith.hadithNumber}) [${hadith.grading}]"
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = hadith.arabic,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = (prefs.arabicFontSizeSp - 2f).coerceAtLeast(20f).sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Right
                )
            }

            if (hadith.transliteration.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hadith.transliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun HadithListPanel(
    hadiths: List<HadithItem>,
    prefs: UserPreferences,
    onToggleBookmark: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(hadiths, key = { it.id }) { hadith ->
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
