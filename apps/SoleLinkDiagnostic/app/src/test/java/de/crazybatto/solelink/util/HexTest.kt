package de.crazybatto.solelink.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HexTest {
    @Test
    fun convertsBytesToUppercaseHex() {
        assertEquals("00 0F A5 FF", byteArrayOf(0, 15, 0xA5.toByte(), 0xFF.toByte()).toHexString())
    }

    @Test
    fun convertsNonPrintableBytesToDots() {
        assertEquals("A..z", byteArrayOf(65, 0, 31, 122).toPrintableAscii())
    }
}
