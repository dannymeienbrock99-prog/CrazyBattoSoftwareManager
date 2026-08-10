package de.crazybatto.solelink.ble

/**
 * Connection policy for the current app generation.
 *
 * The controller deliberately treats one reachable shoe as a complete usable session. A second
 * shoe is optional and can be connected later or handled in a separate session. This matches the
 * current single-GATT architecture and avoids blocking the user when only one shoe is powered on.
 */
object SingleShoePolicy {
    const val CONNECTION_MODE = "SINGLE_SHOE"
    const val MINIMUM_REQUIRED_SHOES = 1
    const val PAIR_REQUIRED = false

    fun chooseBestCandidate(devices: List<DiscoveredDevice>): DiscoveredDevice? =
        devices
            .asSequence()
            .filter { it.connectable != false }
            .filterNot(KnownShoeRegistry::isKnownNonShoe)
            .filter { KnownShoeRegistry.identify(it) != null }
            .sortedWith(
                compareByDescending<DiscoveredDevice> { KnownShoeRegistry.priority(it) }
                    .thenByDescending { it.rssi },
            )
            .firstOrNull()

    fun sideFor(device: DiscoveredDevice?): ShoeSide =
        device?.let(KnownShoeRegistry::identify)?.side ?: ShoeSide.UNKNOWN

    fun sideLabel(side: ShoeSide): String = when (side) {
        ShoeSide.LEFT -> "linker Schuh"
        ShoeSide.RIGHT -> "rechter Schuh"
        ShoeSide.UNKNOWN -> "einzelner Schuh"
    }

    fun requirementSatisfied(connectedShoeCount: Int): Boolean =
        connectedShoeCount >= MINIMUM_REQUIRED_SHOES
}
