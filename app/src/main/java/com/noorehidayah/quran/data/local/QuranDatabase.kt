package com.noorehidayah.quran.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.search.HinglishSearchEngine

class QuranDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "noor_hidayah_offline.db"
        private const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE surahs (
                surah_number INTEGER PRIMARY KEY,
                name_arabic TEXT NOT NULL,
                name_english TEXT NOT NULL,
                name_hinglish TEXT NOT NULL,
                name_hindi TEXT NOT NULL,
                ayah_count INTEGER NOT NULL,
                revelation_type TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE ayahs (
                id TEXT PRIMARY KEY,
                surah_number INTEGER NOT NULL,
                ayah_number INTEGER NOT NULL,
                arabic TEXT NOT NULL,
                transliteration TEXT NOT NULL,
                english TEXT NOT NULL,
                hindi TEXT NOT NULL,
                hinglish TEXT NOT NULL,
                keywords_hinglish TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_ayahs_surah ON ayahs(surah_number, ayah_number)")

        // FTS4 is universally supported across all Android API 24+ devices without native extension issues
        db.execSQL(
            """
            CREATE VIRTUAL TABLE ayahs_fts USING fts4(
                ayah_id,
                surah_number,
                transliteration,
                english,
                hindi,
                hinglish,
                keywords_hinglish
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE tafseer (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ayah_id TEXT NOT NULL,
                fiqh TEXT NOT NULL,
                source_name TEXT NOT NULL,
                english TEXT NOT NULL,
                hinglish TEXT NOT NULL,
                hindi TEXT NOT NULL,
                UNIQUE(ayah_id, fiqh) ON CONFLICT REPLACE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tafseer_ayah ON tafseer(ayah_id)")

        db.execSQL(
            """
            CREATE TABLE duas (
                id INTEGER PRIMARY KEY,
                category TEXT NOT NULL,
                title_english TEXT NOT NULL,
                title_hinglish TEXT NOT NULL,
                title_hindi TEXT NOT NULL,
                title_arabic TEXT NOT NULL,
                arabic TEXT NOT NULL,
                transliteration TEXT NOT NULL,
                english TEXT NOT NULL,
                hinglish TEXT NOT NULL,
                hindi TEXT NOT NULL,
                reference TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS surahs")
        db.execSQL("DROP TABLE IF EXISTS ayahs")
        db.execSQL("DROP TABLE IF EXISTS ayahs_fts")
        db.execSQL("DROP TABLE IF EXISTS tafseer")
        db.execSQL("DROP TABLE IF EXISTS duas")
        onCreate(db)
    }

    fun isSeeded(): Boolean {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM surahs", null).use { cursor ->
            return cursor.moveToFirst() && cursor.getInt(0) > 0
        }
    }

    fun insertSurahs(surahs: List<Surah>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (s in surahs) {
                val cv = ContentValues().apply {
                    put("surah_number", s.surahNumber)
                    put("name_arabic", s.nameArabic)
                    put("name_english", s.nameEnglish)
                    put("name_hinglish", s.nameHinglish)
                    put("name_hindi", s.nameHindi)
                    put("ayah_count", s.ayahCount)
                    put("revelation_type", s.revelationType)
                }
                db.insertWithOnConflict("surahs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAyahs(ayahs: List<Ayah>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (a in ayahs) {
                val cv = ContentValues().apply {
                    put("id", a.id)
                    put("surah_number", a.surahNumber)
                    put("ayah_number", a.ayahNumber)
                    put("arabic", a.arabic)
                    put("transliteration", a.transliteration)
                    put("english", a.english)
                    put("hindi", a.hindi)
                    put("hinglish", a.hinglish)
                    put("keywords_hinglish", a.keywordsHinglish)
                }
                db.insertWithOnConflict("ayahs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

                db.delete("ayahs_fts", "ayah_id = ?", arrayOf(a.id))
                val ftsCv = ContentValues().apply {
                    put("ayah_id", a.id)
                    put("surah_number", a.surahNumber.toString())
                    put("transliteration", a.transliteration)
                    put("english", a.english)
                    put("hindi", a.hindi)
                    put("hinglish", a.hinglish)
                    put("keywords_hinglish", a.keywordsHinglish)
                }
                db.insert("ayahs_fts", null, ftsCv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertTafseers(entries: List<TafseerEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (t in entries) {
                val cv = ContentValues().apply {
                    put("ayah_id", t.ayahId)
                    put("fiqh", t.fiqh)
                    put("source_name", t.sourceName)
                    put("english", t.english)
                    put("hinglish", t.hinglish)
                    put("hindi", t.hindi)
                }
                db.insertWithOnConflict("tafseer", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertDuas(duas: List<DuaItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (d in duas) {
                val cv = ContentValues().apply {
                    put("id", d.id)
                    put("category", d.category)
                    put("title_english", d.titleEnglish)
                    put("title_hinglish", d.titleHinglish)
                    put("title_hindi", d.titleHindi)
                    put("title_arabic", d.titleArabic)
                    put("arabic", d.arabic)
                    put("transliteration", d.transliteration)
                    put("english", d.english)
                    put("hinglish", d.hinglish)
                    put("hindi", d.hindi)
                    put("reference", d.reference)
                }
                db.insertWithOnConflict("duas", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllSurahs(): List<Surah> {
        val list = mutableListOf<Surah>()
        val sql = """
            SELECT s.surah_number, s.name_arabic, s.name_english, s.name_hinglish,
                   s.name_hindi, s.ayah_count, s.revelation_type, COUNT(a.id) AS loaded_count
            FROM surahs s
            LEFT JOIN ayahs a ON s.surah_number = a.surah_number
            GROUP BY s.surah_number
            ORDER BY s.surah_number ASC
        """.trimIndent()
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Surah(
                        surahNumber = c.getInt(0),
                        nameArabic = c.getString(1),
                        nameEnglish = c.getString(2),
                        nameHinglish = c.getString(3),
                        nameHindi = c.getString(4),
                        ayahCount = c.getInt(5),
                        revelationType = c.getString(6),
                        loadedAyahCount = c.getInt(7)
                    )
                )
            }
        }
        return list
    }

    fun getAyahsBySurah(surahNumber: Int): List<Ayah> {
        val list = mutableListOf<Ayah>()
        readableDatabase.rawQuery(
            "SELECT id, surah_number, ayah_number, arabic, transliteration, english, hindi, hinglish, keywords_hinglish FROM ayahs WHERE surah_number = ? ORDER BY ayah_number ASC",
            arrayOf(surahNumber.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Ayah(
                        id = c.getString(0),
                        surahNumber = c.getInt(1),
                        ayahNumber = c.getInt(2),
                        arabic = c.getString(3),
                        transliteration = c.getString(4),
                        english = c.getString(5),
                        hindi = c.getString(6),
                        hinglish = c.getString(7),
                        keywordsHinglish = c.getString(8)
                    )
                )
            }
        }
        return list
    }

    fun getAyahsByIds(ids: Set<String>): List<Ayah> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val list = mutableListOf<Ayah>()
        readableDatabase.rawQuery(
            "SELECT id, surah_number, ayah_number, arabic, transliteration, english, hindi, hinglish, keywords_hinglish FROM ayahs WHERE id IN ($placeholders) ORDER BY surah_number ASC, ayah_number ASC",
            ids.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Ayah(
                        id = c.getString(0),
                        surahNumber = c.getInt(1),
                        ayahNumber = c.getInt(2),
                        arabic = c.getString(3),
                        transliteration = c.getString(4),
                        english = c.getString(5),
                        hindi = c.getString(6),
                        hinglish = c.getString(7),
                        keywordsHinglish = c.getString(8)
                    )
                )
            }
        }
        return list
    }

    fun getTafseersForAyah(ayahId: String): List<TafseerEntry> {
        val list = mutableListOf<TafseerEntry>()
        readableDatabase.rawQuery(
            "SELECT ayah_id, fiqh, source_name, english, hinglish, hindi FROM tafseer WHERE ayah_id = ?",
            arrayOf(ayahId)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    TafseerEntry(
                        ayahId = c.getString(0),
                        fiqh = c.getString(1),
                        sourceName = c.getString(2),
                        english = c.getString(3),
                        hinglish = c.getString(4),
                        hindi = c.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun searchAyahsOffline(rawQuery: String): List<Ayah> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Direct reference lookup e.g. "2:255" or "94:5"
        if (Regex("^\\d+:\\d+$").matches(trimmed)) {
            return getAyahsByIds(setOf(trimmed))
        }

        val tokens = HinglishSearchEngine.expandQueryTokens(trimmed)
        if (tokens.isEmpty()) return emptyList()

        val matchedIds = linkedSetOf<String>()
        val ftsMatch = HinglishSearchEngine.buildFtsMatchQuery(tokens)
        if (ftsMatch.isNotEmpty()) {
            try {
                readableDatabase.rawQuery(
                    "SELECT ayah_id FROM ayahs_fts WHERE ayahs_fts MATCH ? LIMIT 40",
                    arrayOf(ftsMatch)
                ).use { c ->
                    while (c.moveToNext()) {
                        matchedIds.add(c.getString(0))
                    }
                }
            } catch (_: Exception) {
                // Fallback cleanly to LIKE search below if FTS syntax is unusual
            }
        }

        // Fallback LIKE search (also handles Hindi Devanagari & Arabic substrings reliably)
        if (matchedIds.isEmpty()) {
            val likePattern = "%$trimmed%"
            readableDatabase.rawQuery(
                """
                SELECT id FROM ayahs
                WHERE hinglish LIKE ? OR english LIKE ? OR hindi LIKE ?
                   OR arabic LIKE ? OR transliteration LIKE ? OR keywords_hinglish LIKE ?
                LIMIT 40
                """.trimIndent(),
                arrayOf(likePattern, likePattern, likePattern, likePattern, likePattern, likePattern)
            ).use { c ->
                while (c.moveToNext()) {
                    matchedIds.add(c.getString(0))
                }
            }
        }

        return getAyahsByIds(matchedIds)
    }

    fun getAllDuas(): List<DuaItem> {
        val list = mutableListOf<DuaItem>()
        readableDatabase.rawQuery(
            "SELECT id, category, title_english, title_hinglish, title_hindi, title_arabic, arabic, transliteration, english, hinglish, hindi, reference FROM duas ORDER BY id ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    DuaItem(
                        id = c.getInt(0),
                        category = c.getString(1),
                        titleEnglish = c.getString(2),
                        titleHinglish = c.getString(3),
                        titleHindi = c.getString(4),
                        titleArabic = c.getString(5),
                        arabic = c.getString(6),
                        transliteration = c.getString(7),
                        english = c.getString(8),
                        hinglish = c.getString(9),
                        hindi = c.getString(10),
                        reference = c.getString(11)
                    )
                )
            }
        }
        return list
    }

    fun getTotalLoadedAyahs(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM ayahs", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun getTotalLoadedTafseers(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM tafseer", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
