package de.crazybatto.solelink.ble

enum class ShoeSide {
    LEFT,
    RIGHT,
    UNKNOWN,
}

enum class ShoeMatchConfidence {
    CONFIRMED,
    HIGH,
    POSSIBLE,
}

data class KnownShoeMatch(
    val label: String,
    val side: ShoeSide,
    val confidence: ShoeMatchConfidence,
    val stored: Boolean,
    val reason: String,
)

object KnownShoeRegistry {
    const val RIGHT_SHOE_ADDRESS = "C0:04:6F:A7:46:0D"
    const val ADVERTISED_MODEL_NAME = "005-BQ5397-001"
    const val MANUFACTURER_ID = 0x0078
    const val DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"

    fun identify(device: DiscoveredDevice): KnownShoeMatch? {
        val exactAddress = device.address.equals(RIGHT_SHOE_ADDRESS, ignoreCase = true)
        val exactModelName = device.name.equals(ADVERTISED_MODEL_NAME, ignoreCase = true)
        val manufacturerMatches = device.manufacturerData.containsKey(MANUFACTURER_ID)
        val deviceInfoAdvertised = device.serviceUuids.any {
            it.equals(DEVICE_INFORMATION_SERVICE, ignoreCase = true)
        }

        return when {
            exactAddress -> KnownShoeMatch(
                label = "Mein rechter Schuh",
                side = ShoeSide.RIGHT,
                confidence = ShoeMatchConfidence.CONFIRMED,
                stored = true,
                reason = "Gespeicherte Bluetooth-Adresse stimmt überein.",
            )

            exactModelName && manufacturerMatches -> KnownShoeMatch(
                label = "Passender BQ5397-Schuh",
                side = ShoeSide.UNKNOWN,
                confidence = ShoeMatchConfidence.HIGH,
                stored = false,
                reason = "Modellkennung und Herstellerkennung 0x0078 stimmen überein.",
            )

            exactModelName && deviceInfoAdvertised -> KnownShoeMatch(
                label = "Möglicher BQ5397-Schuh",
                side = ShoeSide.UNKNOWN,
                confidence = ShoeMatchConfidence.HIGH,
                stored = false,
                reason = "Modellkennung und Geräteinformationsdienst stimmen überein.",
            )

            exactModelName || (manufacturerMatches && deviceInfoAdvertised) -> KnownShoeMatch(
                label = "Möglicher Smart-Schuh",
                side = ShoeSide.UNKNOWN,
                confidence = ShoeMatchConfidence.POSSIBLE,
                stored = false,
                reason = "Ein Teil des bekannten Bluetooth-Profils stimmt überein.",
            )

            else -> null
        }
    }

    fun isStoredRightShoe(device: DiscoveredDevice): Boolean =
        device.address.equals(RIGHT_SHOE_ADDRESS, ignoreCase = true)

    fun isKnownNonShoe(device: DiscoveredDevice): Boolean =
        device.name.equals("App-RCTW", ignoreCase = true)

    fun priority(device: DiscoveredDevice): Int {
        val match = identify(device)
        return when {
            match?.stored == true -> 1_000
            match?.confidence == ShoeMatchConfidence.HIGH -> 700
            match?.confidence == ShoeMatchConfidence.POSSIBLE -> 400
            isKnownNonShoe(device) -> -100
            else -> 0
        }
    }
}
