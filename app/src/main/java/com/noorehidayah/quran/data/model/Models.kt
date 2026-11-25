package com.noorehidayah.quran.data.model

enum class AppLanguage(val code: String, val displayName: String, val shortLabel: String) {
    HINGLISH("hinglish", "Hinglish", "Hinglish"),
    HINDI("hindi", "हिन्दी", "हिन्दी"),
    ENGLISH("english", "English", "EN"),
    ARABIC("arabic", "العربية", "عربي")
}

enum class FiqhSchool(val key: String, val displayName: String) {
    HANAFI("Hanafi", "Hanafi"),
    SHAFII("Shafi'i", "Shafi'i"),
    MALIKI("Maliki", "Maliki"),
    HANBALI("Hanbali", "Hanbali"),
    JAFARI("Ja'fari", "Ja'fari"),
    AHLE_HADITH("Ahl-e-Hadith", "Ahl-e-Hadith")
}

enum class PrayerCalcMethod(val displayName: String, val fajrAngle: Double, val ishaAngle: Double) {
    KARACHI("Karachi (India/Pak)", 18.0, 18.0),
    MWL("Muslim World League", 18.0, 17.0),
    ISNA("ISNA (North America)", 15.0, 15.0),
    UMM_AL_QURA("Umm al-Qura (Makkah)", 18.5, 18.5)
}

enum class AsrMethod(val displayName: String, val shadowFactor: Double) {
    HANAFI("Hanafi (2x shadow)", 2.0),
    SHAFII("Shafi'i / Standard (1x)", 1.0)
}

enum class SearchCategory(val label: String) {
    ALL("All"),
    QURAN("Quran"),
    HADITH("Hadith"),
    DUAS("Duas & Azkar")
}

data class Surah(
    val surahNumber: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameHinglish: String,
    val nameHindi: String,
    val ayahCount: Int,
    val revelationType: String,
    val loadedAyahCount: Int = 0
)

data class Ayah(
    val id: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val hindi: String,
    val hinglish: String,
    val keywordsHinglish: String
) {
    fun translationFor(language: AppLanguage): String = when (language) {
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.HINDI -> hindi
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> transliteration
    }
}

data class TafseerEntry(
    val ayahId: String,
    val fiqh: String,
    val sourceName: String,
    val english: String,
    val hinglish: String,
    val hindi: String
) {
    fun textFor(language: AppLanguage): String = when (language) {
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.HINDI -> hindi
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> english
    }
}

data class SearchResult(
    val ayah: Ayah,
    val surah: Surah?,
    val tafseers: List<TafseerEntry>
)

data class HadithItem(
    val id: Int,
    val collection: String,
    val bookNumber: String,
    val hadithNumber: String,
    val grading: String,
    val topic: String,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val hinglish: String,
    val hindi: String,
    val keywordsHinglish: String
) {
    fun translationFor(language: AppLanguage): String = when (language) {
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.HINDI -> hindi
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> transliteration
    }
}

data class DuaItem(
    val id: Int,
    val category: String,
    val titleEnglish: String,
    val titleHinglish: String,
    val titleHindi: String,
    val titleArabic: String,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val hinglish: String,
    val hindi: String,
    val reference: String
) {
    fun titleFor(language: AppLanguage): String = when (language) {
        AppLanguage.HINGLISH -> titleHinglish
        AppLanguage.HINDI -> titleHindi
        AppLanguage.ENGLISH -> titleEnglish
        AppLanguage.ARABIC -> titleArabic
    }

    fun translationFor(language: AppLanguage): String = when (language) {
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.HINDI -> hindi
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> transliteration
    }
}

data class UnifiedSearchResults(
    val ayahs: List<SearchResult> = emptyList(),
    val hadiths: List<HadithItem> = emptyList(),
    val duas: List<DuaItem> = emptyList(),
    val detectedIntent: String? = null
)

data class CityLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double
)

data class UserPreferences(
    val language: AppLanguage = AppLanguage.HINGLISH,
    val defaultFiqh: FiqhSchool = FiqhSchool.HANAFI,
    val arabicFontSizeSp: Float = 26f,
    val translationFontSizeSp: Float = 15f,
    val darkMode: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val bookmarks: Set<String> = emptySet(),
    val bookmarkedHadiths: Set<String> = emptySet(),
    val bookmarkedDuas: Set<String> = emptySet(),
    val searchHistory: List<String> = emptyList(),
    val tasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val tasbihLabel: String = "SubhanAllah",
    val tasbihSoundEnabled: Boolean = true,
    val tasbihDailyTotal: Int = 0,
    val selectedCityIndex: Int = 0,
    val prayerMethod: PrayerCalcMethod = PrayerCalcMethod.KARACHI,
    val asrMethod: AsrMethod = AsrMethod.HANAFI,
    val remindersEnabled: Boolean = false
)
