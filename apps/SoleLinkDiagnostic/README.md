# SoleLink Control für Android

SoleLink ist eine eigenständige, lokale Android-Begleit-App für Bluetooth-LE-Schuhe. Die App verwendet einen eigenen Namen, ein eigenes Drachen-Katana-Design und keine Herstellerlogos.

## Version 0.4.0

### Fest hinterlegter rechter Schuh

Die App kennt den bereits bestätigten rechten Schuh dauerhaft:

- Anzeigename: `005-BQ5397-001`
- Bluetooth-Adresse: `C0:04:6F:A7:46:0D`
- Herstellerkennung: `0x0078`
- erwarteter Werbedienst: `0000180a-0000-1000-8000-00805f9b34fb`

Beim Scan wird dieser Schuh automatisch an die erste Stelle gesetzt, als **„Mein rechter Schuh“** markiert und auf Wunsch automatisch verbunden. Geräte namens `App-RCTW` werden nicht mehr als Schuhkandidat behandelt.

### Oberfläche

- Startseite mit festem Schuhstatus, Akkustand und Drachen-Katana-Motiv
- getrennte Passformregler für linken und rechten Schuh
- Preset-Modi „Locker“, „Bewegen“ und „Spiel“
- lokal speicherbarer eigener Modus
- Farbauswahl, Effekte und Leuchtdauer als interaktive Vorschau
- eigener Gerät-Tab für Scan, feste Schuhkarte, Verbindung, GATT-Diagnose und JSON-Export

### Aktive Bluetooth-Funktionen

- BLE-Scan
- automatische Erkennung des hinterlegten rechten Schuhs
- optionale automatische Verbindung
- GATT-Verbindung und Dienstsuche
- sichere Lesezugriffe
- Notifications und Indications
- Standard-Batteriedienst
- Diagnoseprotokoll und JSON-Export mit Schuhklassifizierung

Die Passform- und Lichtoberfläche arbeitet weiterhin als lokale Vorschau. Motor- und LED-Schreibbefehle werden erst aktiviert, sobald das konkrete Steuerprotokoll verifiziert ist.

## Installation und Updates

Diese Fassung verwendet das Paket:

```text
de.crazybatto.solelink.control.stable
```

Außerdem wird sie mit einem festen, ausschließlich für dieses öffentliche Testprojekt bestimmten Entwicklungsschlüssel signiert. Dadurch lassen sich zukünftige SoleLink-Testversionen über diese Fassung installieren, ohne jedes Mal die App löschen zu müssen. Der Schlüssel ist nicht für eine Veröffentlichung im Play Store oder für produktive Signaturen gedacht.

## Bauen

Voraussetzungen:

- JDK 17
- Android SDK 36
- Android Studio oder Gradle Wrapper

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Der Entwicklungs-Build verwendet `solelink-stable.jks`. Das automatisch erzeugte Quellcode-ZIP lässt diesen Schlüssel bewusst aus; der GitHub-Branch enthält ihn nur, damit reproduzierbare Test-Updates gebaut werden können.
