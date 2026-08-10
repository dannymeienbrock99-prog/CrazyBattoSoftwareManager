package de.crazybatto.solelink.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleShoePolicyTest {
    @Test
    fun oneRecognizedShoeIsEnough() {
        assertFalse(SingleShoePolicy.PAIR_REQUIRED)
        assertEquals(1, SingleShoePolicy.MINIMUM_REQUIRED_SHOES)
        assertTrue(SingleShoePolicy.requirementSatisfied(1))
        assertFalse(SingleShoePolicy.requirementSatisfied(0))
    }

    @Test
    fun storedRightShoeIsChosenWithoutSecondShoe() {
        val right = shoe(
            address = KnownShoeRegistry.RIGHT_SHOE_ADDRESS,
            rssi = -70,
        )
        val selected = SingleShoePolicy.chooseBestCandidate(listOf(right))

        assertEquals(KnownShoeRegistry.RIGHT_SHOE_ADDRESS, selected?.address)
        assertEquals(ShoeSide.RIGHT, SingleShoePolicy.sideFor(selected))
    }

    @Test
    fun secondMatchingShoeCanBeUsedAlone() {
        val otherShoe = shoe(
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -48,
        )
        val selected = SingleShoePolicy.chooseBestCandidate(listOf(otherShoe))

        assertEquals(otherShoe.address, selected?.address)
        assertEquals(ShoeSide.UNKNOWN, SingleShoePolicy.sideFor(selected))
    }

    @Test
    fun unrelatedDeviceIsNotAutoSelected() {
        val unrelated = DiscoveredDevice(
            address = "43:78:4A:E4:3B:10",
            name = "App-RCTW",
            rssi = -20,
            connectable = true,
            serviceUuids = emptyList(),
            manufacturerData = emptyMap(),
            rawScanRecordHex = null,
        )

        assertNull(SingleShoePolicy.chooseBestCandidate(listOf(unrelated)))
    }

    private fun shoe(address: String, rssi: Int) = DiscoveredDevice(
        address = address,
        name = KnownShoeRegistry.ADVERTISED_MODEL_NAME,
        rssi = rssi,
        connectable = true,
        serviceUuids = listOf(KnownShoeRegistry.DEVICE_INFORMATION_SERVICE),
        manufacturerData = mapOf(KnownShoeRegistry.MANUFACTURER_ID to "AF 28"),
        rawScanRecordHex = null,
    )
}
