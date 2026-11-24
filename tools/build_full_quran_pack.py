#!/usr/bin/env python3
"""
Helper script to convert full 6,236-verse verified Tanzil.net / Quran.com text dumps
into the exact `ayahs.json` format used by Noor-e-Hidayah (offline SQLite + FTS4).

Usage:
  1. Download verified plain-text (`surah|ayah|text`) files from https://tanzil.net/download/
     - Arabic Uthmani: quran-uthmani.txt
     - English (Saheeh International): en.sahih.txt
     - Hindi (Suhel Farooq Khan): hi.hindi.txt
     - Roman Urdu / Hinglish (or Transliteration): en.transliteration.txt
  2. Run:
     python3 tools/build_full_quran_pack.py \
       --arabic quran-uthmani.txt \
       --english en.sahih.txt \
       --hindi hi.hindi.txt \
       --hinglish hi.roman.txt \
       --out app/src/main/assets/data/ayahs.json
  3. Rebuild the APK in Android Studio (or import the generated JSON directly via Settings > Import Pack).
"""

import argparse
import json
from pathlib import Path


def load_tanzil_txt(path: Path) -> dict[str, str]:
    data = {}
    if not path or not path.exists():
        return data
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|", 2)
        if len(parts) == 3:
            key = f"{int(parts[0])}:{int(parts[1])}"
            data[key] = parts[2].strip()
    return data


def main():
    parser = argparse.ArgumentParser(description="Build full 6,236-verse ayahs.json pack for Noor-e-Hidayah.")
    parser.add_argument("--arabic", required=True, type=Path, help="Tanzil Arabic pipe-delimited file")
    parser.add_argument("--english", type=Path, help="English translation pipe-delimited file")
    parser.add_argument("--hindi", type=Path, help="Hindi translation pipe-delimited file")
    parser.add_argument("--hinglish", type=Path, help="Hinglish / Roman Urdu translation file")
    parser.add_argument("--translit", type=Path, help="Arabic transliteration file")
    parser.add_argument("--out", default=Path("ayahs_full.json"), type=Path)
    args = parser.parse_args()

    arabic = load_tanzil_txt(args.arabic)
    english = load_tanzil_txt(args.english) if args.english else {}
    hindi = load_tanzil_txt(args.hindi) if args.hindi else {}
    hinglish = load_tanzil_txt(args.hinglish) if args.hinglish else {}
    translit = load_tanzil_txt(args.translit) if args.translit else {}

    out_list = []
    for key, ar_text in arabic.items():
        s_num, a_num = map(int, key.split(":"))
        en_text = english.get(key, "")
        hi_text = hindi.get(key, "")
        hl_text = hinglish.get(key, en_text)
        tr_text = translit.get(key, "")
        out_list.append({
            "id": key,
            "surahNumber": s_num,
            "ayahNumber": a_num,
            "arabic": ar_text,
            "transliteration": tr_text,
            "english": en_text,
            "hindi": hi_text,
            "hinglish": hl_text,
            "keywordsHinglish": ""
        })

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out_list, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(out_list)} verses to {args.out}")


if __name__ == "__main__":
    main()
