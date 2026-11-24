package com.noorehidayah.quran.data.repository

import android.content.Context
import android.net.Uri
import com.noorehidayah.quran.data.local.QuranDatabase
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.SearchResult
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class QuranRepository(private val context: Context) {

    private val db = QuranDatabase(context)

    suspend fun initializeOfflineData() = withContext(Dispatchers.IO) {
        if (!db.isSeeded()) {
            seedFromAssets()
        }
    }

    private fun seedFromAssets() {
        val surahsJson = readAssetText("data/surahs.json")
        db.insertSurahs(parseSurahs(surahsJson))

        val ayahsJson = readAssetText("data/ayahs.json")
        db.insertAyahs(parseAyahs(ayahsJson))

        val tafseerJson = readAssetText("data/tafseer.json")
        db.insertTafseers(parseTafseer(tafseerJson))

        val duasJson = readAssetText("data/duas.json")
        db.insertDuas(parseDuas(duasJson))
    }

    private fun readAssetText(path: String): String {
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    suspend fun getAllSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        db.getAllSurahs()
    }

    suspend fun getAyahsWithTafseer(surahNumber: Int): List<Pair<Ayah, List<TafseerEntry>>> =
        withContext(Dispatchers.IO) {
            val ayahs = db.getAyahsBySurah(surahNumber)
            ayahs.map { ayah -> ayah to db.getTafseersForAyah(ayah.id) }
        }

    suspend fun searchByMeaning(query: String, allSurahs: List<Surah>): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val surahMap = allSurahs.associateBy { it.surahNumber }
            val matchedAyahs = db.searchAyahsOffline(query)
            matchedAyahs.map { ayah ->
                SearchResult(
                    ayah = ayah,
                    surah = surahMap[ayah.surahNumber],
                    tafseers = db.getTafseersForAyah(ayah.id)
                )
            }
        }

    suspend fun getBookmarkedAyahs(ids: Set<String>, allSurahs: List<Surah>): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val surahMap = allSurahs.associateBy { it.surahNumber }
            db.getAyahsByIds(ids).map { ayah ->
                SearchResult(
                    ayah = ayah,
                    surah = surahMap[ayah.surahNumber],
                    tafseers = db.getTafseersForAyah(ayah.id)
                )
            }
        }

    suspend fun getAllDuas(): List<DuaItem> = withContext(Dispatchers.IO) {
        db.getAllDuas()
    }

    suspend fun getStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        db.getTotalLoadedAyahs() to db.getTotalLoadedTafseers()
    }

    /**
     * Imports a user-selected JSON file (Full Quran ayahs pack OR Multi-Fiqh Tafseer pack)
     * directly into the offline SQLite database + FTS index.
     */
    suspend fun importOfflineJsonPack(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Could not read selected file."))

            val arr = JSONArray(content)
            if (arr.length() == 0) {
                return@withContext Result.failure(IllegalArgumentException("JSON file is empty."))
            }

            val first = arr.getJSONObject(0)
            return@withContext when {
                first.has("arabic") && first.has("surahNumber") -> {
                    val ayahs = parseAyahs(content)
                    db.insertAyahs(ayahs)
                    Result.success("Imported ${ayahs.size} Quran ayahs into offline search!")
                }
                first.has("fiqh") && first.has("ayahId") -> {
                    val entries = parseTafseer(content)
                    db.insertTafseers(entries)
                    Result.success("Imported ${entries.size} Multi-Fiqh Tafseer entries offline!")
                }
                else -> Result.failure(
                    IllegalArgumentException("Unrecognized JSON pack format. Expected Quran ayahs or Tafseer entries.")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSurahs(json: String): List<Surah> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Surah(
                surahNumber = o.getInt("surahNumber"),
                nameArabic = o.getString("nameArabic"),
                nameEnglish = o.getString("nameEnglish"),
                nameHinglish = o.getString("nameHinglish"),
                nameHindi = o.getString("nameHindi"),
                ayahCount = o.getInt("ayahCount"),
                revelationType = o.getString("revelationType")
            )
        }
    }

    private fun parseAyahs(json: String): List<Ayah> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Ayah(
                id = o.getString("id"),
                surahNumber = o.getInt("surahNumber"),
                ayahNumber = o.getInt("ayahNumber"),
                arabic = o.getString("arabic"),
                transliteration = o.optString("transliteration", ""),
                english = o.optString("english", ""),
                hindi = o.optString("hindi", ""),
                hinglish = o.optString("hinglish", ""),
                keywordsHinglish = o.optString("keywordsHinglish", "")
            )
        }
    }

    private fun parseTafseer(json: String): List<TafseerEntry> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TafseerEntry(
                ayahId = o.getString("ayahId"),
                fiqh = o.getString("fiqh"),
                sourceName = o.optString("sourceName", "Classical Tafseer"),
                english = o.optString("english", ""),
                hinglish = o.optString("hinglish", ""),
                hindi = o.optString("hindi", "")
            )
        }
    }

    private fun parseDuas(json: String): List<DuaItem> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            DuaItem(
                id = o.getInt("id"),
                category = o.getString("category"),
                titleEnglish = o.getString("titleEnglish"),
                titleHinglish = o.getString("titleHinglish"),
                titleHindi = o.getString("titleHindi"),
                titleArabic = o.getString("titleArabic"),
                arabic = o.getString("arabic"),
                transliteration = o.getString("transliteration"),
                english = o.getString("english"),
                hinglish = o.getString("hinglish"),
                hindi = o.getString("hindi"),
                reference = o.getString("reference")
            )
        }
    }
}
