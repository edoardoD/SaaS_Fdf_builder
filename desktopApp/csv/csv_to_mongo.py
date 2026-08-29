#!/usr/bin/env python3
"""
CSV -> MongoDB Migrator per Manutenzioni Maker
Legge i file CSV in Fogli_Esportati_CSV/ e fa UPSERT dei template globali
(cantiereId=null) nella collection 'impianti' di MongoDB.

Salta RI: gia gestita da AntincendioMigrationTest.kt con logica speciale.

Uso:
    pip install pymongo
    python3 csv_to_mongo.py [--dry-run]
"""

import os
import re
import sys
import uuid

try:
    from pymongo import MongoClient
    from pymongo.errors import ConnectionFailure
except ImportError:
    print("pymongo non installato. Esegui: pip install pymongo")
    sys.exit(1)

# --- CONFIGURAZIONE ---
MONGO_URI  = "mongodb://localhost:27017"
DB_NAME    = "manutenzioni_db"
COLLECTION = "impianti"

SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(os.path.dirname(SCRIPT_DIR))
CSV_DIR      = os.path.join(PROJECT_ROOT, "Fogli_Esportati_CSV")

SKIP_CODES = {"RI"}

FILE_TO_CODE = {
    "CAB MT.csv": "CAB",
    "UPS.csv":    "UPS",
    "QMT.csv":    "QMT",
    "Q.csv":      "Q",
    "PEM.csv":    "PEM",
    "EM.csv":     "EM",
    "IS.csv":     "IS",
    "IE.csv":     "IE",
    "RI.csv":     "RI",
    "TRFS.csv":   "TRFS",
    "TRFO.csv":   "TRFO",
    "FTV.csv":    "FTV",
    "SPD.csv":    "SPD",
    "GE.csv":     "GE",
    "RIF.csv":    "RIF",
    "BACS.csv":   "BACS",
    "DEG.csv":    "DEG",
    "AMB.csv":    "AMB",
    "DS.csv":     "DS",
    "RIG.csv":    "RIG",
}

IGNORED_FILES = {"CALENDARIO.csv", "Foglio1.csv", "Foglio2.csv", "PARAMETRI.csv"}

NORMATIVE_MAP = {
    "CAB":  [{"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"},
             {"codNormativa": "CEI 0-16",  "descrizione": "Regola tecnica per la connessione alle reti AT e MT"}],
    "UPS":  [{"codNormativa": "CEI EN 62040", "descrizione": "Sistemi statici di continuita (UPS)"}],
    "QMT":  [{"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"},
             {"codNormativa": "CEI 0-16",  "descrizione": "Regola tecnica per la connessione alle reti AT e MT"}],
    "Q":    [{"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"},
             {"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"}],
    "PEM":  [{"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"},
             {"codNormativa": "DM 37/08",  "descrizione": "Attivita di installazione degli impianti"}],
    "EM":   [{"codNormativa": "UNI EN 1838", "descrizione": "Illuminazione di emergenza"},
             {"codNormativa": "CEI 64-8",    "descrizione": "Impianti elettrici utilizzatori"}],
    "IS":   [{"codNormativa": "UNI EN 1838", "descrizione": "Illuminazione di emergenza"},
             {"codNormativa": "CEI 64-8",    "descrizione": "Impianti elettrici utilizzatori"}],
    "IE":   [{"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"},
             {"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"}],
    "TRFS": [{"codNormativa": "CEI 14-4",  "descrizione": "Trasformatori di potenza"},
             {"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"}],
    "TRFO": [{"codNormativa": "CEI 14-4",  "descrizione": "Trasformatori di potenza"},
             {"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"}],
    "FTV":  [{"codNormativa": "CEI 82-25", "descrizione": "Guida alla realizzazione di sistemi fotovoltaici"},
             {"codNormativa": "CEI 11-20", "descrizione": "Impianti di produzione di energia elettrica"}],
    "SPD":  [{"codNormativa": "CEI EN 61643", "descrizione": "Dispositivi di protezione contro le sovratensioni"}],
    "GE":   [{"codNormativa": "CEI 11-20", "descrizione": "Impianti di produzione di energia elettrica"},
             {"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"}],
    "RIF":  [{"codNormativa": "CEI 33-7",  "descrizione": "Condensatori di potenza per impianti a corrente alternata"}],
    "BACS": [{"codNormativa": "CEI 64-8",         "descrizione": "Impianti elettrici utilizzatori"},
             {"codNormativa": "UNI EN ISO 16484",  "descrizione": "Building automation and control systems (BACS)"}],
    "DEG":  [{"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"},
             {"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"}],
    "AMB":  [{"codNormativa": "CEI 64-8",  "descrizione": "Impianti elettrici utilizzatori"},
             {"codNormativa": "CEI 11-27", "descrizione": "Lavori su impianti elettrici"}],
    "DS":   [{"codNormativa": "CEI 100-55", "descrizione": "Sistemi elettroacustici applicati ai servizi di emergenza"}],
    "RIG":  [{"codNormativa": "UNI EN 50194", "descrizione": "Rivelazione gas combustibili in locali domestici"},
             {"codNormativa": "UNI 11224",    "descrizione": "Manutenzione sistemi di rivelazione incendi"}],
}


