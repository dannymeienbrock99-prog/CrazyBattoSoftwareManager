package de.crazybatto.solelink.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import de.crazybatto.solelink.util.toHexString
import de.crazybatto.solelink.util.toPrintableAscii
import java.util.ArrayDeque
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GattInspector(
    context: Context,
    private val log: (LogLevel, String, String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(GattState())
    val state: StateFlow<GattState> = _state.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var requestedDevice: BluetoothDevice? = null
    private var requestedDeviceName: String? = null
    private var explicitDisconnect = false
    private var reconnectAttempts = 0
    private var reconnectRunnable: Runnable? = null
    private var connectionEpoch = 0L
    private var conservativeShoeInspection = false

    private val pendingOperations = ArrayDeque<GattOperation>()
    private var currentOperation: GattOperation? = null
    private var operationToken = 0L

    private sealed interface GattOperation {
        val serviceUuid: UUID
        val characteristicUuid: UUID

        data class Read(
            override val serviceUuid: UUID,
            override val characteristicUuid: UUID,
        ) : GattOperation

        data class EnableNotifications(
            override val serviceUuid: UUID,
            override val characteristicUuid: UUID,
            val indication: Boolean,
        ) : GattOperation
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) {
                closeGatt(gatt)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> handleConnected(gatt, status)
                BluetoothProfile.STATE_DISCONNECTED -> handleDisconnected(gatt, status)
                else -> if (status != BluetoothGatt.GATT_SUCCESS) {
                    fail("GATT-Verbindungsfehler: Status $status.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail("GATT-Dienste konnten nicht gelesen werden: Status $status.")
                return
            }

            val services = snapshotServices(gatt.services)
            _state.value = _state.value.copy(
                status = ConnectionStatus.INSPECTING,
                services = services,
                lastError = null,
            )
            log(
                LogLevel.INFO,
                "GATT",
                "${services.size} Dienste mit " +
                    "${services.sumOf { it.characteristics.size }} Merkmalen gefunden.",
            )

            if (conservativeShoeInspection) {
                log(
                    LogLevel.INFO,
                    "SCHUH",
                    "Schonender Inspektionsmodus aktiv: Es werden nur sichere Standardwerte " +
                        "gelesen und keine proprietären Benachrichtigungen aktiviert.",
                )
            }

            queueSafeInspection(gatt)
        }

        @Deprecated("Deprecated by Android; retained for Android 12 and lower.")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (gatt !== bluetoothGatt) return
            handleCharacteristicRead(
                characteristic = characteristic,
                value = characteristic.value ?: byteArrayOf(),
                status = status,
            )
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (gatt !== bluetoothGatt) return
            handleCharacteristicRead(characteristic, value, status)
        }

        @Deprecated("Deprecated by Android; retained for Android 12 and lower.")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (gatt !== bluetoothGatt) return
            handleCharacteristicChanged(
                characteristic,
                characteristic.value ?: byteArrayOf(),
            )
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (gatt !== bluetoothGatt) return
            handleCharacteristicChanged(characteristic, value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (gatt !== bluetoothGatt) return

            val operation = currentOperation
            if (operation is GattOperation.EnableNotifications) {
                val enabled = status == BluetoothGatt.GATT_SUCCESS
                updateCharacteristic(
                    serviceUuid = operation.serviceUuid,
                    characteristicUuid = operation.characteristicUuid,
                    notificationsEnabled = enabled,
                )
                log(
                    if (enabled) LogLevel.INFO else LogLevel.WARNING,
                    "GATT",
                    if (enabled) {
                        "Benachrichtigungen aktiviert: ${operation.characteristicUuid}"
                    } else {
                        "Benachrichtigungen konnten nicht aktiviert werden: " +
                            "${operation.characteristicUuid} (Status $status)"
                    },
                )
                completeOperation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, advertisedName: String?) {
        requestedDevice = device
        requestedDeviceName = advertisedName
        explicitDisconnect = false
        reconnectAttempts = 0
        cancelReconnect()
        beginConnection(device, advertisedName, isRetry = false)
    }

    @SuppressLint("MissingPermission")
    private fun beginConnection(
        device: BluetoothDevice,
        advertisedName: String?,
        isRetry: Boolean,
    ) {
        cancelReconnect()
        closeCurrentGattForReplacement()
        cancelOperations()

        connectionEpoch++
        conservativeShoeInspection = device.address.equals(
            KnownShoeRegistry.RIGHT_SHOE_ADDRESS,
            ignoreCase = true,
        ) || advertisedName?.contains(
            KnownShoeRegistry.ADVERTISED_MODEL_NAME,
            ignoreCase = true,
        ) == true

        _state.value = GattState(
            status = ConnectionStatus.CONNECTING,
            deviceName = advertisedName,
            deviceAddress = device.address,
            lastError = if (isRetry) {
                "Wiederverbindung zum Schuh wird aufgebaut."
            } else {
                null
            },
            reconnectAttempt = reconnectAttempts,
        )

        val bondText = when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> "gekoppelt"
            BluetoothDevice.BOND_BONDING -> "Kopplung läuft"
            else -> "nicht systemgekoppelt"
        }
        log(
            LogLevel.INFO,
            "GATT",
            "Verbindung zu ${advertisedName ?: device.address} wird aufgebaut " +
                "($bondText${if (isRetry) ", Wiederholungsversuch $reconnectAttempts" else ""}).",
        )

        try {
            val newGatt = device.connectGatt(
                appContext,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE,
            )
            bluetoothGatt = newGatt
            if (newGatt == null) {
                fail("Android konnte kein GATT-Verbindungsobjekt erstellen.")
            }
        } catch (securityException: SecurityException) {
            fail("Bluetooth-Berechtigung fehlt: ${securityException.message.orEmpty()}")
        } catch (exception: IllegalArgumentException) {
            fail("Ungültiges Bluetooth-Gerät: ${exception.message.orEmpty()}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnected(gatt: BluetoothGatt, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            handleDisconnected(gatt, status)
            return
        }

        _state.value = _state.value.copy(
            status = ConnectionStatus.CONNECTED,
            deviceAddress = gatt.device.address,
            deviceName = requestedDeviceName,
            lastError = null,
            lastDisconnectStatus = null,
            reconnectAttempt = reconnectAttempts,
        )
        log(
            LogLevel.INFO,
            "GATT",
            "Verbunden mit ${requestedDeviceName ?: gatt.device.address}. " +
                "Die Verbindung stabilisiert sich kurz vor der Dienstsuche.",
        )

        try {
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
        } catch (_: SecurityException) {
            // Die Verbindung funktioniert auch ohne Prioritätswechsel weiter.
        }

        val epoch = connectionEpoch
        val delay = if (conservativeShoeInspection) {
            SHOE_SERVICE_DISCOVERY_DELAY_MILLIS
        } else {
            DEFAULT_SERVICE_DISCOVERY_DELAY_MILLIS
        }
        handler.postDelayed(
            {
                if (
                    epoch == connectionEpoch &&
                    gatt === bluetoothGatt &&
                    _state.value.status == ConnectionStatus.CONNECTED
                ) {
                    discoverServices(gatt)
                }
            },
            delay,
        )
    }

    private fun handleDisconnected(gatt: BluetoothGatt, status: Int) {
        val userRequestedDisconnect = explicitDisconnect
        cancelOperations()
        connectionEpoch++
        closeGatt(gatt)

        if (userRequestedDisconnect) {
            log(LogLevel.INFO, "GATT", "Bluetooth-Verbindung wurde getrennt.")
            _state.value = _state.value.copy(
                status = ConnectionStatus.DISCONNECTED,
                lastError = null,
                lastDisconnectStatus = status,
                reconnectAttempt = 0,
            )
            return
        }

        val shouldRetry = conservativeShoeInspection &&
            GattDisconnectReason.canRetryAutomatically(status) &&
            reconnectAttempts < MAX_RECONNECT_ATTEMPTS &&
            requestedDevice != null

        if (shouldRetry) {
            scheduleReconnect(status)
            return
        }

        val message = GattDisconnectReason.finalMessage(
            status = status,
            knownShoe = conservativeShoeInspection,
        )
        log(LogLevel.WARNING, "GATT", message)
        _state.value = _state.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            lastError = message,
            lastDisconnectStatus = status,
            reconnectAttempt = reconnectAttempts,
        )
    }

    private fun scheduleReconnect(status: Int) {
        val device = requestedDevice ?: return
        reconnectAttempts++
        val delay = RECONNECT_DELAYS_MILLIS[
            (reconnectAttempts - 1).coerceIn(RECONNECT_DELAYS_MILLIS.indices)
        ]
        val message = GattDisconnectReason.retryMessage(
            status = status,
            attempt = reconnectAttempts,
            maximumAttempts = MAX_RECONNECT_ATTEMPTS,
            delayMillis = delay,
        )

        log(LogLevel.WARNING, "GATT", message)
        _state.value = _state.value.copy(
            status = ConnectionStatus.CONNECTING,
            lastError = message,
            lastDisconnectStatus = status,
            reconnectAttempt = reconnectAttempts,
        )

        cancelReconnect()
        val scheduledEpoch = ++connectionEpoch
        reconnectRunnable = Runnable {
            reconnectRunnable = null
            if (
                explicitDisconnect ||
                scheduledEpoch != connectionEpoch ||
                requestedDevice?.address != device.address
            ) {
                return@Runnable
            }
            beginConnection(device, requestedDeviceName, isRetry = true)
        }.also { runnable ->
            handler.postDelayed(runnable, delay)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        explicitDisconnect = true
        reconnectAttempts = 0
        cancelReconnect()
        cancelOperations()
        connectionEpoch++

        val gatt = bluetoothGatt
        bluetoothGatt = null
        if (gatt != null) {
            try {
                gatt.disconnect()
            } catch (_: SecurityException) {
                // Das Objekt wird unten trotzdem geschlossen.
            }
            closeGatt(gatt)
        }

        _state.value = _state.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            lastError = null,
            reconnectAttempt = 0,
        )
        log(LogLevel.INFO, "GATT", "Bluetooth-Verbindung wurde getrennt.")
    }

    @SuppressLint("MissingPermission")
    private fun closeCurrentGattForReplacement() {
        val previousGatt = bluetoothGatt ?: return
        bluetoothGatt = null
        try {
            previousGatt.disconnect()
        } catch (_: SecurityException) {
            // Direktes Schließen verhindert veraltete Callback-Rennen.
        }
        closeGatt(previousGatt)
    }

    @SuppressLint("MissingPermission")
    private fun discoverServices(gatt: BluetoothGatt) {
        if (gatt !== bluetoothGatt) return

        _state.value = _state.value.copy(status = ConnectionStatus.DISCOVERING)
        try {
            if (!gatt.discoverServices()) {
                fail("Android konnte die GATT-Dienstsuche nicht starten.")
            }
        } catch (_: SecurityException) {
            fail("Dienstsuche ohne Bluetooth-Berechtigung nicht möglich.")
        }
    }

    private fun snapshotServices(services: List<BluetoothGattService>): List<ServiceSnapshot> =
        services.map { service ->
            ServiceSnapshot(
                uuid = service.uuid.toString(),
                isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
                characteristics = service.characteristics.map { characteristic ->
                    CharacteristicSnapshot(
                        serviceUuid = service.uuid.toString(),
                        uuid = characteristic.uuid.toString(),
                        properties = propertyLabels(characteristic.properties),
                        permissions = characteristic.permissions,
                        descriptors = characteristic.descriptors.map { descriptor ->
                            DescriptorSnapshot(descriptor.uuid.toString())
                        },
                    )
                },
            )
        }

    private fun queueSafeInspection(gatt: BluetoothGatt) {
        cancelOperations()
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val readable =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                val safeRead = !conservativeShoeInspection ||
                    characteristic.uuid in SAFE_SHOE_READ_CHARACTERISTICS

                if (readable && safeRead) {
                    pendingOperations.add(
                        GattOperation.Read(service.uuid, characteristic.uuid),
                    )
                }

                val supportsIndication =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                val supportsNotification =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                val hasCccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG) != null
                val safeNotification = !conservativeShoeInspection ||
                    characteristic.uuid == BATTERY_LEVEL_CHARACTERISTIC

                if (
                    hasCccd &&
                    safeNotification &&
                    (supportsNotification || supportsIndication)
                ) {
                    pendingOperations.add(
                        GattOperation.EnableNotifications(
                            serviceUuid = service.uuid,
                            characteristicUuid = characteristic.uuid,
                            indication = supportsIndication,
                        ),
                    )
                }
            }
        }

        if (pendingOperations.isEmpty()) {
            setReady()
        } else {
            runNextOperation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun runNextOperation() {
        if (currentOperation != null) return

        val gatt = bluetoothGatt
        if (gatt == null) {
            cancelOperations()
            return
        }

        val operation = if (pendingOperations.isEmpty()) null else pendingOperations.removeFirst()
        if (operation == null) {
            setReady()
            return
        }

        val service = gatt.getService(operation.serviceUuid)
        val characteristic = service?.getCharacteristic(operation.characteristicUuid)
        if (characteristic == null) {
            log(
                LogLevel.WARNING,
                "GATT",
                "Merkmal ${operation.characteristicUuid} ist nicht mehr verfügbar.",
            )
            scheduleNextOperation()
            return
        }

        currentOperation = operation
        scheduleOperationTimeout(operation)

        val started = try {
            when (operation) {
                is GattOperation.Read -> gatt.readCharacteristic(characteristic)
                is GattOperation.EnableNotifications ->
                    enableNotifications(gatt, characteristic, operation.indication)
            }
        } catch (_: SecurityException) {
            log(
                LogLevel.ERROR,
                "GATT",
                "Bluetooth-Berechtigung während der Inspektion verloren.",
            )
            false
        }

        if (!started) {
            log(
                LogLevel.WARNING,
                "GATT",
                "Operation konnte nicht gestartet werden: ${operation.characteristicUuid}",
            )
            completeOperation()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        indication: Boolean,
    ): Boolean {
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return false
        }

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            ?: return false
        val descriptorValue = if (indication) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, descriptorValue) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = descriptorValue
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun handleCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val serviceUuid = characteristic.service?.uuid ?: currentOperation?.serviceUuid
            if (serviceUuid != null) {
                updateCharacteristic(
                    serviceUuid = serviceUuid,
                    characteristicUuid = characteristic.uuid,
                    value = value,
                )
            }
            log(
                LogLevel.DATA,
                "READ ${characteristic.uuid}",
                "${value.toHexString()} | ${value.toPrintableAscii()}",
            )
        } else {
            log(
                LogLevel.WARNING,
                "GATT",
                "Lesen von ${characteristic.uuid} fehlgeschlagen (Status $status).",
            )
        }
        completeOperation()
    }

    private fun handleCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        val serviceUuid = characteristic.service?.uuid ?: return
        updateCharacteristic(
            serviceUuid = serviceUuid,
            characteristicUuid = characteristic.uuid,
            value = value,
        )
        log(
            LogLevel.DATA,
            "NOTIFY ${characteristic.uuid}",
            "${value.toHexString()} | ${value.toPrintableAscii()}",
        )
    }

    private fun updateCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        value: ByteArray? = null,
        notificationsEnabled: Boolean? = null,
    ) {
        val current = _state.value
        var batteryPercent = current.batteryPercent

        val firstValueByte = value?.firstOrNull()
        if (characteristicUuid == BATTERY_LEVEL_CHARACTERISTIC && firstValueByte != null) {
            batteryPercent = firstValueByte.toInt() and 0xFF
        }

        val services = current.services.map { service ->
            if (service.uuid != serviceUuid.toString()) {
                service
            } else {
                service.copy(
                    characteristics = service.characteristics.map { characteristic ->
                        if (characteristic.uuid != characteristicUuid.toString()) {
                            characteristic
                        } else {
                            characteristic.copy(
                                valueHex = value?.toHexString() ?: characteristic.valueHex,
                                valueAscii = value?.toPrintableAscii()
                                    ?: characteristic.valueAscii,
                                notificationsEnabled = notificationsEnabled
                                    ?: characteristic.notificationsEnabled,
                            )
                        }
                    },
                )
            }
        }

        _state.value = current.copy(
            services = services,
            batteryPercent = batteryPercent,
        )
    }

    private fun propertyLabels(properties: Int): List<String> = buildList {
        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) add("BROADCAST")
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            add("WRITE_NO_RESPONSE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) {
            add("SIGNED_WRITE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) {
            add("EXTENDED")
        }
    }

    private fun scheduleOperationTimeout(operation: GattOperation) {
        val token = ++operationToken
        handler.postDelayed(
            {
                if (token == operationToken && currentOperation == operation) {
                    log(
                        LogLevel.WARNING,
                        "GATT",
                        "Zeitüberschreitung bei ${operation.characteristicUuid}.",
                    )
                    completeOperation()
                }
            },
            OPERATION_TIMEOUT_MILLIS,
        )
    }

    private fun completeOperation() {
        operationToken++
        currentOperation = null
        scheduleNextOperation()
    }

    private fun scheduleNextOperation() {
        val delay = if (conservativeShoeInspection) {
            SHOE_OPERATION_DELAY_MILLIS
        } else {
            DEFAULT_OPERATION_DELAY_MILLIS
        }
        handler.postDelayed({ runNextOperation() }, delay)
    }

    @SuppressLint("MissingPermission")
    private fun setReady() {
        currentOperation = null
        reconnectAttempts = 0
        _state.value = _state.value.copy(
            status = ConnectionStatus.READY,
            lastError = null,
            lastDisconnectStatus = null,
            reconnectAttempt = 0,
        )

        bluetoothGatt?.let { gatt ->
            try {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
            } catch (_: SecurityException) {
                // Optionaler Optimierungsschritt.
            }
        }

        log(
            LogLevel.INFO,
            "GATT",
            if (conservativeShoeInspection) {
                "Schuhverbindung ist bereit. Sichere Standardwerte wurden gelesen; " +
                    "proprietäre Steuerbefehle bleiben gesperrt."
            } else {
                "Sichere Nur-Lese-Inspektion abgeschlossen. " +
                    "Es wurden keine Steuerbefehle gesendet."
            },
        )
    }

    private fun cancelOperations() {
        pendingOperations.clear()
        currentOperation = null
        operationToken++
    }

    private fun cancelReconnect() {
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
    }

    private fun fail(message: String) {
        log(LogLevel.ERROR, "GATT", message)
        _state.value = _state.value.copy(
            status = ConnectionStatus.ERROR,
            lastError = message,
        )
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(gatt: BluetoothGatt) {
        try {
            gatt.close()
        } catch (_: SecurityException) {
            // Closing should not require additional user interaction.
        }
        if (bluetoothGatt === gatt) {
            bluetoothGatt = null
        }
    }

    fun close() {
        explicitDisconnect = true
        cancelReconnect()
        disconnect()
        requestedDevice = null
        requestedDeviceName = null
        cancelOperations()
    }

    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        private val SAFE_SHOE_READ_CHARACTERISTICS = setOf(
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"), // Device Name
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"), // Battery Level
            UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb"), // System ID
            UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"), // Model Number
            UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"), // Serial Number
            UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"), // Firmware Revision
            UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), // Manufacturer Name
            UUID.fromString("00002a50-0000-1000-8000-00805f9b34fb"), // PnP ID
        )

        private val RECONNECT_DELAYS_MILLIS = longArrayOf(2_000L, 5_000L)
        private const val MAX_RECONNECT_ATTEMPTS = 2
        private const val DEFAULT_SERVICE_DISCOVERY_DELAY_MILLIS = 250L
        private const val SHOE_SERVICE_DISCOVERY_DELAY_MILLIS = 900L
        private const val DEFAULT_OPERATION_DELAY_MILLIS = 35L
        private const val SHOE_OPERATION_DELAY_MILLIS = 180L
        private const val OPERATION_TIMEOUT_MILLIS = 5_000L
    }
}
