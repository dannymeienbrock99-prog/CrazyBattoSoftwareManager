# CrazyBatto – Nike Adapt 2.0 BB für Android

Diese Android-Test-App dient der lokalen Bluetooth-LE-Kommunikation mit selbstschnürenden Schuhen. Sie verwendet das CrazyBatto-Drachenlogo und erscheint auf dem Android-Display unter dem Namen **„Nike Adapt 2.0 BB“**.

> Dies ist keine offizielle Nike-App und wird nicht von Nike entwickelt, veröffentlicht oder unterstützt. Das bereitgestellte Original-APK dient ausschließlich als technische Referenz für Interoperabilität. Nike-Quellcode, Nike-Grafiken und Nike-App-Ressourcen werden nicht übernommen.

## Version 0.4.3 – Einzelschuh-Modus

Die App verlangt kein vollständiges Paar mehr. **Ein linker oder ein rechter Schuh reicht aus**, um eine Sitzung zu starten.

### Verhalten

- Standardmodus: nur rechter Schuh
- umschaltbar auf nur linken Schuh
- optional: beide Schuhe nacheinander
- die App wartet nach einer erfolgreichen Verbindung nicht auf den zweiten Schuh
- Scan und automatische Verbindung wählen den besten erkannten Schuhkandidaten
- der gespeicherte rechte Schuh bleibt bevorzugt, ist aber nicht mehr die einzige mögliche Verbindung
- ein zweiter passender Schuh mit derselben Modell- und Herstellerkennung kann allein genutzt werden
- `App-RCTW` wird weiterhin nicht als Schuhkandidat ausgewählt
- nur eine aktive GATT-Verbindung zur gleichen Zeit; beim Wechsel wird der andere Schuh separat verbunden

### Bedienoberfläche

Auf der Startseite gibt es einen neuen Bereich **„EINZELSCHUH-MODUS“** mit:

- Nur rechter Schuh
- Nur linker Schuh
- Beide nacheinander (optional)

Im Einzelschuh-Modus wird die nicht ausgewählte Seite in der Passformansicht deaktiviert. Preset-Modi verändern nur die gewählte Seite. Sichtbare Texte weisen ausdrücklich darauf hin, dass ein Schuh genügt.

### Diagnoseexport

Der JSON-Export verwendet Schema 4 und enthält:

```json
{
  "connectionPolicy": {
    "mode": "SINGLE_SHOE",
    "pairRequired": false,
    "minimumRequiredShoes": 1
  }
}
```

### Fest hinterlegter rechter Schuh

- Anzeigename: `005-BQ5397-001`
- Bluetooth-Adresse: `C0:04:6F:A7:46:0D`
- Herstellerkennung: `0x0078`
- Werbedienst: `0000180a-0000-1000-8000-00805f9b34fb`

### Aktive Funktionen

- BLE-Scan
- automatische Schuherkennung
- Einzelverbindung links oder rechts
- Status-19-Wiederverbindung
- GATT-Dienstsuche
- sichere Standard-Lesezugriffe
- Standard-Batteriedienst
- Diagnoseprotokoll und JSON-Export
- interaktive Passform-, Modus- und Lichtvorschau

Motor- und LED-Schreibbefehle bleiben gesperrt, bis der Kopplungs- und Authentifizierungsablauf eindeutig rekonstruiert und sicher getestet wurde.

## Installation und Updates

Paketname:

```text
de.crazybatto.solelink.control.stable
```

Version 0.4.3 kann als Update über 0.4.0, 0.4.1 oder 0.4.2 installiert werden, sofern die vorhandene Fassung mit demselben Testschlüssel signiert wurde.

## Bauen

```bash
python3 tools/apply_single_shoe_logic.py
python3 tools/apply_single_shoe_ui.py
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
