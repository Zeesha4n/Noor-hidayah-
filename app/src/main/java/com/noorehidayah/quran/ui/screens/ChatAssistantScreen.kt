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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.SearchCategory
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.data.repository.QuranRepository
import com.noorehidayah.quran.utils.PrayerTimeCalculator
import com.noorehidayah.quran.utils.QiblaCalculator
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatAssistantScreen(
    repository: QuranRepository,
    surahs: List<Surah>,
    prefs: UserPreferences,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var onlineBoost by remember { mutableStateOf(true) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = 1L,
                isUser = false,
                text = "Assalamu Alaikum! Main Noor Guide hoon (Offline + Optional Online Boost).\n\nAap Hinglish, Hindi ya English mein kuch bhi pooch sakte hain—jaise:\n• \"Bura khwab dekhna kaisa hai\"\n• \"Khana khane ki dua\"\n• \"Rafa-ul-Yadain / Ameen\"\n• \"Sabr par Quran ki ayat\"\n• \"Aaj namaz ka waqt kya hai?\""
            )
        )
    }

    val quickPrompts = listOf(
        "Bura khwab / Sapna",
        "Khana khane ki dua",
        "Rafa-ul-Yadain",
        "Namaz ka waqt",
        "Sabr par ayat",
        "Qibla direction"
    )

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        messages.add(ChatMessage(System.currentTimeMillis(), isUser = true, text = trimmed))
        inputText = ""

        scope.launch {
            val reply = buildHybridReply(trimmed, onlineBoost, repository, surahs, prefs)
            messages.add(ChatMessage(System.currentTimeMillis() + 1, isUser = false, text = reply))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Noor Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Offline Quran/Masail + Online Boost", style = MaterialTheme.typography.labelSmall)
                }
                FilterChip(
                    selected = onlineBoost,
                    onClick = { onlineBoost = !onlineBoost },
                    label = { Text(if (onlineBoost) "Online Boost: ON" else "Offline Only", fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickPrompts.forEach { p ->
                        AssistChip(onClick = { sendMessage(p) }, label = { Text(p, fontSize = 12.sp) })
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 315.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Apna sawaal Hinglish mein likhein...") },
                singleLine = true
            )
            IconButton(onClick = { sendMessage(inputText) }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private suspend fun buildHybridReply(
    query: String,
    onlineBoost: Boolean,
    repository: QuranRepository,
    surahs: List<Surah>,
    prefs: UserPreferences
): String {
    val q = query.lowercase()
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]

    if (q.contains("namaz") && (q.contains("waqt") || q.contains("time")) || q.contains("azan") || q.contains("sehri") || q.contains("iftar")) {
        val t = PrayerTimeCalculator.calculate(city, prefs.prayerMethod, prefs.asrMethod)
        return "Aaj ${city.name} (${prefs.asrMethod.displayName}) ke Namaz Waqt:\n\n" +
            "• Fajr (Sehri): ${t.fajr}\n" +
            "• Sunrise: ${t.sunrise}\n" +
            "• Dhuhr: ${t.dhuhr}\n" +
            "• Asr: ${t.asr}\n" +
            "• Maghrib (Iftar): ${t.maghrib}\n" +
            "• Isha: ${t.isha}\n\n" +
            "Agli Namaz: ${t.nextPrayerName} (${t.nextPrayerTime} • ${t.nextPrayerCountdown})"
    }

    if (q.contains("qibla") || q.contains("kaaba") || q.contains("direction")) {
        val deg = QiblaCalculator.qiblaBearingDegrees(city.latitude, city.longitude)
        val dir = QiblaCalculator.cardinalDirection(deg)
        val km = QiblaCalculator.distanceToMakkahKm(city.latitude, city.longitude)
        return "${city.name} se Qibla ki disha: ${deg.toInt()}° $dir (Makkah se faasla: $km km)."
    }

    val results = repository.unifiedOfflineSearch(query, SearchCategory.ALL, surahs)
    val sb = StringBuilder()
    results.detectedIntent?.let { sb.append("$it\n\n") }

    if (results.masail.isNotEmpty()) {
        val m = results.masail.first()
        sb.append("⚖️ Masla (${m.topic} • ${prefs.defaultFiqh.displayName} Fiqh):\n")
        sb.append("${m.rulingFor(prefs.defaultFiqh)}\n(Ref: ${m.hadithReference})\n\n")
    }

    if (results.hadiths.isNotEmpty()) {
        val h = results.hadiths.first()
        sb.append("📜 [${h.grading}] ${h.collection} #${h.hadithNumber}:\n")
        sb.append("${h.translationFor(prefs.language)}\n\n")
    }

    if (results.ayahs.isNotEmpty()) {
        val top = results.ayahs.first()
        sb.append("📖 Quran (${top.surah?.nameEnglish ?: ""} ${top.ayah.id}):\n")
        sb.append("${top.ayah.arabic}\n${top.ayah.translationFor(prefs.language)}\n")
        val fiqhTafseer = top.tafseers.firstOrNull { it.fiqh.equals(prefs.defaultFiqh.key, ignoreCase = true) }
        if (fiqhTafseer != null) {
            sb.append("📚 ${fiqhTafseer.fiqh} Tafseer: ${fiqhTafseer.textFor(prefs.language)}\n")
        }
        sb.append("\n")
    }

    if (results.duas.isNotEmpty()) {
        val d = results.duas.first()
        sb.append("🤲 Dua (${d.titleFor(prefs.language)}):\n")
        sb.append("${d.arabic}\n${d.translationFor(prefs.language)}\n(${d.reference})\n")
    }

    // Online Boost fallback when internet is available and offline found nothing
    if (sb.isEmpty() && onlineBoost) {
        val onlineMatches = repository.searchQuranOnline(query)
        if (onlineMatches.isNotEmpty()) {
            sb.append("🌐 Online Verified Quran Search:\n\n")
            onlineMatches.forEach { sb.append("$it\n\n") }
        }
    }

    return if (sb.isEmpty()) {
        "Offline database mein exact match nahi mila. Simple lafz try karein (jaise: khwab, sapna, sabr, mushkil, khana, rafa, ameen, maafi, walidain, rizq) — ya full Hadith books download karne ke liye Hadith tab kholein."
    } else {
        sb.toString().trim()
    }
}
