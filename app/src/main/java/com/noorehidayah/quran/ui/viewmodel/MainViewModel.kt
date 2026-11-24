package com.noorehidayah.quran.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noorehidayah.quran.data.model.AppLanguage
import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.FiqhSchool
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import com.noorehidayah.quran.data.model.SearchResult
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.data.prefs.PreferencesManager
import com.noorehidayah.quran.data.repository.QuranRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val isReady: Boolean = false,
    val surahs: List<Surah> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedSurah: Surah? = null,
    val selectedSurahAyahs: List<Pair<Ayah, List<TafseerEntry>>> = emptyList(),
    val bookmarkedResults: List<SearchResult> = emptyList(),
    val duas: List<DuaItem> = emptyList(),
    val loadedAyahCount: Int = 0,
    val loadedTafseerCount: Int = 0,
    val importStatusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val prefsManager = PreferencesManager(application)

    val preferences: StateFlow<UserPreferences> = prefsManager.state

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeOfflineData()
            refreshData()
        }
    }

    private suspend fun refreshData() {
        val surahs = repository.getAllSurahs()
        val duas = repository.getAllDuas()
        val (ayahCount, tafseerCount) = repository.getStats()
        val bookmarks = repository.getBookmarkedAyahs(preferences.value.bookmarks, surahs)
        _uiState.value = _uiState.value.copy(
            isReady = true,
            surahs = surahs,
            duas = duas,
            bookmarkedResults = bookmarks,
            loadedAyahCount = ayahCount,
            loadedTafseerCount = tafseerCount
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(120)
            val results = repository.searchByMeaning(query, _uiState.value.surahs)
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }

    fun openSurah(surahNumber: Int) {
        val surah = _uiState.value.surahs.firstOrNull { it.surahNumber == surahNumber } ?: return
        prefsManager.update { it.copy(lastReadSurah = surahNumber, lastReadAyah = 1) }
        viewModelScope.launch {
            val ayahs = repository.getAyahsWithTafseer(surahNumber)
            _uiState.value = _uiState.value.copy(
                selectedSurah = surah,
                selectedSurahAyahs = ayahs
            )
        }
    }

    fun closeSurahReader() {
        _uiState.value = _uiState.value.copy(selectedSurah = null, selectedSurahAyahs = emptyList())
    }

    fun toggleBookmark(ayahId: String) {
        prefsManager.update { current ->
            val updated = current.bookmarks.toMutableSet()
            if (!updated.add(ayahId)) updated.remove(ayahId)
            current.copy(bookmarks = updated)
        }
        viewModelScope.launch {
            val bookmarks = repository.getBookmarkedAyahs(preferences.value.bookmarks, _uiState.value.surahs)
            _uiState.value = _uiState.value.copy(bookmarkedResults = bookmarks)
        }
    }

    fun setLanguage(lang: AppLanguage) = prefsManager.update { it.copy(language = lang) }
    fun setDefaultFiqh(fiqh: FiqhSchool) = prefsManager.update { it.copy(defaultFiqh = fiqh) }
    fun setArabicFontSize(size: Float) = prefsManager.update { it.copy(arabicFontSizeSp = size) }
    fun setDarkMode(enabled: Boolean) = prefsManager.update { it.copy(darkMode = enabled) }
    fun incrementTasbih() = prefsManager.update { it.copy(tasbihCount = it.tasbihCount + 1) }
    fun resetTasbih() = prefsManager.update { it.copy(tasbihCount = 0) }
    fun setTasbihPreset(label: String, target: Int) =
        prefsManager.update { it.copy(tasbihLabel = label, tasbihTarget = target, tasbihCount = 0) }
    fun setCityIndex(idx: Int) = prefsManager.update { it.copy(selectedCityIndex = idx) }
    fun setPrayerMethod(m: PrayerCalcMethod) = prefsManager.update { it.copy(prayerMethod = m) }
    fun setAsrMethod(m: AsrMethod) = prefsManager.update { it.copy(asrMethod = m) }

    fun importPack(uri: Uri) {
        viewModelScope.launch {
            val result = repository.importOfflineJsonPack(uri)
            val msg = result.fold(
                onSuccess = { it },
                onFailure = { "Import failed: ${it.localizedMessage}" }
            )
            refreshData()
            _uiState.value = _uiState.value.copy(importStatusMessage = msg)
        }
    }

    fun clearImportStatus() {
        _uiState.value = _uiState.value.copy(importStatusMessage = null)
    }
}
