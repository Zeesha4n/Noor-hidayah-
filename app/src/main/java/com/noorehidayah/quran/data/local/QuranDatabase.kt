package com.noorehidayah.quran.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.noorehidayah.quran.data.model.Ayah
import com.noorehidayah.quran.data.model.DuaItem
import com.noorehidayah.quran.data.model.HadithItem
import com.noorehidayah.quran.data.model.Surah
import com.noorehidayah.quran.data.model.TafseerEntry
import com.noorehidayah.quran.data.search.HinglishSearchEngine

class QuranDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "noor_hidayah_offline.db"
        private const val DB_VERSION = 2
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

        db.execSQL(
            """
            CREATE VIRTUAL TABLE duas_fts USING fts4(
                dua_id,
                category,
                title_english,
                title_hinglish,
                title_hindi,
                english,
                hinglish,
                hindi
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE hadiths (
                id INTEGER PRIMARY KEY,
                collection TEXT NOT NULL,
                book_number TEXT NOT NULL,
                hadith_number TEXT NOT NULL,
                grading TEXT NOT NULL,
                topic TEXT NOT NULL,
                arabic TEXT NOT NULL,
                transliteration TEXT NOT NULL,
                english TEXT NOT NULL,
                hinglish TEXT NOT NULL,
                hindi TEXT NOT NULL,
                keywords_hinglish TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE VIRTUAL TABLE hadiths_fts USING fts4(
                hadith_id,
                collection,
                topic,
                english,
                hinglish,
                hindi,
                keywords_hinglish
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
        db.execSQL("DROP TABLE IF EXISTS duas_fts")
        db.execSQL("DROP TABLE IF EXISTS hadiths")
        db.execSQL("DROP TABLE IF EXISTS hadiths_fts")
        onCreate(db)
    }

    fun isSeeded(): Boolean {
        val db = readableDatabase
        val surahsCount = db.rawQuery("SELECT COUNT(*) FROM surahs", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        val hadithCount = db.rawQuery("SELECT COUNT(*) FROM hadiths", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return surahsCount > 0 && hadithCount > 0
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
            val stmt = db.compileStatement(
                "INSERT OR REPLACE INTO ayahs (id, surah_number, ayah_number, arabic, transliteration, english, hindi, hinglish, keywords_hinglish) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            for (a in ayahs) {
                stmt.clearBindings()
                stmt.bindString(1, a.id)
                stmt.bindLong(2, a.surahNumber.toLong())
                stmt.bindLong(3, a.ayahNumber.toLong())
                stmt.bindString(4, a.arabic)
                stmt.bindString(5, a.transliteration)
                stmt.bindString(6, a.english)
                stmt.bindString(7, a.hindi)
                stmt.bindString(8, a.hinglish)
                stmt.bindString(9, a.keywordsHinglish)
                stmt.executeInsert()
            }
            db.execSQL("DELETE FROM ayahs_fts")
            db.execSQL(
                "INSERT INTO ayahs_fts(ayah_id, surah_number, transliteration, english, hindi, hinglish, keywords_hinglish) " +
                    "SELECT id, CAST(surah_number AS TEXT), transliteration, english, hindi, hinglish, keywords_hinglish FROM ayahs"
            )
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

                db.delete("duas_fts", "dua_id = ?", arrayOf(d.id.toString()))
                val ftsCv = ContentValues().apply {
                    put("dua_id", d.id.toString())
                    put("category", d.category)
                    put("title_english", d.titleEnglish)
                    put("title_hinglish", d.titleHinglish)
                    put("title_hindi", d.titleHindi)
                    put("english", d.english)
                    put("hinglish", d.hinglish)
                    put("hindi", d.hindi)
                }
                db.insert("duas_fts", null, ftsCv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertHadiths(hadiths: List<HadithItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (h in hadiths) {
                val cv = ContentValues().apply {
                    put("id", h.id)
                    put("collection", h.collection)
                    put("book_number", h.bookNumber)
                    put("hadith_number", h.hadithNumber)
                    put("grading", h.grading)
                    put("topic", h.topic)
                    put("arabic", h.arabic)
                    put("transliteration", h.transliteration)
                    put("english", h.english)
                    put("hinglish", h.hinglish)
                    put("hindi", h.hindi)
                    put("keywords_hinglish", h.keywordsHinglish)
                }
                db.insertWithOnConflict("hadiths", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

                db.delete("hadiths_fts", "hadith_id = ?", arrayOf(h.id.toString()))
                val ftsCv = ContentValues().apply {
                    put("hadith_id", h.id.toString())
                    put("collection", h.collection)
                    put("topic", h.topic)
                    put("english", h.english)
                    put("hinglish", h.hinglish)
                    put("hindi", h.hindi)
                    put("keywords_hinglish", h.keywordsHinglish)
                }
                db.insert("hadiths_fts", null, ftsCv)
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
                list.add(readAyahCursor(c))
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
                list.add(readAyahCursor(c))
            }
        }
        return list
    }

    private fun readAyahCursor(c: android.database.Cursor): Ayah = Ayah(
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
            }
        }

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

    fun searchHadithsOffline(rawQuery: String): List<HadithItem> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()
        val tokens = HinglishSearchEngine.expandQueryTokens(trimmed)
        val matchedIds = linkedSetOf<Int>()
        val ftsMatch = HinglishSearchEngine.buildFtsMatchQuery(tokens)
        if (ftsMatch.isNotEmpty()) {
            try {
                readableDatabase.rawQuery(
                    "SELECT hadith_id FROM hadiths_fts WHERE hadiths_fts MATCH ? LIMIT 25",
                    arrayOf(ftsMatch)
                ).use { c ->
                    while (c.moveToNext()) {
                        c.getString(0).toIntOrNull()?.let { matchedIds.add(it) }
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (matchedIds.isEmpty()) {
            val likePattern = "%$trimmed%"
            readableDatabase.rawQuery(
                "SELECT id FROM hadiths WHERE hinglish LIKE ? OR english LIKE ? OR hindi LIKE ? OR collection LIKE ? OR topic LIKE ? LIMIT 25",
                arrayOf(likePattern, likePattern, likePattern, likePattern, likePattern)
            ).use { c ->
                while (c.moveToNext()) {
                    matchedIds.add(c.getInt(0))
                }
            }
        }
        if (matchedIds.isEmpty()) return emptyList()
        val all = getAllHadiths().associateBy { it.id }
        return matchedIds.mapNotNull { all[it] }
    }

    fun searchDuasOffline(rawQuery: String): List<DuaItem> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()
        val tokens = HinglishSearchEngine.expandQueryTokens(trimmed)
        val matchedIds = linkedSetOf<Int>()
        val ftsMatch = HinglishSearchEngine.buildFtsMatchQuery(tokens)
        if (ftsMatch.isNotEmpty()) {
            try {
                readableDatabase.rawQuery(
                    "SELECT dua_id FROM duas_fts WHERE duas_fts MATCH ? LIMIT 25",
                    arrayOf(ftsMatch)
                ).use { c ->
                    while (c.moveToNext()) {
                        c.getString(0).toIntOrNull()?.let { matchedIds.add(it) }
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (matchedIds.isEmpty()) {
            val likePattern = "%$trimmed%"
            readableDatabase.rawQuery(
                "SELECT id FROM duas WHERE title_hinglish LIKE ? OR title_english LIKE ? OR hinglish LIKE ? OR english LIKE ? OR hindi LIKE ? OR category LIKE ? LIMIT 25",
                arrayOf(likePattern, likePattern, likePattern, likePattern, likePattern, likePattern)
            ).use { c ->
                while (c.moveToNext()) {
                    matchedIds.add(c.getInt(0))
                }
            }
        }
        if (matchedIds.isEmpty()) return emptyList()
        val all = getAllDuas().associateBy { it.id }
        return matchedIds.mapNotNull { all[it] }
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

    fun getAllHadiths(): List<HadithItem> {
        val list = mutableListOf<HadithItem>()
        readableDatabase.rawQuery(
            "SELECT id, collection, book_number, hadith_number, grading, topic, arabic, transliteration, english, hinglish, hindi, keywords_hinglish FROM hadiths ORDER BY id ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    HadithItem(
                        id = c.getInt(0),
                        collection = c.getString(1),
                        bookNumber = c.getString(2),
                        hadithNumber = c.getString(3),
                        grading = c.getString(4),
                        topic = c.getString(5),
                        arabic = c.getString(6),
                        transliteration = c.getString(7),
                        english = c.getString(8),
                        hinglish = c.getString(9),
                        hindi = c.getString(10),
                        keywordsHinglish = c.getString(11)
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

    fun getTotalLoadedHadiths(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM hadiths", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
