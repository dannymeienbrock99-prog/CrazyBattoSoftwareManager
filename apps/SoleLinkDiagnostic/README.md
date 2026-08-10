# CrazyBatto – Nike Adapt 2.0 BB für Android

Dieses Repository enthält eine eigenständige Android-Test-App für die lokale Bluetooth-LE-Kommunikation mit selbstschnürenden Schuhen. Die App verwendet das CrazyBatto-Drachenlogo und erscheint auf dem Android-Display unter dem Namen **„Nike Adapt 2.0 BB“**.

> Hinweis: Dies ist keine offizielle Nike-App und wird nicht von Nike entwickelt, veröffentlicht oder unterstützt. Das bereitgestellte Original-APK wurde ausschließlich als technische Referenz für Interoperabilität und die Analyse des Kopplungsablaufs betrachtet. Quellcode, Nike-Grafiken und Nike-App-Ressourcen werden nicht in diese App übernommen.

## Version 0.4.2

### Neues Display- und Verknüpfungslogo

- neues quadratisches CrazyBatto-Drachenlogo
- metallisches Windows-Emblem im Zentrum
- Schriftzug `CrazyBatto`
- Unterzeile `NIKE ADAPT 2.0 BB`
- als Android-Launcher-Symbol eingebaut
- wird ebenfalls im Kopfbereich der App angezeigt
- Verknüpfungsname auf dem Android-Display: `Nike Adapt 2.0 BB`

### Fest hinterlegter rechter Schuh

- Anzeigename: `005-BQ5397-001`
- Bluetooth-Adresse: `C0:04:6F:A7:46:0D`
- Herstellerkennung: `0x0078`
- erwarteter Werbedienst: `0000180a-0000-1000-8000-00805f9b34fb`

Beim Scan wird dieser Schuh automatisch an die erste Stelle gesetzt, als **„Mein rechter Schuh“** markiert und auf Wunsch automatisch verbunden. Geräte namens `App-RCTW` werden nicht als Schuhkandidat behandelt.

### Behandlung von Status 19

Status `19` beziehungsweise `0x13` bedeutet, dass die Bluetooth-Gegenstelle die Verbindung beendet hat. Die App verwendet eine kurze Stabilisierungspause, langsamere sichere GATT-Abfragen und höchstens zwei Wiederverbindungsversuche. Proprietäre Motor- und LED-Befehle bleiben gesperrt, bis der originale Kopplungs- und Authentifizierungsablauf eindeutig rekonstruiert und sicher getestet wurde.

### Aktive Funktionen

- BLE-Scan
- Erkennung des hinterlegten rechten Schuhs
- optionale automatische Verbindung
- GATT-Dienstsuche
- sichere Standard-Lesezugriffe
- Wiederverbindung bei Status 19, 8 und 133
- Standard-Batteriedienst
- Diagnoseprotokoll und JSON-Export
- interaktive Passform-, Modus- und Lichtvorschau

## Installation und Updates

Paketname:

```text
de.crazybatto.solelink.control.stable
```

Version 0.4.2 kann als Update über 0.4.0 oder 0.4.1 installiert werden, sofern diese Fassungen mit demselben Testschlüssel signiert wurden.

## Bauen

Voraussetzungen:

- JDK 17
- Android SDK 36
- Android Studio oder Gradle Wrapper

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
