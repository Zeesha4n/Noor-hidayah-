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

        return UserPreferences(
            language = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.HINGLISH,
            defaultFiqh = FiqhSchool.entries.firstOrNull { it.key == fiqhKey } ?: FiqhSchool.HANAFI,
            arabicFontSizeSp = prefs.getFloat("arabic_size", 26f),
            translationFontSizeSp = prefs.getFloat("trans_size", 15f),
            darkMode = prefs.getBoolean("dark_mode", false),
            lastReadSurah = prefs.getInt("last_surah", 1),
            lastReadAyah = prefs.getInt("last_ayah", 1),
            bookmarks = prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet(),
            tasbihCount = prefs.getInt("tasbih_count", 0),
            tasbihTarget = prefs.getInt("tasbih_target", 33),
            tasbihLabel = prefs.getString("tasbih_label", "SubhanAllah") ?: "SubhanAllah",
            selectedCityIndex = prefs.getInt("city_index", 0),
            prayerMethod = PrayerCalcMethod.entries[methodOrdinal],
            asrMethod = AsrMethod.entries[asrOrdinal]
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
            .putInt("last_surah", next.lastReadSurah)
            .putInt("last_ayah", next.lastReadAyah)
            .putStringSet("bookmarks", next.bookmarks)
            .putInt("tasbih_count", next.tasbihCount)
            .putInt("tasbih_target", next.tasbihTarget)
            .putString("tasbih_label", next.tasbihLabel)
            .putInt("city_index", next.selectedCityIndex)
            .putInt("prayer_method", next.prayerMethod.ordinal)
            .putInt("asr_method", next.asrMethod.ordinal)
            .apply()
    }
}
