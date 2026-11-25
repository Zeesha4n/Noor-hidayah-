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
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import com.noorehidayah.quran.data.model.SearchCategory
import com.noorehidayah.quran.data.model.SearchResult
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.model.UnifiedSearchResults
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
    val searchCategory: SearchCategory = SearchCategory.ALL,
    val searchResults: UnifiedSearchResults = UnifiedSearchResults(),
    val selectedSurah: Surah? = null,
    val selectedSurahAyahs: List<Pair<Ayah, List<TafseerEntry>>> = emptyList(),
    val bookmarkedResults: List<SearchResult> = emptyList(),
    val duas: List<DuaItem> = emptyList(),
    val hadiths: List<HadithItem> = emptyList(),
    val loadedAyahCount: Int = 0,
    val loadedTafseerCount: Int = 0,
    val loadedHadithCount: Int = 0,
    val importStatusMessage: String? = null,
    val startupError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repo = QuranRepository(application)
    private val repository get() = repo
    private val prefsManager = PreferencesManager(application)

    val preferences: StateFlow<UserPreferences> = prefsManager.state

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.initializeOfflineData()
                refreshData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReady = true,
                    startupError = e.stackTraceToString().take(600)
                )
            }
        }
    }

    private suspend fun refreshData() {
        val surahs = repository.getAllSurahs()
        val duas = repository.getAllDuas()
        val hadiths = repository.getAllHadiths()
        val (ayahCount, tafseerCount, hadithCount) = repository.getStats()
        val bookmarks = repository.getBookmarkedAyahs(preferences.value.bookmarks, surahs)
        _uiState.value = _uiState.value.copy(
            isReady = true,
            surahs = surahs,
            duas = duas,
            hadiths = hadiths,
            bookmarkedResults = bookmarks,
            loadedAyahCount = ayahCount,
            loadedTafseerCount = tafseerCount,
            loadedHadithCount = hadithCount
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = UnifiedSearchResults())
            return
        }
        searchJob = viewModelScope.launch {
            delay(120)
            val results = repository.unifiedOfflineSearch(
                query = query,
                category = _uiState.value.searchCategory,
                allSurahs = _uiState.value.surahs
            )
            _uiState.value = _uiState.value.copy(searchResults = results)
            if (query.trim().length >= 3) {
                prefsManager.update { current ->
                    val updated = (listOf(query.trim()) + current.searchHistory).distinct().take(6)
                    current.copy(searchHistory = updated)
                }
            }
        }
    }

    fun onSearchCategoryChange(category: SearchCategory) {
        _uiState.value = _uiState.value.copy(searchCategory = category)
        val q = _uiState.value.searchQuery
        if (q.isNotBlank()) {
            viewModelScope.launch {
                val results = repository.unifiedOfflineSearch(q, category, _uiState.value.surahs)
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
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

    fun toggleHadithBookmark(id: Int) {
        val key = id.toString()
        prefsManager.update { current ->
            val updated = current.bookmarkedHadiths.toMutableSet()
            if (!updated.add(key)) updated.remove(key)
            current.copy(bookmarkedHadiths = updated)
        }
    }

    fun toggleDuaBookmark(id: Int) {
        val key = id.toString()
        prefsManager.update { current ->
            val updated = current.bookmarkedDuas.toMutableSet()
            if (!updated.add(key)) updated.remove(key)
            current.copy(bookmarkedDuas = updated)
        }
    }

    fun completeOnboarding() = prefsManager.update { it.copy(onboardingCompleted = true) }
    fun setLanguage(lang: AppLanguage) = prefsManager.update { it.copy(language = lang) }
    fun setDefaultFiqh(fiqh: FiqhSchool) = prefsManager.update { it.copy(defaultFiqh = fiqh) }
    fun setArabicFontSize(size: Float) = prefsManager.update { it.copy(arabicFontSizeSp = size) }
    fun setDarkMode(enabled: Boolean) = prefsManager.update { it.copy(darkMode = enabled) }
    fun incrementTasbih() = prefsManager.update {
        it.copy(tasbihCount = it.tasbihCount + 1, tasbihDailyTotal = it.tasbihDailyTotal + 1)
    }
    fun resetTasbih() = prefsManager.update { it.copy(tasbihCount = 0) }
    fun toggleTasbihSound(enabled: Boolean) = prefsManager.update { it.copy(tasbihSoundEnabled = enabled) }
    fun setTasbihPreset(label: String, target: Int) =
        prefsManager.update { it.copy(tasbihLabel = label, tasbihTarget = target, tasbihCount = 0) }
    fun setCityIndex(idx: Int) = prefsManager.update { it.copy(selectedCityIndex = idx) }
    fun setPrayerMethod(m: PrayerCalcMethod) = prefsManager.update { it.copy(prayerMethod = m) }
    fun setAsrMethod(m: AsrMethod) = prefsManager.update { it.copy(asrMethod = m) }
    fun setRemindersEnabled(enabled: Boolean) = prefsManager.update { it.copy(remindersEnabled = enabled) }

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
}
