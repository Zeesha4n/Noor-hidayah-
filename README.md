# Noor-e-Hidayah — 100% Offline Android Quran & Muslim App

Built with **Kotlin + Jetpack Compose (Material 3) + Offline SQLite FTS4** (zero external server dependencies, zero KSP/KAPT build glitches).

## Features Included
1. **4-Language Interface & Instant Translation Switcher**: Switch between **Hinglish**, **Hindi (हिन्दी)**, **English**, and **Arabic (العربية)** in 1 tap from the top bar or directly on any Ayah card.
2. **Offline Hinglish Meaning Search (`HinglishSearchEngine` + SQLite FTS4)**: Search by meaning in Hinglish (e.g., *sabr / sabar*, *mushkil / pareshani / tension*, *hifazat / nazar / jadu*, *maafi / tauba*, *waswasa*, *kursi*, *tauheed*) or direct verse reference (`2:255`, `94:5`).
3. **Multi-Fiqh Tafseer**: Expandable Tafseer panel under every Ayah with 1-tap switching across **Hanafi**, **Shafi'i**, **Maliki**, **Hanbali**, **Ja'fari**, and **Ahl-e-Hadith**.
4. **All 114 Surahs Index + Verified Starter Surahs**: Pre-loaded with all 114 Surah metadata plus verified Arabic, Hindi, English, Hinglish text & Multi-Fiqh Tafseer for key daily Surahs & verses (Al-Fatihah, Ayat al-Kursi, Al-Baqarah 2:201 & 2:286, Ash-Sharh, Al-Asr, Al-Kawthar, Al-Kafirun, An-Nasr, Al-Ikhlas, Al-Falaq, An-Nas).
5. **100% Offline Tools**:
   - **Prayer Times Calculator** (Karachi, MWL, ISNA, Umm al-Qura + Hanafi/Shafi'i Asr + 15 built-in Indian & global cities).
   - **Digital Tasbih Counter** with haptic feedback & presets.
   - **Masnoon Duas** in Arabic, Hinglish, Hindi, and English.
6. **In-App Offline Pack Importer**: Import the full 6,236-verse Quran JSON or additional Fiqh Tafseer JSON packs directly from phone storage via **Settings > Import Offline Pack**.

## How to Build the APK in Android Studio
1. Extract `NoorHidayahAndroid.zip`.
2. Open **Android Studio** (Jellyfish / Koala / Ladybug or newer) → **Open** → select the `NoorHidayahAndroid` folder.
3. Let Gradle sync (uses Android SDK 34 + Kotlin 1.9.24 + Jetpack Compose BOM 2024.06.00).
4. Connect your Android phone (or start an Emulator) and press **Run ▶**, or go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## How to Bundle the Full 6,236-Verse Verified Quran
To ensure 100% scriptural accuracy with zero typos, use verified Quran text from Tanzil.net:
1. Download `quran-uthmani.txt`, `en.sahih.txt`, and `hi.hindi.txt` from Tanzil.net.
2. Run `python3 tools/build_full_quran_pack.py --arabic quran-uthmani.txt --english en.sahih.txt --hindi hi.hindi.txt --out app/src/main/assets/data/ayahs.json`.
3. Rebuild the APK (or import the generated JSON file right inside the app under **Settings**).
