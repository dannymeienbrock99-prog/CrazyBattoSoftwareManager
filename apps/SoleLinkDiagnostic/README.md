# SoleLink Control für Android

SoleLink ist eine eigenständige, lokale Android-Begleit-App für Bluetooth-LE-Schuhe und ähnliche Geräte. Die Oberfläche orientiert sich an der Bedienlogik moderner Smart-Shoe-Apps, verwendet aber einen eigenen Namen, ein eigenes Drachen-Katana-Design und keine Herstellerlogos.

## Version 0.3.0

- neue Startseite mit großem Paar-/Verbindungsbereich
- getrennte Passformregler für linken und rechten Schuh
- Preset-Modi „Locker“, „Bewegen“ und „Spiel“
- eigener lokal speicherbarer Modus
- Farbauswahl, Effekte und Leuchtdauer als interaktive Vorschau
- Bluetooth-Scan, GATT-Inspektor und JSON-Diagnoseexport
- Drachen-Katana-App-Symbol und Startbild
- eigenes Paket `de.crazybatto.solelink.control`, damit diese Fassung neben älteren Testversionen installiert werden kann

## Aktueller Funktionsumfang

Die Bluetooth-Suche, Verbindung, GATT-Dienstsuche, sichere Lesezugriffe, Notifications und der Diagnoseexport sind aktiv.

Die Passform- und Lichtoberfläche reagiert vollständig als lokale Vorschau. Unbekannte Motor- und LED-Schreibbefehle werden erst aktiviert, sobald ein verifiziertes Protokollprofil für das konkrete Schuhmodell vorliegt.

## Bauen

Voraussetzungen:

- JDK 17
- Android SDK 36
- Android Studio oder Gradle Wrapper

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Die APK liegt anschließend unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```
