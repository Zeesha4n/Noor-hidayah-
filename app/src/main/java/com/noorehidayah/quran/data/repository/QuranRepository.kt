package com.noorehidayah.quran.data.repository

import android.content.Context
import android.net.Uri
import com.noorehidayah.quran.data.local.QuranDatabase
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.SearchCategory
import com.noorehidayah.quran.data.model.SearchResult
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.model.UnifiedSearchResults
import com.noorehidayah.quran.data.search.HinglishSearchEngine
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
        db.insertSurahs(parseSurahs(readAssetText("data/surahs.json")))
        db.insertAyahs(parseAyahs(readAssetText("data/ayahs.json")))
        db.insertTafseers(parseTafseer(readAssetText("data/tafseer.json")))
        db.insertDuas(parseDuas(readAssetText("data/duas.json")))
        db.insertHadiths(parseHadiths(readAssetText("data/hadiths.json")))
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

    suspend fun unifiedOfflineSearch(
        query: String,
        category: SearchCategory,
        allSurahs: List<Surah>
    ): UnifiedSearchResults = withContext(Dispatchers.IO) {
        val surahMap = allSurahs.associateBy { it.surahNumber }
        val intentHint = HinglishSearchEngine.detectIntentHint(query)

        val ayahResults = if (category == SearchCategory.ALL || category == SearchCategory.QURAN) {
            db.searchAyahsOffline(query).map { ayah ->
                SearchResult(
                    ayah = ayah,
                    surah = surahMap[ayah.surahNumber],
                    tafseers = db.getTafseersForAyah(ayah.id)
                )
            }
        } else emptyList()

        val hadithResults = if (category == SearchCategory.ALL || category == SearchCategory.HADITH) {
            db.searchHadithsOffline(query)
        } else emptyList()

        val duaResults = if (category == SearchCategory.ALL || category == SearchCategory.DUAS) {
            db.searchDuasOffline(query)
        } else emptyList()

        UnifiedSearchResults(
            ayahs = ayahResults,
            hadiths = hadithResults,
            duas = duaResults,
            detectedIntent = intentHint
        )
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

    suspend fun getAllHadiths(): List<HadithItem> = withContext(Dispatchers.IO) {
        db.getAllHadiths()
    }

    suspend fun getStats(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        Triple(db.getTotalLoadedAyahs(), db.getTotalLoadedTafseers(), db.getTotalLoadedHadiths())
    }

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
                first.has("collection") && first.has("hadithNumber") -> {
                    val hadiths = parseHadiths(content)
                    db.insertHadiths(hadiths)
                    Result.success("Imported ${hadiths.size} Hadith entries offline!")
                }
                else -> Result.failure(
                    IllegalArgumentException("Unrecognized JSON pack format.")
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
                arabic = o.optString("arabic", ""),
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
                category = o.optString("category", ""),
                titleEnglish = o.optString("titleEnglish", ""),
                titleHinglish = o.optString("titleHinglish", ""),
                titleHindi = o.optString("titleHindi", ""),
                titleArabic = o.optString("titleArabic", ""),
                arabic = o.optString("arabic", ""),
                transliteration = o.optString("transliteration", ""),
                english = o.optString("english", ""),
                hinglish = o.optString("hinglish", ""),
                hindi = o.optString("hindi", ""),
                reference = o.optString("reference", "")
            )
        }
    }

    private fun parseHadiths(json: String): List<HadithItem> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            HadithItem(
                id = o.getInt("id"),
                collection = o.getString("collection"),
                bookNumber = o.optString("bookNumber", ""),
                hadithNumber = o.optString("hadithNumber", ""),
                grading = o.optString("grading", "Sahih"),
                topic = o.optString("topic", ""),
                arabic = o.optString("arabic", ""),
                transliteration = o.optString("transliteration", ""),
                english = o.optString("english", ""),
                hinglish = o.optString("hinglish", ""),
                hindi = o.optString("hindi", ""),
                keywordsHinglish = o.optString("keywordsHinglish", "")
            )
        }
    }
}
