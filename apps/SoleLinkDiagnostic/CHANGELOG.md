# Änderungsprotokoll

## 0.4.3

- ausdrücklichen Einzelschuh-Modus eingeführt
- ein linker oder rechter Schuh reicht für eine nutzbare Sitzung
- zweiter Schuh ist optional und blockiert den Start nicht
- neue Auswahl: „Nur rechter Schuh“, „Nur linker Schuh“ und „Beide nacheinander“
- nicht ausgewählte Passformseite wird in der Oberfläche deaktiviert
- Preset-Modi verändern nur die aktuell ausgewählte Seite
- Scan wählt den besten erkannten Schuhkandidaten statt zwingend nur den gespeicherten rechten Schuh
- gespeicherter rechter Schuh behält die höchste Priorität
- anderer passender Schuh kann ohne den rechten Schuh allein verbunden werden
- Diagnoseexport auf Schema 4 erweitert
- Exportfelder `mode=SINGLE_SHOE`, `pairRequired=false` und `minimumRequiredShoes=1`
- zusätzliche Tests für einzelne rechte, einzelne andere und fehlende Schuhkandidaten
- VersionCode auf 7 und Version auf 0.4.3 erhöht

## 0.4.2

- neues CrazyBatto-Drachenlogo als Android-Launcher- und Verknüpfungssymbol
- Unterzeile `NIKE ADAPT 2.0 BB`
- Android-Verknüpfungsname `Nike Adapt 2.0 BB`

## 0.4.1

- Status 19 (`0x13`) verständlich erklärt
- höchstens zwei automatische Wiederverbindungsversuche
- verzögerte und langsamere sichere GATT-Abfragen
- Diagnoseexport Schema 3

## 0.4.0

- rechter Schuh `005-BQ5397-001` / `C0:04:6F:A7:46:0D` fest hinterlegt
- Herstellerkennung `0x0078`
- stabiler Testsignaturschlüssel

## 0.3.0

- Smart-Shoe-Bedienoberfläche
- Passform-, Modus- und Lichtvorschau

## 0.1.0

- erster sicherer BLE-Scanner und GATT-Inspektor
