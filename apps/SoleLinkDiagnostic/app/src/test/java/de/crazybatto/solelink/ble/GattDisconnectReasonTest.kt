package de.crazybatto.solelink.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattDisconnectReasonTest {
    @Test
    fun status19IsRecognizedAsRetryablePeerTermination() {
        assertTrue(
            GattDisconnectReason.canRetryAutomatically(
                GattDisconnectReason.TERMINATED_BY_PEER,
            ),
        )
        assertTrue(
            GattDisconnectReason.finalMessage(
                status = GattDisconnectReason.TERMINATED_BY_PEER,
                knownShoe = true,
            ).contains("Status 19"),
        )
    }

    @Test
    fun localHostTerminationIsNotAutomaticallyRetried() {
        assertFalse(
            GattDisconnectReason.canRetryAutomatically(
                GattDisconnectReason.TERMINATED_BY_LOCAL_HOST,
            ),
        )
    }

    @Test
    fun retryMessageIncludesAttemptAndButtonAdvice() {
        val message = GattDisconnectReason.retryMessage(
            status = GattDisconnectReason.TERMINATED_BY_PEER,
            attempt = 1,
            maximumAttempts = 2,
            delayMillis = 2_000L,
        )

        assertTrue(message.contains("1/2"))
        assertTrue(message.contains("Seitentaste"))
    }
}
