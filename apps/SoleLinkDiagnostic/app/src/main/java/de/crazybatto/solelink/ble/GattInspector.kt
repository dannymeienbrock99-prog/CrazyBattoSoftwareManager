package de.crazybatto.solelink.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
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
    private var requestedDeviceName: String? = null
    private var explicitDisconnect = false

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
            when (newState) {
                android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                    bluetoothGatt = gatt
                    _state.value = _state.value.copy(
                        status = ConnectionStatus.CONNECTED,
                        deviceAddress = gatt.device.address,
                        deviceName = requestedDeviceName,
                        lastError = null,
                    )
                    log(
                        LogLevel.INFO,
                        "GATT",
                        "Verbunden mit ${requestedDeviceName ?: gatt.device.address}. Dienste werden gesucht.",
                    )
                    discoverServices(gatt)
                }

                android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                    cancelOperations()
                    val message = if (explicitDisconnect) {
                        "Bluetooth-Verbindung wurde getrennt."
                    } else {
                        "Bluetooth-Verbindung wurde unerwartet getrennt (Status $status)."
                    }
                    log(
                        if (explicitDisconnect) LogLevel.INFO else LogLevel.WARNING,
                        "GATT",
                        message,
                    )
                    _state.value = _state.value.copy(
                        status = ConnectionStatus.DISCONNECTED,
                        lastError = if (explicitDisconnect) null else message,
                    )
                    closeGatt(gatt)
                }
            }

            if (status != BluetoothGatt.GATT_SUCCESS &&
                newState != android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
            ) {
                fail("GATT-Verbindungsfehler: Status $status.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
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
            queueSafeInspection(gatt)
        }

        @Deprecated("Deprecated by Android; retained for Android 12 and lower.")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
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
            handleCharacteristicRead(characteristic, value, status)
        }

        @Deprecated("Deprecated by Android; retained for Android 12 and lower.")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
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
            handleCharacteristicChanged(characteristic, value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
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
        disconnect()
        explicitDisconnect = false
        requestedDeviceName = advertisedName
        _state.value = GattState(
            status = ConnectionStatus.CONNECTING,
            deviceName = advertisedName,
            deviceAddress = device.address,
        )
        log(
            LogLevel.INFO,
            "GATT",
            "Verbindung zu ${advertisedName ?: device.address} wird aufgebaut.",
        )

        try {
            bluetoothGatt = device.connectGatt(
                appContext,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE,
            )
            if (bluetoothGatt == null) {
                fail("Android konnte kein GATT-Verbindungsobjekt erstellen.")
            }
        } catch (securityException: SecurityException) {
            fail("Bluetooth-Berechtigung fehlt: ${securityException.message.orEmpty()}")
        } catch (exception: IllegalArgumentException) {
            fail("Ungültiges Bluetooth-Gerät: ${exception.message.orEmpty()}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val gatt = bluetoothGatt ?: return
        explicitDisconnect = true
        cancelOperations()
        try {
            gatt.disconnect()
        } catch (_: SecurityException) {
            closeGatt(gatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverServices(gatt: BluetoothGatt) {
        _state.value = _state.value.copy(status = ConnectionStatus.DISCOVERING)
        try {
            if (!gatt.discoverServices()) {
                fail("Android konnte die GATT-Dienstsuche nicht starten.")
            }
        } catch (securityException: SecurityException) {
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
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    pendingOperations.add(
                        GattOperation.Read(service.uuid, characteristic.uuid),
                    )
                }

                val supportsIndication =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                val supportsNotification =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                val hasCccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG) != null

                if (hasCccd && (supportsNotification || supportsIndication)) {
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
            handler.post(::runNextOperation)
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
        } catch (securityException: SecurityException) {
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
        handler.post(::runNextOperation)
    }

    private fun setReady() {
        currentOperation = null
        _state.value = _state.value.copy(status = ConnectionStatus.READY)
        log(
            LogLevel.INFO,
            "GATT",
            "Sichere Nur-Lese-Inspektion abgeschlossen. " +
                "Es wurden keine Schuh-Steuerbefehle gesendet.",
        )
    }

    private fun cancelOperations() {
        pendingOperations.clear()
        currentOperation = null
        operationToken++
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
        disconnect()
        bluetoothGatt?.let(::closeGatt)
        cancelOperations()
    }

    companion object {
        private val CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private const val OPERATION_TIMEOUT_MILLIS = 5_000L
    }
}
