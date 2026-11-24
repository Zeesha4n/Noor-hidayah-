package com.noorehidayah.quran.data.search

/**
 * Offline Hinglish phonetic normalization & synonym expander.
 * Handles common spelling variations users type in Hinglish (e.g., sabar/sabr, mafi/maafi,
 * rizk/rizq, tension/pareshani/mushkil, nazar/hasad, maa baap/walidain).
 */
object HinglishSearchEngine {

    private val synonymGroups: List<List<String>> = listOf(
        listOf("sabr", "sabar", "patience", "bardasht", "himmat"),
        listOf("maafi", "mafi", "tauba", "tawbah", "istighfar", "forgiveness", "gunah", "galti"),
        listOf("mushkil", "pareshani", "tension", "stress", "depression", "gham", "hardship", "bojh", "aasani", "ease", "relief"),
        listOf("sukoon", "sukun", "peace", "dil", "chain", "shanti"),
        listOf("hifazat", "protection", "safety", "panah", "dar", "khauf", "kursi", "ayatul"),
        listOf("jadu", "jaadu", "magic", "nazar", "hasad", "jalan", "envy", "ruqyah"),
        listOf("waswasa", "shaitan", "overthinking", "anxiety", "uljhan", "jinn"),
        listOf("rizq", "rizk", "rozi", "barkat", "sustenance", "bhalai"),
        listOf("walidain", "parents", "maa", "baap", "mata", "pita"),
        listOf("shukr", "shukar", "alhamdulillah", "thanks", "gratitude", "tareef"),
        listOf("tauheed", "tawheed", "allah", "ek", "ikhlas", "oneness"),
        listOf("hidayat", "rasta", "guidance", "seedha", "confusion", "faisla"),
        listOf("madad", "help", "sahara", "tawakkul", "bharosa", "dua"),
        listOf("namaz", "salah", "prayer", "ibadat", "worship", "qurbani"),
        listOf("waqt", "time", "zamana", "asr", "nuqsan", "kamyabi", "success"),
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

    /**
     * Cleans raw user input and returns expanded search tokens safe for SQLite FTS/LIKE queries.
     */
    fun expandQueryTokens(rawQuery: String): List<String> {
        val cleaned = rawQuery
            .lowercase()
            .replace(Regex("[^\p{L}\p{N}\s]"), " ")
            .trim()
        if (cleaned.isEmpty()) return emptyList()

        val rawTokens = cleaned.split(Regex("\s+")).filter { it.length >= 2 }
        if (rawTokens.isEmpty()) return listOf(cleaned)

        val expanded = linkedSetOf<String>()
        for (token in rawTokens) {
            expanded.add(token)
            val normalized = normalizePhonetic(token)
            if (normalized != token) expanded.add(normalized)
            lookupMap[token]?.let { expanded.addAll(it) }
            lookupMap[normalized]?.let { expanded.addAll(it) }
        }
        return expanded.toList()
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

    /**
     * Builds an FTS4 MATCH expression combining tokens safely with OR + prefix matching.
     */
    fun buildFtsMatchQuery(tokens: List<String>): String {
        return tokens
            .map { it.replace(""", "").trim() }
            .filter { it.isNotEmpty() }
            .take(12)
            .joinToString(" OR ") { "${it}*" }
    }
}
