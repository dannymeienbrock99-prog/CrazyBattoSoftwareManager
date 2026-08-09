package de.crazybatto.solelink.util

import de.crazybatto.solelink.ble.DiscoveredDevice
import de.crazybatto.solelink.ble.GattState
import de.crazybatto.solelink.ble.KnownShoeRegistry
import de.crazybatto.solelink.ble.LogEntry
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

object JsonExport {
    fun build(
        devices: List<DiscoveredDevice>,
        gattState: GattState,
        logs: List<LogEntry>,
    ): String {
        val root = JSONObject()
            .put("schemaVersion", 2)
            .put("generatedAt", Instant.now().toString())
            .put(
                "application",
                JSONObject()
                    .put("name", "SoleLink")
                    .put("version", "0.4.0")
                    .put("mode", "adaptive-shoe-control-preview"),
            )
            .put(
                "knownRightShoe",
                JSONObject()
                    .put("label", "Mein rechter Schuh")
                    .put("advertisedName", KnownShoeRegistry.ADVERTISED_MODEL_NAME)
                    .put("address", KnownShoeRegistry.RIGHT_SHOE_ADDRESS)
                    .put("manufacturerId", KnownShoeRegistry.MANUFACTURER_ID)
                    .put("manufacturerIdHex", "0x0078"),
            )
            .put(
                "privacyNotice",
                "Bluetooth addresses and nearby-device metadata may identify hardware. " +
                    "Share this export only intentionally.",
            )
            .put("scanResults", JSONArray().apply {
                devices.forEach { put(it.toJson()) }
            })
            .put("connection", gattState.toJson())
            .put("logs", JSONArray().apply {
                logs.forEach { entry ->
                    put(
                        JSONObject()
                            .put("timestamp", Instant.ofEpochMilli(entry.timestampMillis).toString())
                            .put("level", entry.level.name)
                            .put("source", entry.source)
                            .put("message", entry.message),
                    )
                }
            })

        return root.toString(2)
    }

    private fun DiscoveredDevice.toJson(): JSONObject {
        val shoeMatch = KnownShoeRegistry.identify(this)
        return JSONObject()
            .put("address", address)
            .putNullable("name", name)
            .put("rssi", rssi)
            .putNullable("connectable", connectable)
            .put("lastSeen", Instant.ofEpochMilli(lastSeenMillis).toString())
            .put("serviceUuids", JSONArray(serviceUuids))
            .put("manufacturerData", JSONObject().apply {
                manufacturerData.forEach { (companyId, hex) ->
                    put(companyId.toString(), hex)
                }
            })
            .putNullable("rawScanRecordHex", rawScanRecordHex)
            .put(
                "shoeIdentification",
                shoeMatch?.let { match ->
                    JSONObject()
                        .put("matched", true)
                        .put("label", match.label)
                        .put("side", match.side.name)
                        .put("confidence", match.confidence.name)
                        .put("stored", match.stored)
                        .put("reason", match.reason)
                } ?: JSONObject().put("matched", false),
            )
    }

    private fun GattState.toJson(): JSONObject =
        JSONObject()
            .put("status", status.name)
            .putNullable("deviceName", deviceName)
            .putNullable("deviceAddress", deviceAddress)
            .put(
                "connectedToStoredRightShoe",
                deviceAddress?.equals(
                    KnownShoeRegistry.RIGHT_SHOE_ADDRESS,
                    ignoreCase = true,
                ) == true,
            )
            .putNullable("batteryPercent", batteryPercent)
            .putNullable("lastError", lastError)
            .put("services", JSONArray().apply {
                services.forEach { service ->
                    put(
                        JSONObject()
                            .put("uuid", service.uuid)
                            .put("primary", service.isPrimary)
                            .put("characteristics", JSONArray().apply {
                                service.characteristics.forEach { characteristic ->
                                    put(
                                        JSONObject()
                                            .put("uuid", characteristic.uuid)
                                            .put("properties", JSONArray(characteristic.properties))
                                            .put("permissions", characteristic.permissions)
                                            .putNullable("valueHex", characteristic.valueHex)
                                            .putNullable("valueAscii", characteristic.valueAscii)
                                            .put(
                                                "notificationsEnabled",
                                                characteristic.notificationsEnabled,
                                            )
                                            .put(
                                                "descriptors",
                                                JSONArray().apply {
                                                    characteristic.descriptors.forEach {
                                                        put(it.uuid)
                                                    }
                                                },
                                            ),
                                    )
                                }
                            }),
                    )
                }
            })

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)
}
