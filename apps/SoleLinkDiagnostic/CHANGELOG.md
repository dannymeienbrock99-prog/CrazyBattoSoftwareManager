# Änderungsprotokoll

## 0.4.1

- Status 19 (`0x13`) wird korrekt als durch die Gegenstelle beendete Verbindung erklärt
- maximal zwei automatische Wiederverbindungsversuche nach 2 und 5 Sekunden
- sichtbare Aufforderung, während der Wiederverbindung eine Seitentaste am Schuh zu drücken
- verzögerte Dienstsuche, damit sich die BLE-Verbindung zunächst stabilisieren kann
- langsamere GATT-Operationsfolge für den gespeicherten Schuh
- proprietäre Benachrichtigungen werden beim ersten Schuhkontakt nicht mehr automatisch aktiviert
- beim gespeicherten Schuh werden zunächst nur sichere Standardwerte gelesen
- veraltete GATT-Callbacks früherer Verbindungen werden ignoriert und geschlossen
- Verbindungsstatus und Wiederholungsversuch werden in Diagnoseexport Schema 3 gespeichert
- Tests für Status-19-Erkennung und Wiederverbindungslogik ergänzt
- VersionCode auf 5 und Version auf 0.4.1 erhöht

## 0.4.0

- rechter Schuh dauerhaft als `005-BQ5397-001` / `C0:04:6F:A7:46:0D` hinterlegt
- Herstellerkennung `0x0078` und Geräteinformationsdienst in die Erkennung aufgenommen
- gespeicherter Schuh wird bei Scans immer zuerst angezeigt
- gespeicherter Schuh erhält den sichtbaren Namen „Mein rechter Schuh“
- automatische Verbindung zum gespeicherten Schuh ist standardmäßig aktiv
- `App-RCTW` wird ausdrücklich nicht als Schuhprofil behandelt
- Diagnoseexport auf Schema 2 erweitert und um Schuhklassifizierung ergänzt
- Scanzeit auf 20 Sekunden erhöht
- Paket auf `de.crazybatto.solelink.control.stable` umgestellt
- stabiler Entwicklungsschlüssel für zukünftige Test-Updates eingeführt
- VersionCode auf 4 und Version auf 0.4.0 erhöht
- zusätzliche Unit-Tests für die feste Schuherkennung

## 0.3.0

- neue Smart-Shoe-Bedienoberfläche mit eigenständigem Branding
- Passform-, Modus- und Lichtvorschau
- Bluetooth-Scan, GATT-Diagnose und JSON-Export

## 0.1.0

- erster sicherer BLE-Scanner und GATT-Inspektor
