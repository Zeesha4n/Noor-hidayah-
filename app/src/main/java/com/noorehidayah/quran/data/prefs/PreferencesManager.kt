package com.noorehidayah.quran.data.prefs

import android.content.Context
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import com.noorehidayah.quran.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("noor_hidayah_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    private fun load(): UserPreferences {
        val langCode = prefs.getString("language", AppLanguage.HINGLISH.code)
        val fiqhKey = prefs.getString("fiqh", FiqhSchool.HANAFI.key)
        val methodOrdinal = prefs.getInt("prayer_method", 0).coerceIn(0, PrayerCalcMethod.entries.lastIndex)
        val asrOrdinal = prefs.getInt("asr_method", 0).coerceIn(0, AsrMethod.entries.lastIndex)
        val historyStr = prefs.getString("search_history", "") ?: ""
        val historyList = historyStr.split("||").map { it.trim() }.filter { it.isNotEmpty() }

        return UserPreferences(
            language = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.HINGLISH,
            defaultFiqh = FiqhSchool.entries.firstOrNull { it.key == fiqhKey } ?: FiqhSchool.HANAFI,
            arabicFontSizeSp = prefs.getFloat("arabic_size", 26f),
            translationFontSizeSp = prefs.getFloat("trans_size", 15f),
            darkMode = prefs.getBoolean("dark_mode", false),
            onboardingCompleted = prefs.getBoolean("onboarding_done", false),
            lastReadSurah = prefs.getInt("last_surah", 1),
            lastReadAyah = prefs.getInt("last_ayah", 1),
            bookmarks = prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet(),
            bookmarkedHadiths = prefs.getStringSet("bookmarks_hadith", emptySet())?.toSet() ?: emptySet(),
            bookmarkedDuas = prefs.getStringSet("bookmarks_dua", emptySet())?.toSet() ?: emptySet(),
            searchHistory = historyList,
            tasbihCount = prefs.getInt("tasbih_count", 0),
            tasbihTarget = prefs.getInt("tasbih_target", 33),
            tasbihLabel = prefs.getString("tasbih_label", "SubhanAllah") ?: "SubhanAllah",
            tasbihSoundEnabled = prefs.getBoolean("tasbih_sound", true),
            tasbihDailyTotal = prefs.getInt("tasbih_daily_total", 0),
            selectedCityIndex = prefs.getInt("city_index", 0),
            prayerMethod = PrayerCalcMethod.entries[methodOrdinal],
            asrMethod = AsrMethod.entries[asrOrdinal],
            remindersEnabled = prefs.getBoolean("reminders_enabled", true),
            autoSyncEnabled = prefs.getBoolean("auto_sync", true),
            autoTafseerSynced = prefs.getBoolean("auto_tafseer_synced", false),
            autoHadithSynced = prefs.getBoolean("auto_hadith_synced", false),
            autoLocationEnabled = prefs.getBoolean("auto_location", true),
            autoCityName = prefs.getString("auto_city_name", null),
            autoLat = if (prefs.contains("auto_lat")) prefs.getFloat("auto_lat", 0f).toDouble() else null,
            autoLon = if (prefs.contains("auto_lon")) prefs.getFloat("auto_lon", 0f).toDouble() else null,
            autoTz = if (prefs.contains("auto_tz")) prefs.getFloat("auto_tz", 5.5f).toDouble() else null
        )
    }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString("language", next.language.code)
            .putString("fiqh", next.defaultFiqh.key)
            .putFloat("arabic_size", next.arabicFontSizeSp)
            .putFloat("trans_size", next.translationFontSizeSp)
            .putBoolean("dark_mode", next.darkMode)
            .putBoolean("onboarding_done", next.onboardingCompleted)
            .putInt("last_surah", next.lastReadSurah)
            .putInt("last_ayah", next.lastReadAyah)
            .putStringSet("bookmarks", next.bookmarks)
            .putStringSet("bookmarks_hadith", next.bookmarkedHadiths)
            .putStringSet("bookmarks_dua", next.bookmarkedDuas)
            .putString("search_history", next.searchHistory.take(8).joinToString("||"))
            .putInt("tasbih_count", next.tasbihCount)
            .putInt("tasbih_target", next.tasbihTarget)
            .putString("tasbih_label", next.tasbihLabel)
            .putBoolean("tasbih_sound", next.tasbihSoundEnabled)
            .putInt("tasbih_daily_total", next.tasbihDailyTotal)
            .putInt("city_index", next.selectedCityIndex)
            .putInt("prayer_method", next.prayerMethod.ordinal)
            .putInt("asr_method", next.asrMethod.ordinal)
            .putBoolean("reminders_enabled", next.remindersEnabled)
            .putBoolean("auto_sync", next.autoSyncEnabled)
            .putBoolean("auto_tafseer_synced", next.autoTafseerSynced)
            .putBoolean("auto_hadith_synced", next.autoHadithSynced)
            .putBoolean("auto_location", next.autoLocationEnabled)
            .putString("auto_city_name", next.autoCityName)
            .apply {
                if (next.autoLat != null && next.autoLon != null && next.autoTz != null) {
                    putFloat("auto_lat", next.autoLat.toFloat())
                    putFloat("auto_lon", next.autoLon.toFloat())
                    putFloat("auto_tz", next.autoTz.toFloat())
                }
            }
            .apply()
    }
}
