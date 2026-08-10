# Protokollaufzeichnung für Interoperabilität

Dieses Dokument beschreibt den sicheren nächsten Schritt zur Rekonstruktion des
BLE-Protokolls. Es ist keine Anleitung zur Umgehung von Konten, Bezahlsystemen
oder Zugriffsschutz.

## Benötigt

- rechtmäßig vorhandener Adapt BB 2.0
- Android-Handy, auf dem die originale App noch funktioniert
- USB-Debugging
- Android Platform Tools (`adb`)
- Wireshark
- Schuhe während der Tests **nicht tragen**

## HCI-Snoop aktivieren

1. Android-Entwickleroptionen öffnen.
2. `Bluetooth-HCI-Snoop-Protokoll aktivieren`.
3. Bluetooth kurz aus- und wieder einschalten.
4. Original-App öffnen und genau eine Aktion durchführen.
5. Android-Fehlerbericht erstellen.
6. Die Datei `btsnoop_hci.log` aus dem Fehlerbericht extrahieren.
7. Das Protokoll in Wireshark öffnen.

Je nach Hersteller und Android-Version kann der Logpfad abweichen. Der
Android-Fehlerbericht ist meist zuverlässiger als ein direkter Dateizugriff.

## Je Aktion ein separates Protokoll

Für klare Differenzen jede Aktion in einer neuen Aufzeichnung durchführen:

1. Verbindung ohne weitere Aktion
2. Linker Schuh einmal enger
3. Linker Schuh einmal lockerer
4. Rechter Schuh einmal enger
5. Rechter Schuh einmal lockerer
6. Vollständig öffnen
7. LED auf eine eindeutig andere Farbe stellen
8. Passform speichern
9. Akkustand abrufen
10. Verbindung trennen

Zwischen Aktionen mindestens fünf Sekunden warten. Zeitpunkt und Aktion
zusätzlich in einer Textdatei notieren.

## Gesuchte Informationen

- GATT-Service-UUID
- Schreib-Characteristic
- Notify-/Indicate-Characteristic
- Paketpräfix und Befehlsnummer
- linker/rechter Schuh
- Sequenznummer
- Payload-Länge
- Prüfsumme oder MAC
- Challenge-Response oder Sitzungsschlüssel
- Status- und Fehlerantworten
- sichere Wertebereiche

## Sicherheitsgrenze

Keine zufälligen Bytes an schreibfähige Merkmale senden. Erst wenn ein Paket aus
mehreren Aufzeichnungen reproduzierbar zugeordnet ist, darf es in einem
separaten Labor-Build getestet werden. Dabei müssen vorhanden sein:

- sofortige Lockerungsfunktion
- maximales Kraft-/Schrittlimit
- Befehlstimeout
- Verbindungsabbruch
- Tests ohne getragenen Schuh
- vollständiges Sendeprotokoll
