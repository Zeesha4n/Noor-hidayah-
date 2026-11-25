package com.noorehidayah.quran.data.repository

import android.content.Context
import android.net.Uri
import com.noorehidayah.quran.data.local.QuranDatabase
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.MaslaItem
import com.noorehidayah.quran.data.model.SearchCategory
import com.noorehidayah.quran.data.model.SearchResult
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.model.UnifiedSearchResults
import com.noorehidayah.quran.data.search.HinglishSearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
        db.insertMasail(parseMasail(readAssetText("data/masail.json")))
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

        val masailResults = if (category == SearchCategory.ALL) {
            db.searchMasailOffline(query)
        } else emptyList()

        UnifiedSearchResults(
            ayahs = ayahResults,
            hadiths = hadithResults,
            duas = duaResults,
            masail = masailResults,
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

    suspend fun getAllDuas(): List<DuaItem> = withContext(Dispatchers.IO) { db.getAllDuas() }
    suspend fun getAllHadiths(): List<HadithItem> = withContext(Dispatchers.IO) { db.getAllHadiths() }
    suspend fun getAllMasail(): List<MaslaItem> = withContext(Dispatchers.IO) { db.getAllMasail() }

    suspend fun getStats(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        Triple(db.getTotalLoadedAyahs(), db.getTotalLoadedTafseers(), db.getTotalLoadedHadiths())
    }

    /**
     * Downloads a full authentic Hadith collection from fawazahmed0/hadith-api CDN
     * and stores it in SQLite + FTS4 for 100% offline search afterwards.
     */
    suspend fun downloadHadithCollection(
        editionId: String,
        collectionName: String,
        idOffset: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/$editionId.min.json"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
            }
            if (conn.responseCode != 200) {
                return@withContext Result.failure(IllegalStateException("HTTP ${conn.responseCode}"))
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rootObj = JSONObject(body)
            val arr = rootObj.getJSONArray("hadiths")
            val list = ArrayList<HadithItem>(arr.length())

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val hNum = o.optString("hadithnumber", (i + 1).toString())
                val text = o.optString("text", "").trim()
                if (text.isEmpty()) continue

                var gradeText = if (collectionName.contains("Sahih", ignoreCase = true)) {
                    "Sahih (Authentic)"
                } else {
                    "Verified Collection"
                }
                val gradesArr = o.optJSONArray("grades")
                if (gradesArr != null && gradesArr.length() > 0) {
                    val g0 = gradesArr.optJSONObject(0)
                    val gStr = g0?.optString("grade", "") ?: ""
                    if (gStr.isNotBlank()) gradeText = gStr
                }

                list.add(
                    HadithItem(
                        id = idOffset + i,
                        collection = collectionName,
                        bookNumber = "Book ${o.optJSONObject("reference")?.optInt("book", 1) ?: 1}",
                        hadithNumber = hNum,
                        grading = gradeText,
                        topic = collectionName,
                        arabic = "",
                        transliteration = "",
                        english = text,
                        hinglish = text,
                        hindi = text,
                        keywordsHinglish = collectionName.lowercase()
                    )
                )
            }

            db.insertHadiths(list)
            Result.success("Downloaded ${list.size} Hadiths from $collectionName for offline use!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Online Boost search using AlQuran Cloud public search API when internet is connected.
     */

    /**
     * Downloads full 114-Surah Tafseer from spa5k/tafsir_api CDN for offline storage.
     */
    suspend fun downloadFullTafseerEdition(
        slug: String,
        fiqhName: String,
        sourceLabel: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var totalInserted = 0
            for (surah in 1..114) {
                val url = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@v1.2.3/tafsir/$slug/$surah.json"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                }
                if (conn.responseCode != 200) continue
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val ayahsArr = JSONObject(body).optJSONArray("ayahs") ?: continue
                val batch = ArrayList<TafseerEntry>(ayahsArr.length())
                for (i in 0 until ayahsArr.length()) {
                    val o = ayahsArr.getJSONObject(i)
                    val aNum = o.optInt("ayah", 0)
                    val txt = o.optString("text", "").trim()
                    if (aNum > 0 && txt.isNotEmpty()) {
                        batch.add(
                            TafseerEntry(
                                ayahId = "$surah:$aNum",
                                fiqh = fiqhName,
                                sourceName = sourceLabel,
                                english = txt,
                                hinglish = txt,
                                hindi = txt
                            )
                        )
                    }
                }
                if (batch.isNotEmpty()) {
                    db.insertTafseers(batch)
                    totalInserted += batch.size
                }
            }
            if (totalInserted > 0) Result.success(totalInserted)
            else Result.failure(IllegalStateException("No tafseer downloaded (offline?)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchQuranOnline(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.alquran.cloud/v1/search/$encoded/all/en.sahih"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 10000
            }
            if (conn.responseCode != 200) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val matches = JSONObject(body).optJSONObject("data")?.optJSONArray("matches")
                ?: return@withContext emptyList()
            val out = mutableListOf<String>()
            val limit = minOf(matches.length(), 3)
            for (i in 0 until limit) {
                val m = matches.getJSONObject(i)
                val surahObj = m.optJSONObject("surah")
                val sName = surahObj?.optString("englishName", "") ?: ""
                val sNum = surahObj?.optInt("number", 0) ?: 0
                val aNum = m.optInt("numberInSurah", 0)
                val txt = m.optString("text", "")
                out.add("• $sName ($sNum:$aNum): $txt")
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
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
                nameArabic = o.optString("nameArabic", ""),
                nameEnglish = o.optString("nameEnglish", ""),
                nameHinglish = o.optString("nameHinglish", ""),
                nameHindi = o.optString("nameHindi", ""),
                ayahCount = o.getInt("ayahCount"),
                revelationType = o.optString("revelationType", "")
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
                fiqh = o.optString("fiqh", ""),
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
                collection = o.optString("collection", ""),
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

    private fun parseMasail(json: String): List<MaslaItem> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MaslaItem(
                id = o.getInt("id"),
                topic = o.optString("topic", ""),
                questionHinglish = o.optString("questionHinglish", ""),
                questionEnglish = o.optString("questionEnglish", ""),
                questionHindi = o.optString("questionHindi", ""),
                hanafi = o.optString("hanafi", ""),
                shafii = o.optString("shafii", ""),
                maliki = o.optString("maliki", ""),
                hanbali = o.optString("hanbali", ""),
                jafari = o.optString("jafari", ""),
                ahleHadith = o.optString("ahleHadith", ""),
                hadithReference = o.optString("hadithReference", ""),
                keywordsHinglish = o.optString("keywordsHinglish", "")
            )
        }
    }
}
