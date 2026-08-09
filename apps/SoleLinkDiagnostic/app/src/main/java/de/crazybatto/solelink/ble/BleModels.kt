package de.crazybatto.solelink.ble

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    DATA,
}

data class LogEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val source: String,
    val message: String,
)

data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val connectable: Boolean?,
    val serviceUuids: List<String>,
    val manufacturerData: Map<Int, String>,
    val rawScanRecordHex: String?,
    val lastSeenMillis: Long = System.currentTimeMillis(),
)

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCOVERING,
    INSPECTING,
    READY,
    DISCONNECTED,
    ERROR,
}

data class DescriptorSnapshot(
    val uuid: String,
)

data class CharacteristicSnapshot(
    val serviceUuid: String,
    val uuid: String,
    val properties: List<String>,
    val permissions: Int,
    val valueHex: String? = null,
    val valueAscii: String? = null,
    val notificationsEnabled: Boolean = false,
    val descriptors: List<DescriptorSnapshot> = emptyList(),
)

data class ServiceSnapshot(
    val uuid: String,
    val isPrimary: Boolean,
    val characteristics: List<CharacteristicSnapshot>,
)

data class GattState(
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val services: List<ServiceSnapshot> = emptyList(),
    val batteryPercent: Int? = null,
    val lastError: String? = null,
)
