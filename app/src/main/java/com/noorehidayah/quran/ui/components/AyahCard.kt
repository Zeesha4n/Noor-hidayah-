package com.noorehidayah.quran.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.utils.QuranAudioPlayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AyahCard(
    ayah: Ayah,
    surah: Surah?,
    tafseers: List<TafseerEntry>,
    globalLanguage: AppLanguage,
    defaultFiqh: FiqhSchool,
    arabicFontSizeSp: Float,
    isBookmarked: Boolean,
    strings: Strings,
    onToggleBookmark: () -> Unit,
    onOpenSurah: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioState by QuranAudioPlayer.state.collectAsState()
    val isThisPlaying = audioState.activeAyahId == ayah.id && audioState.isPlaying
    val isThisDownloading = audioState.activeAyahId == ayah.id && audioState.isDownloading

    LaunchedEffect(audioState.errorMessage) {
        val msg = audioState.errorMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var cardLang by remember(globalLanguage) { mutableStateOf(globalLanguage) }
    var showTafseer by remember { mutableStateOf(false) }
    var selectedFiqh by remember(defaultFiqh) { mutableStateOf(defaultFiqh) }

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
                    val badgeText = if (surah != null) {
                        "${surah.nameEnglish} • ${ayah.id}"
                    } else {
                        "Ayah ${ayah.id}"
                    }
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        scope.launch {
                            QuranAudioPlayer.togglePlayAyah(
                                context = context,
                                ayahId = ayah.id,
                                surahNumber = ayah.surahNumber,
                                ayahNumber = ayah.ayahNumber
                            )
                        }
                    }) {
                        if (isThisDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Tilawat",
                                tint = if (isThisPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = {
                        val copyText = "${ayah.arabic}\n\n${ayah.translationFor(cardLang)}\n(${surah?.nameEnglish ?: ""} ${ayah.id})"
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Ayah", copyText))
                        Toast.makeText(context, strings.copiedToast, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            Spacer(modifier = Modifier.height(10.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = ayah.arabic,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = arabicFontSizeSp.sp,
                    lineHeight = (arabicFontSizeSp * 1.7f).sp,
                    textAlign = TextAlign.Right,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ayah.transliteration.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(AppLanguage.HINGLISH, AppLanguage.HINDI, AppLanguage.ENGLISH).forEach { lang ->
                    FilterChip(
                        selected = cardLang == lang,
                        onClick = { cardLang = lang },
                        label = { Text(lang.shortLabel, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ayah.translationFor(cardLang),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showTafseer = !showTafseer }) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showTafseer) strings.hideTafseer else strings.tafseerButton)
                }
                if (onOpenSurah != null) {
                    TextButton(onClick = onOpenSurah) {
                        Text(strings.openSurah)
                    }
                }
            }

            AnimatedVisibility(visible = showTafseer) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FiqhSchool.entries.forEach { school ->
                            FilterChip(
                                selected = selectedFiqh == school,
                                onClick = { selectedFiqh = school },
                                label = { Text(school.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val entry = tafseers.firstOrNull {
                        it.fiqh.equals(selectedFiqh.key, ignoreCase = true)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (entry != null) {
                                Text(
                                    text = "${selectedFiqh.displayName} • ${entry.sourceName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.textFor(cardLang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = strings.noTafseerYet,
                                    style = MaterialTheme.typography.bodySmall,
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
