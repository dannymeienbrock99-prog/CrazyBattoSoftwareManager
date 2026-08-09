package de.crazybatto.solelink.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownShoeRegistryTest {
    @Test
    fun exactStoredAddressIsConfirmedRightShoe() {
        val device = device(
            address = KnownShoeRegistry.RIGHT_SHOE_ADDRESS,
            name = KnownShoeRegistry.ADVERTISED_MODEL_NAME,
            manufacturerData = mapOf(KnownShoeRegistry.MANUFACTURER_ID to "AF 28"),
        )

        val match = KnownShoeRegistry.identify(device)

        assertEquals(ShoeSide.RIGHT, match?.side)
        assertEquals(ShoeMatchConfidence.CONFIRMED, match?.confidence)
        assertTrue(match?.stored == true)
    }

    @Test
    fun matchingModelAndManufacturerIsHighConfidenceCandidate() {
        val device = device(
            address = "AA:BB:CC:DD:EE:FF",
            name = KnownShoeRegistry.ADVERTISED_MODEL_NAME,
            manufacturerData = mapOf(KnownShoeRegistry.MANUFACTURER_ID to "00"),
        )

        val match = KnownShoeRegistry.identify(device)

        assertEquals(ShoeMatchConfidence.HIGH, match?.confidence)
        assertEquals(ShoeSide.UNKNOWN, match?.side)
    }

    @Test
    fun unrelatedRctwDeviceIsNotShoe() {
        val device = device(
            address = "43:78:4A:E4:3B:10",
            name = "App-RCTW",
        )

        assertNull(KnownShoeRegistry.identify(device))
        assertTrue(KnownShoeRegistry.isKnownNonShoe(device))
    }

    @Test
    fun storedRightShoeSortsAboveUnrelatedDevice() {
        val stored = device(
            address = KnownShoeRegistry.RIGHT_SHOE_ADDRESS,
            name = KnownShoeRegistry.ADVERTISED_MODEL_NAME,
        )
        val unrelated = device(
            address = "43:78:4A:E4:3B:10",
            name = "App-RCTW",
        )

        assertTrue(KnownShoeRegistry.priority(stored) > KnownShoeRegistry.priority(unrelated))
    }

    private fun device(
        address: String,
        name: String?,
        manufacturerData: Map<Int, String> = emptyMap(),
    ) = DiscoveredDevice(
        address = address,
        name = name,
        rssi = -55,
        connectable = true,
        serviceUuids = listOf(KnownShoeRegistry.DEVICE_INFORMATION_SERVICE),
        manufacturerData = manufacturerData,
        rawScanRecordHex = null,
    )
}
