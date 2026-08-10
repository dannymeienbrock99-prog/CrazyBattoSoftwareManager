# Änderungsprotokoll

## 0.4.2

- neues CrazyBatto-Drachenlogo als Android-Launcher- und Verknüpfungssymbol eingebaut
- bisherige Unterzeile `WindowsManager` durch `NIKE ADAPT 2.0 BB` ersetzt
- Android-Verknüpfungsname auf `Nike Adapt 2.0 BB` geändert
- neues Logo wird auch im Kopfbereich der App verwendet
- VersionCode auf 6 und Version auf 0.4.2 erhöht
- Build-Artefakte eindeutig als `Nike-Adapt-2.0-BB-CrazyBatto-0.4.2` benannt
- CI prüft nun zusätzlich sichtbaren App-Namen, Versionsnummer und APK-Signatur
- bereitgestelltes Original-APK nur als Interoperabilitätsreferenz dokumentiert; kein Nike-Code und keine Nike-Grafik übernommen

## 0.4.1

- Status 19 (`0x13`) wird korrekt als durch die Gegenstelle beendete Verbindung erklärt
- maximal zwei automatische Wiederverbindungsversuche nach 2 und 5 Sekunden
- sichtbare Aufforderung, während der Wiederverbindung eine Seitentaste am Schuh zu drücken
- verzögerte Dienstsuche, langsamere GATT-Operationsfolge und sichere Standardlesevorgänge
- proprietäre Benachrichtigungen werden beim ersten Schuhkontakt nicht automatisch aktiviert
- Diagnoseexport Schema 3 und Tests für Status-19-Erkennung ergänzt

## 0.4.0

- rechter Schuh dauerhaft als `005-BQ5397-001` / `C0:04:6F:A7:46:0D` hinterlegt
- Herstellerkennung `0x0078` und Geräteinformationsdienst in die Erkennung aufgenommen
- gespeicherter Schuh wird zuerst angezeigt und automatisch verbunden
- `App-RCTW` wird nicht als Schuhprofil behandelt
- stabiler Entwicklungsschlüssel für zukünftige Test-Updates eingeführt

## 0.3.0

- Smart-Shoe-Bedienoberfläche mit eigenständigem Branding
- Passform-, Modus- und Lichtvorschau
- Bluetooth-Scan, GATT-Diagnose und JSON-Export

## 0.1.0

- erster sicherer BLE-Scanner und GATT-Inspektor
