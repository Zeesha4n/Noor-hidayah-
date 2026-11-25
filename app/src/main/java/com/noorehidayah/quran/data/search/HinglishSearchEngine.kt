package com.noorehidayah.quran.data.search

import kotlin.math.min

/**
 * Lightweight (<50 KB) 100% Offline Smart Search Engine:
 * - Rule-based intent detection for natural Hinglish/English/Hindi queries
 * - Phonetic & transliteration normalization
 * - Lightweight suffix stemming
 * - Synonym expansion across Quran, Hadith & Duas
 * - Typo tolerance via bounded Levenshtein edit distance
 */
object HinglishSearchEngine {

    private val stopWords = setOf(
        "ka", "ki", "ke", "ko", "mein", "me", "se", "par", "pe", "hai", "hain", "ho",
        "kya", "kaise", "kab", "kyun", "aur", "ya", "bhi", "toh", "to", "waqt", "time",
        "padhein", "padhe", "batao", "bataye", "matlab", "meaning", "of", "the", "is",
        "in", "for", "to", "a", "an", "and", "what", "how", "when", "why", "dua", "ayat"
    )

    private val synonymGroups: List<List<String>> = listOf(
        listOf("sabr", "sabar", "patience", "bardasht", "himmat", "dhairya"),
        listOf("maafi", "mafi", "tauba", "tawbah", "istighfar", "forgiveness", "gunah", "galti", "kshama"),
        listOf("mushkil", "pareshani", "tension", "stress", "depression", "gham", "hardship", "bojh", "aasani", "ease", "relief", "takleef"),
        listOf("sukoon", "sukun", "peace", "dil", "chain", "shanti"),
        listOf("hifazat", "protection", "safety", "panah", "dar", "khauf", "kursi", "ayatul"),
        listOf("jadu", "jaadu", "magic", "nazar", "hasad", "jalan", "envy", "ruqyah"),
        listOf("waswasa", "shaitan", "overthinking", "anxiety", "uljhan", "jinn"),
        listOf("rizq", "rizk", "rozi", "barkat", "sustenance", "bhalai", "roti"),
        listOf("walidain", "parents", "maa", "baap", "mata", "pita", "mother", "father"),
        listOf("shukr", "shukar", "alhamdulillah", "thanks", "gratitude", "tareef"),
        listOf("tauheed", "tawheed", "allah", "ek", "ikhlas", "oneness"),
        listOf("hidayat", "rasta", "guidance", "seedha", "confusion", "faisla"),
        listOf("madad", "help", "sahara", "tawakkul", "bharosa"),
        listOf("namaz", "salah", "prayer", "ibadat", "worship", "qurbani"),
        listOf("gussa", "anger", "krodh", "control"),
        listOf("safar", "travel", "journey", "musafir"),
        listOf("niyat", "intention", "amal", "irada"),
        listOf("qayamat", "akhirat", "insaaf", "hisab", "judgement"),
        listOf("deen", "mazhab", "religion", "islam")
    )

    private val lookupMap: Map<String, List<String>> by lazy {
        val map = mutableMapOf<String, List<String>>()
        for (group in synonymGroups) {
            for (word in group) {
                map[word] = group
            }
        }
        map
    }

    private val allKnownKeywords: List<String> by lazy {
        synonymGroups.flatten().distinct()
    }

    fun detectIntentHint(rawQuery: String): String? {
        val q = rawQuery.lowercase()
        return when {
            q.contains("mushkil") || q.contains("tension") || q.contains("pareshan") || q.contains("stress") ->
                "Intent: Mushkil & Sukoon ke liye Quran, Hadith aur Duas"
            q.contains("sabr") || q.contains("sabar") || q.contains("patience") ->
                "Intent: Sabr (Patience) par Quran & Sahih Hadith"
            q.contains("maafi") || q.contains("tauba") || q.contains("gunah") ->
                "Intent: Tauba & Istighfar (Forgiveness)"
            q.contains("rizq") || q.contains("rozi") || q.contains("barkat") ->
                "Intent: Halal Rizq & Tawakkul"
            q.contains("maa") || q.contains("baap") || q.contains("walidain") || q.contains("parent") ->
                "Intent: Walidain (Parents) ke Haq aur Dua"
            q.contains("safar") || q.contains("travel") ->
                "Intent: Safar (Travel) ki Sunnah Dua"
            q.contains("hifazat") || q.contains("nazar") || q.contains("dar") || q.contains("kursi") ->
                "Intent: Hifazat & Subah-Shaam Azkar"
            else -> null
        }
    }

    fun expandQueryTokens(rawQuery: String): List<String> {
        val cleaned = rawQuery
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .trim()
        if (cleaned.isEmpty()) return emptyList()

        val allWords = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
        val meaningful = allWords.filterNot { it in stopWords }.ifEmpty { allWords }

        val expanded = linkedSetOf<String>()
        for (token in meaningful) {
            expanded.add(token)
            val stemmed = lightStem(token)
            if (stemmed != token) expanded.add(stemmed)

            val normalized = normalizePhonetic(token)
            if (normalized != token) expanded.add(normalized)

            // Exact synonym match
            lookupMap[token]?.let { expanded.addAll(it) }
            lookupMap[stemmed]?.let { expanded.addAll(it) }
            lookupMap[normalized]?.let { expanded.addAll(it) }

            // Typo-tolerance fallback (edit distance <= 1 or 2)
            if (token.length >= 4 && lookupMap[token] == null) {
                val maxDist = if (token.length >= 6) 2 else 1
                for (kw in allKnownKeywords) {
                    if (absDiff(kw.length, token.length) <= maxDist && editDistance(token, kw) <= maxDist) {
                        expanded.add(kw)
                        lookupMap[kw]?.let { expanded.addAll(it) }
                    }
                }
            }
        }
        return expanded.toList()
    }

    private fun lightStem(token: String): String {
        return when {
            token.length > 5 && token.endsWith("ing") -> token.dropLast(3)
            token.length > 5 && token.endsWith("ness") -> token.dropLast(4)
            token.length > 4 && token.endsWith("es") -> token.dropLast(2)
            token.length > 4 && token.endsWith("s") -> token.dropLast(1)
            else -> token
        }
    }

    private fun normalizePhonetic(token: String): String {
        return token
            .replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("kh", "k")
            .replace("q", "k")
            .replace("z", "j")
            .takeIf { it.length >= 2 } ?: token
    }

    fun buildFtsMatchQuery(tokens: List<String>): String {
        return tokens
            .map { it.replace("\"", "").trim() }
            .filter { it.isNotEmpty() }
            .take(14)
            .joinToString(" OR ") { "${it}*" }
    }

    private fun absDiff(a: Int, b: Int) = if (a > b) a - b else b - a

    private fun editDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
