#!/usr/bin/env python3
"""Patch the existing controller into explicit single-shoe mode before building."""

from pathlib import Path

ROOT = Path("apps/SoleLinkDiagnostic/app/src/main/java/de/crazybatto/solelink")
VIEW_MODEL = ROOT / "ui/MainViewModel.kt"
JSON_EXPORT = ROOT / "util/JsonExport.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def patch_view_model() -> None:
    text = VIEW_MODEL.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "import de.crazybatto.solelink.ble.KnownShoeRegistry\n",
        "import de.crazybatto.solelink.ble.KnownShoeRegistry\n"
        "import de.crazybatto.solelink.ble.SingleShoePolicy\n",
        "import single-shoe policy",
    )

    text = replace_once(
        text,
        '            "SoleLink Control gestartet. Der rechte Schuh ist fest hinterlegt.",\n',
        '            "Einzelschuh-Modus gestartet. Ein verbundener Schuh reicht; " +\n'
        '                "der zweite ist optional.",\n',
        "startup message",
    )

    text = replace_once(
        text,
        "                val knownShoe = scannedDevices.firstOrNull(KnownShoeRegistry::isStoredRightShoe)\n",
        "                val knownShoe = SingleShoePolicy.chooseBestCandidate(scannedDevices)\n",
        "select first available shoe",
    )

    text = replace_once(
        text,
        '                        "Mein rechter Schuh wurde gefunden und wird automatisch verbunden.",\n',
        '                        "Ein passender Schuh wurde gefunden und wird allein verbunden. " +\n'
        '                            "Die App wartet nicht auf den zweiten Schuh.",\n',
        "auto-connect message",
    )

    text = replace_once(
        text,
        "    fun startScan() {\n"
        "        autoConnectAttemptedForScan = false\n"
        "        scanner.startScan(durationMillis = 20_000L)\n"
        "    }\n",
        "    fun startScan() {\n"
        "        autoConnectAttemptedForScan = false\n"
        "        appendLog(\n"
        "            LogLevel.INFO,\n"
        "            \"SCHUH\",\n"
        "            \"Einzelschuh-Suche gestartet. Sobald ein passender Schuh gefunden wird, \" +\n"
        "                \"ist die Verbindung ausreichend.\",\n"
        "        )\n"
        "        scanner.startScan(durationMillis = 20_000L)\n"
        "    }\n",
        "scan message",
    )

    text = replace_once(
        text,
        '                "Der gespeicherte rechte Schuh ist noch nicht in Reichweite. Scan wird gestartet.",\n',
        '                "Noch kein einzelner Schuh in Reichweite. Die Suche wird gestartet; " +\n'
        '                    "ein zweiter Schuh ist nicht erforderlich.",\n',
        "missing shoe message",
    )

    text = replace_once(
        text,
        '                "Automatische Verbindung zum rechten Schuh ist eingeschaltet."\n',
        '                "Automatische Verbindung zum ersten passenden Schuh ist eingeschaltet."\n',
        "auto-connect enabled message",
    )

    text = replace_once(
        text,
        '                "Automatische Verbindung zum rechten Schuh ist ausgeschaltet."\n',
        '                "Automatische Einzelschuh-Verbindung ist ausgeschaltet."\n',
        "auto-connect disabled message",
    )

    text = replace_once(
        text,
        "    private fun connectInternal(device: DiscoveredDevice) {\n"
        "        scanner.stopScan()\n"
        "        val adapter = bluetoothManager?.adapter\n",
        "    private fun connectInternal(device: DiscoveredDevice) {\n"
        "        scanner.stopScan()\n"
        "        appendLog(\n"
        "            LogLevel.INFO,\n"
        "            \"SCHUH\",\n"
        "            \"${device.name ?: device.address} wird als einzelner aktiver Schuh verwendet.\",\n"
        "        )\n"
        "        val adapter = bluetoothManager?.adapter\n",
        "single active shoe connection",
    )

    text = replace_once(
        text,
        "    fun disconnect() {\n"
        "        autoConnectAttemptedForScan = true\n"
        "        inspector.disconnect()\n"
        "    }\n",
        "    fun disconnect() {\n"
        "        autoConnectAttemptedForScan = true\n"
        "        inspector.disconnect()\n"
        "        appendLog(\n"
        "            LogLevel.INFO,\n"
        "            \"SCHUH\",\n"
        "            \"Einzelschuh getrennt. Die App wartet nicht auf ein Paar.\",\n"
        "        )\n"
        "    }\n",
        "disconnect message",
    )

    VIEW_MODEL.write_text(text, encoding="utf-8")


def patch_json_export() -> None:
    text = JSON_EXPORT.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "import de.crazybatto.solelink.ble.LogEntry\n",
        "import de.crazybatto.solelink.ble.LogEntry\n"
        "import de.crazybatto.solelink.ble.SingleShoePolicy\n",
        "import single-shoe export policy",
    )

    text = replace_once(
        text,
        '.put("schemaVersion", 3)\n',
        '.put("schemaVersion", 4)\n',
        "export schema version",
    )

    text = replace_once(
        text,
        '.put("mode", "adaptive-shoe-control-preview"),\n',
        '.put("mode", "single-shoe-control-preview"),\n',
        "export application mode",
    )

    text = replace_once(
        text,
        "            .put(\n"
        "                \"privacyNotice\",\n",
        "            .put(\n"
        "                \"connectionPolicy\",\n"
        "                JSONObject()\n"
        "                    .put(\"mode\", SingleShoePolicy.CONNECTION_MODE)\n"
        "                    .put(\"pairRequired\", SingleShoePolicy.PAIR_REQUIRED)\n"
        "                    .put(\n"
        "                        \"minimumRequiredShoes\",\n"
        "                        SingleShoePolicy.MINIMUM_REQUIRED_SHOES,\n"
        "                    )\n"
        "                    .put(\n"
        "                        \"description\",\n"
        "                        \"Left or right shoe can be used independently; the second shoe is optional.\",\n"
        "                    ),\n"
        "            )\n"
        "            .put(\n"
        "                \"privacyNotice\",\n",
        "add connection policy",
    )

    text = replace_once(
        text,
        "            .put(\"reconnectAttempt\", reconnectAttempt)\n"
        "            .put(\"services\", JSONArray().apply {\n",
        "            .put(\"reconnectAttempt\", reconnectAttempt)\n"
        "            .put(\"pairRequired\", SingleShoePolicy.PAIR_REQUIRED)\n"
        "            .put(\n"
        "                \"singleShoeRequirementSatisfied\",\n"
        "                SingleShoePolicy.requirementSatisfied(\n"
        "                    if (status in setOf(\n"
        "                            de.crazybatto.solelink.ble.ConnectionStatus.CONNECTED,\n"
        "                            de.crazybatto.solelink.ble.ConnectionStatus.DISCOVERING,\n"
        "                            de.crazybatto.solelink.ble.ConnectionStatus.INSPECTING,\n"
        "                            de.crazybatto.solelink.ble.ConnectionStatus.READY,\n"
        "                        )\n"
        "                    ) 1 else 0,\n"
        "                ),\n"
        "            )\n"
        "            .put(\"services\", JSONArray().apply {\n",
        "add single-shoe connection state",
    )

    JSON_EXPORT.write_text(text, encoding="utf-8")


def main() -> None:
    patch_view_model()
    patch_json_export()
    print("Single-shoe connection logic patch applied successfully.")


if __name__ == "__main__":
    main()