def fix_mojibake(text):
    if not text:
        return text
    replacements = {
        '\x92': "'", '\x93': '"', '\x94': '"', '\x96': '-', '\x97': '--',
        '\xe0': 'a', '\xe8': 'e', '\xe9': 'e', '\xec': 'i',
        '\xf2': 'o', '\xf9': 'u', '\xc0': 'A', '\xc8': 'E',
        '\xc9': 'E', '\xcc': 'I', '\xd2': 'O', '\xd9': 'U',
        '\xf3': 'o', '\xfa': 'u', '\xed': 'i',
        '#N/D': '', '#RIF!': '',
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def read_csv_file(filepath):
    for enc in ['mac_roman', 'utf-8-sig', 'utf-8', 'cp1252', 'latin-1']:
        try:
            with open(filepath, 'r', encoding=enc) as f:
                return f.read()
        except (UnicodeDecodeError, UnicodeError):
            continue
    with open(filepath, 'rb') as f:
        return f.read().decode('mac_roman', errors='replace')


def parse_frequenza(raw):
    raw = raw.strip()
    if not raw or raw.upper() == "NP":
        return None
    raw = raw.replace(" ", "")
    if raw.startswith("0") and len(raw) >= 2:
        try:
            return {"tipo": "M", "valore": int(raw)}
        except ValueError:
            return None
    try:
        return {"tipo": "A", "valore": int(raw)}
    except ValueError:
        return None


def parse_csv(filepath, filename, code):
    raw = read_csv_file(filepath)
    lines = raw.strip().replace('\r\n', '\n').replace('\r', '\n').split('\n')

    if len(lines) < 3:
        print(f"   WARN: File troppo corto: {filename}")
        return None

    nome_completo = fix_mojibake(lines[0].split(";")[0].strip())

    premessa = ""
    for i, line in enumerate(lines):
        if line.strip().rstrip(";").strip().lower().startswith("premessa"):
            for j in range(i + 1, len(lines)):
                content = lines[j].split(";")[0].strip()
                if not content:
                    all_parts = [p.strip() for p in lines[j].split(";") if p.strip()]
                    content = " ".join(all_parts)
                if content:
                    premessa = fix_mojibake(content)
                    break
            break

    lista_attivita = []
    np_count = 0

    for i in range(2, len(lines)):
        line = lines[i].strip()
        if not line or line.replace(";", "").strip() == "":
            continue

        parts = line.split(";")
        if len(parts) < 2:
            continue

        periodicita_raw = parts[0].strip()
        progressivo_raw = parts[1].strip() if len(parts) > 1 else ""

        if not progressivo_raw:
            continue
        try:
            n_attivita = int(progressivo_raw)
        except ValueError:
            continue

        freq = parse_frequenza(periodicita_raw)
        if freq is None:
            np_count += 1
            continue

        tipo_attivita = fix_mojibake(parts[2].strip()) if len(parts) > 2 else ""
        if not tipo_attivita:
            continue

        descrizione = ""
        if len(parts) > 4 and parts[4].strip():
            descrizione = parts[4].strip()
        elif len(parts) > 3 and parts[3].strip():
            descrizione = parts[3].strip()

        if not descrizione:
            continue

        if descrizione.startswith('"') and not descrizione.endswith('"'):
            for j in range(i + 1, len(lines)):
                next_part = lines[j].strip().split(";")[0].strip()
                descrizione += " " + next_part
                if '"' in lines[j]:
                    break

        descrizione = fix_mojibake(descrizione.strip('"').strip())

        lista_attivita.append({
            "nAttivita":    n_attivita,
            "tipoAttivita": tipo_attivita,
            "descrizione":  descrizione,
            "frequenza":    freq,
        })

    if not lista_attivita:
        print(f"   WARN: Nessuna attivita valida in: {filename}")
        return None

    print(f"   OK: {len(lista_attivita)} attivita, {np_count} NP scartate")
    return {
        "_t":             "ImpiantoStandard",  # discriminatore polimorfismo Kotlin
        "id":             str(uuid.uuid4()),    # UUID per nuovi inserimenti
        "codIntervento":  code,
        "nomeCompleto":   nome_completo,
        "premessa":       premessa,
        "listaAttivita":  lista_attivita,
        "listaNormative": NORMATIVE_MAP.get(code, []),
        "cantiereId":     None,
        "quantita":       1,
        "noteSpecifiche": None,
    }


def upsert_to_mongo(collection, impianto, dry_run):
    code = impianto["codIntervento"]
    filter_query = {"codIntervento": code, "cantiereId": None}

    if dry_run:
        existing = collection.find_one(filter_query) if collection else None
        status = "AGGIORNA" if existing else "INSERISCE"
        return f"[DRY-RUN] {status}: {code}"

    result = collection.update_one(
        filter_query,
        {
            # Sempre aggiornati (anche sui documenti esistenti)
            "$set": {
                "_t":             "ImpiantoStandard",
                "nomeCompleto":   impianto["nomeCompleto"],
                "premessa":       impianto["premessa"],
                "listaAttivita":  impianto["listaAttivita"],
                "listaNormative": impianto["listaNormative"],
                "cantiereId":     None,
                "quantita":       impianto.get("quantita", 1),
                "noteSpecifiche": impianto.get("noteSpecifiche"),
            },
            # Solo al primo inserimento (non sovrascrive id esistenti)
            "$setOnInsert": {
                "id": impianto["id"],
            }
        },
        upsert=True
    )

    if result.upserted_id:
        return f"INSERITO  (_id: {result.upserted_id})"
    return f"AGGIORNATO (matched: {result.matched_count}, modified: {result.modified_count})"


def main():
    dry_run = "--dry-run" in sys.argv

    if dry_run:
        print("DRY-RUN attivo - nessuna scrittura su MongoDB\n")

    if not os.path.isdir(CSV_DIR):
        print(f"Directory CSV non trovata: {CSV_DIR}")
        sys.exit(1)

    print(f"CSV dir  : {CSV_DIR}")
    print(f"MongoDB  : {MONGO_URI} / {DB_NAME}.{COLLECTION}")
    print(f"Skip     : {SKIP_CODES}\n")

    collection = None
    if not dry_run:
        try:
            client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=3000)
            client.admin.command('ping')
            collection = client[DB_NAME][COLLECTION]
            print("Connesso a MongoDB\n")
        except ConnectionFailure as e:
            print(f"Impossibile connettersi a MongoDB: {e}")
            sys.exit(1)

    csv_files = sorted([f for f in os.listdir(CSV_DIR) if f.endswith('.csv')])
    processed = skipped = errors = 0

    for filename in csv_files:
        filepath = os.path.join(CSV_DIR, filename)

        if filename in IGNORED_FILES:
            print(f"Skip (ignorato): {filename}")
            skipped += 1
            continue

        if filename not in FILE_TO_CODE:
            print(f"Skip (non mappato): {filename}")
            skipped += 1
            continue

        code = FILE_TO_CODE[filename]

        if code in SKIP_CODES:
            print(f"Skip (gestito separatamente): {filename} -> {code}")
            skipped += 1
            continue

        if os.path.getsize(filepath) == 0:
            print(f"Skip (vuoto): {filename}")
            skipped += 1
            continue

        print(f"\n{filename} -> {code}")
        impianto = parse_csv(filepath, filename, code)

        if impianto is None:
            errors += 1
            continue

        status = upsert_to_mongo(collection, impianto, dry_run)
        print(f"   DB: {status}")
        processed += 1

    print(f"\n{'='*50}")
    print(f"Processati : {processed}")
    print(f"Saltati    : {skipped}")
    print(f"Errori     : {errors}")

    if not dry_run and processed > 0:
        print(f"\nVerifica con mongosh:")
        print(f"  use {DB_NAME}")
        print(f"  db.{COLLECTION}.find({{cantiereId: null}}, {{codIntervento: 1, nomeCompleto: 1}}).sort({{codIntervento: 1}})")


if __name__ == "__main__":
    main()
