package de.crazybatto.solelink.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.crazybatto.solelink.ble.BleScanner
import de.crazybatto.solelink.ble.ConnectionStatus
import de.crazybatto.solelink.ble.DiscoveredDevice
import de.crazybatto.solelink.ble.GattInspector
import de.crazybatto.solelink.ble.KnownShoeRegistry
import de.crazybatto.solelink.ble.LogEntry
import de.crazybatto.solelink.ble.LogLevel
import de.crazybatto.solelink.util.JsonExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _knownRightShoe = MutableStateFlow<DiscoveredDevice?>(null)
    val knownRightShoe: StateFlow<DiscoveredDevice?> = _knownRightShoe.asStateFlow()

    private val _autoConnectKnownRightShoe = MutableStateFlow(true)
    val autoConnectKnownRightShoe: StateFlow<Boolean> =
        _autoConnectKnownRightShoe.asStateFlow()

    private val scanner = BleScanner(application, ::appendLog)
    private val inspector = GattInspector(application, ::appendLog)
    private val bluetoothManager = application.getSystemService(BluetoothManager::class.java)

    private var autoConnectAttemptedForScan = false

    val devices = scanner.devices
    val isScanning = scanner.isScanning
    val gattState = inspector.state

    init {
        appendLog(
            LogLevel.INFO,
            "APP",
            "SoleLink Control gestartet. Der rechte Schuh ist fest hinterlegt.",
        )

        viewModelScope.launch {
            scanner.devices.collect { scannedDevices ->
                val knownShoe = scannedDevices.firstOrNull(KnownShoeRegistry::isStoredRightShoe)
                _knownRightShoe.value = knownShoe

                if (
                    knownShoe != null &&
                    _autoConnectKnownRightShoe.value &&
                    !autoConnectAttemptedForScan &&
                    inspector.state.value.status.canStartConnection()
                ) {
                    autoConnectAttemptedForScan = true
                    appendLog(
                        LogLevel.INFO,
                        "SCHUH",
                        "Mein rechter Schuh wurde gefunden und wird automatisch verbunden.",
                    )
                    connectInternal(knownShoe)
                }
            }
        }
    }

    fun startScan() {
        autoConnectAttemptedForScan = false
        scanner.startScan(durationMillis = 20_000L)
    }

    fun stopScan() = scanner.stopScan()

    fun clearDevices() = scanner.clear()

    @SuppressLint("MissingPermission")
    fun connect(device: DiscoveredDevice) {
        autoConnectAttemptedForScan = true
        connectInternal(device)
    }

    fun connectKnownRightShoe() {
        val shoe = _knownRightShoe.value
        if (shoe == null) {
            appendLog(
                LogLevel.INFO,
                "SCHUH",
                "Der gespeicherte rechte Schuh ist noch nicht in Reichweite. Scan wird gestartet.",
            )
            startScan()
            return
        }

        autoConnectAttemptedForScan = true
        connectInternal(shoe)
    }

    fun setAutoConnectKnownRightShoe(enabled: Boolean) {
        _autoConnectKnownRightShoe.value = enabled
        appendLog(
            LogLevel.INFO,
            "SCHUH",
            if (enabled) {
                "Automatische Verbindung zum rechten Schuh ist eingeschaltet."
            } else {
                "Automatische Verbindung zum rechten Schuh ist ausgeschaltet."
            },
        )

        if (enabled) {
            autoConnectAttemptedForScan = false
            val shoe = _knownRightShoe.value
            if (shoe != null && inspector.state.value.status.canStartConnection()) {
                autoConnectAttemptedForScan = true
                connectInternal(shoe)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectInternal(device: DiscoveredDevice) {
        scanner.stopScan()
        val adapter = bluetoothManager?.adapter
        if (adapter == null) {
            appendLog(LogLevel.ERROR, "GATT", "Bluetooth-Adapter ist nicht verfügbar.")
            return
        }

        try {
            inspector.connect(adapter.getRemoteDevice(device.address), device.name)
        } catch (securityException: SecurityException) {
            appendLog(
                LogLevel.ERROR,
                "GATT",
                "Bluetooth-Verbindungsberechtigung fehlt.",
            )
        } catch (exception: IllegalArgumentException) {
            appendLog(
                LogLevel.ERROR,
                "GATT",
                "Ungültige Geräteadresse: ${device.address}",
            )
        }
    }

    fun disconnect() {
        autoConnectAttemptedForScan = true
        inspector.disconnect()
    }

    fun clearLogs() {
        _logs.value = emptyList()
        appendLog(LogLevel.INFO, "APP", "Protokoll wurde geleert.")
    }

    fun notePreviewAction(message: String) {
        appendLog(LogLevel.INFO, "VORSCHAU", message)
    }

    fun exportJson(): String =
        JsonExport.build(
            devices = devices.value,
            gattState = gattState.value,
            logs = logs.value,
        )

    fun notePermissionResult(granted: Boolean) {
        appendLog(
            if (granted) LogLevel.INFO else LogLevel.WARNING,
            "PERMISSION",
            if (granted) {
                "Bluetooth-Berechtigungen wurden erteilt."
            } else {
                "Nicht alle Bluetooth-Berechtigungen wurden erteilt."
            },
        )
    }

    private fun appendLog(level: LogLevel, source: String, message: String) {
        _logs.update { current ->
            (current + LogEntry(level = level, source = source, message = message))
                .takeLast(MAX_LOG_ENTRIES)
        }
    }

    override fun onCleared() {
        scanner.close()
        inspector.close()
        super.onCleared()
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 1_000
    }
}

private fun ConnectionStatus.canStartConnection(): Boolean = this in setOf(
    ConnectionStatus.IDLE,
    ConnectionStatus.DISCONNECTED,
    ConnectionStatus.ERROR,
)
