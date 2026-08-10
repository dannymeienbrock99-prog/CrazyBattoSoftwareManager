package de.crazybatto.solelink.ble

object GattDisconnectReason {
    const val CONNECTION_TIMEOUT = 0x08
    const val TERMINATED_BY_PEER = 0x13
    const val TERMINATED_BY_LOCAL_HOST = 0x16
    const val GENERIC_ANDROID_GATT_ERROR = 133

    fun canRetryAutomatically(status: Int): Boolean = status in setOf(
        CONNECTION_TIMEOUT,
        TERMINATED_BY_PEER,
        GENERIC_ANDROID_GATT_ERROR,
    )

    fun retryMessage(
        status: Int,
        attempt: Int,
        maximumAttempts: Int,
        delayMillis: Long,
    ): String {
        val seconds = (delayMillis / 1_000L).coerceAtLeast(1L)
        return when (status) {
            TERMINATED_BY_PEER ->
                "Der Schuh hat die Bluetooth-Verbindung beendet (Status 19). " +
                    "Drücke jetzt kurz eine Seitentaste. Wiederverbindung " +
                    "$attempt/$maximumAttempts in $seconds Sekunden."

            CONNECTION_TIMEOUT ->
                "Die Verbindung zum Schuh ist abgelaufen (Status 8). " +
                    "Wiederverbindung $attempt/$maximumAttempts in $seconds Sekunden."

            GENERIC_ANDROID_GATT_ERROR ->
                "Android hat einen allgemeinen Bluetooth-GATT-Fehler gemeldet (Status 133). " +
                    "Wiederverbindung $attempt/$maximumAttempts in $seconds Sekunden."

            else ->
                "Bluetooth-Verbindung unterbrochen (Status $status). " +
                    "Wiederverbindung $attempt/$maximumAttempts in $seconds Sekunden."
        }
    }

    fun finalMessage(status: Int, knownShoe: Boolean): String = when (status) {
        TERMINATED_BY_PEER -> if (knownShoe) {
            "Der Schuh hat die Verbindung selbst beendet (Status 19 / 0x13). " +
                "Meist ist er noch mit einer anderen App oder einem alten Bluetooth-Schlüssel " +
                "verbunden oder nicht im Wiederverbindungsmodus. Schließe die frühere Adapt-App, " +
                "drücke eine Seitentaste und starte den Scan erneut."
        } else {
            "Das Bluetooth-Gerät hat die Verbindung selbst beendet (Status 19 / 0x13)."
        }

        CONNECTION_TIMEOUT ->
            "Die Bluetooth-Verbindung ist abgelaufen (Status 8). Gerät näher ans Handy legen und erneut versuchen."

        TERMINATED_BY_LOCAL_HOST ->
            "Android hat die Bluetooth-Verbindung beendet (Status 22 / 0x16)."

        GENERIC_ANDROID_GATT_ERROR ->
            "Android meldet einen allgemeinen Bluetooth-GATT-Fehler (Status 133). Bluetooth kurz aus- und wieder einschalten."

        0 ->
            "Die Bluetooth-Verbindung wurde von der Gegenstelle ohne zusätzlichen Fehlercode beendet."

        else ->
            "Bluetooth-Verbindung wurde getrennt (Status $status)."
    }
}
