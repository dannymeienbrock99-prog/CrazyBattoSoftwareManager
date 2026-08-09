# SoleLink Diagnostic für Android

**SoleLink Diagnostic** ist eine unabhängige Android-App zur sicheren Analyse von
Bluetooth-Low-Energy-Geräten wie dem Nike Adapt BB 2.0. Die App wurde für
Interoperabilitätsforschung entwickelt und ist **nicht mit Nike verbunden oder von
Nike autorisiert**.

## Stand dieser Version: 0.1.0

Die App kann:

- BLE-Geräte in der Nähe suchen und nach Signalstärke sortieren
- Gerätename, Adresse, RSSI, Service-UUIDs und Herstellerdaten anzeigen
- eine GATT-Verbindung herstellen
- alle gefundenen Dienste, Merkmale und Deskriptoren auflisten
- ausschließlich als lesbar markierte Merkmale auslesen
- Notifications und Indications über den standardisierten CCCD aktivieren
- Standard-Batteriestände erkennen
- alle Scan-, Verbindungs- und Datenereignisse protokollieren
- die vollständige Diagnose als JSON speichern

Die App kann in dieser Version **nicht**:

- den Schuh enger oder lockerer stellen
- LED-Farben verändern
- Passformprofile schreiben
- Firmware aktualisieren
- Nike-Anmeldung oder Nike-Cloudfunktionen ersetzen

Diese Funktionen bleiben gesperrt, bis das Protokoll anhand eigener,
rechtmäßiger Aufzeichnungen eindeutig verstanden und mit Sicherheitsgrenzen
implementiert wurde.

## Sicherheitsprinzip

Die App sendet keine unbekannten Werte an schreibfähige Merkmale. Sie verwendet
nur folgende BLE-Operationen:

1. Scan
2. Verbindung
3. Dienstsuche
4. Lesen von Merkmalen mit `PROPERTY_READ`
5. Aktivieren standardisierter Notifications/Indications über UUID `0x2902`

Schuhe bei späteren Protokolltests nicht tragen. Motorbefehle dürfen erst nach
eindeutiger Zuordnung, Begrenzung und Notfall-Lockerung freigeschaltet werden.

## Voraussetzungen

- Android 8.0 oder neuer, empfohlen Android 12+
- Bluetooth Low Energy
- Android Studio mit JDK 17
- Android SDK Platform 36
- Für den automatischen Build: Internetzugang für Gradle- und Maven-Abhängigkeiten

## In Android Studio öffnen

1. Diesen Ordner in Android Studio als Projekt öffnen.
2. JDK 17 auswählen.
3. Gradle synchronisieren.
4. Ein echtes Android-Gerät per USB verbinden.
5. Die Variante `debug` starten.
6. Auf dem Handy den Zugriff auf „Geräte in der Nähe“ erlauben.

Falls der Gradle Wrapper noch nicht erzeugt wurde, unter Windows zuerst
`bootstrap-and-build.ps1` in PowerShell ausführen. Das Skript lädt Gradle 8.13
von der offiziellen Gradle-Distribution, erzeugt den Wrapper und baut die
Debug-APK.

## APK bauen

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\bootstrap-and-build.ps1
```

Danach liegt die installierbare Debug-APK hier:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Mit vorhandenem Wrapper:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Linux/macOS:

```bash
./bootstrap-and-build.sh
```

## Diagnose durchführen

1. Beide Schuhe laden und in die Nähe des Handys legen.
2. App starten und Bluetooth-Berechtigungen erlauben.
3. `Scan starten` drücken.
4. Das wahrscheinlich passende Gerät anhand des Namens und der Signalstärke wählen.
5. `Sicher verbinden und auslesen` drücken.
6. Im Tab **Inspektor** Dienste und Werte prüfen.
7. Im Tab **Protokoll** einen JSON-Export speichern.

Bluetooth-Adressen können Hardware identifizieren. Diagnoseexporte deshalb nur
bewusst teilen.

## Nächste Entwicklungsstufe

Für eine echte Steuerung werden vergleichbare Aufzeichnungen pro Einzelaktion
benötigt:

- nur verbinden
- einmal enger
- einmal lockerer
- vollständig öffnen
- einzelne LED-Farben
- Passform speichern
- Status- und Akkurückmeldungen

Siehe [`docs/protocol-capture.md`](docs/protocol-capture.md).

## Paket und Eigentum

- Paketname: `de.crazybatto.solelink`
- Projektinhaber: Crazy_Batto
- App-Design und Quellcode sind eigenständig erstellt.
- Nike, Adapt und weitere Marken gehören ihren jeweiligen Inhabern.
