package de.crazybatto.solelink.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import de.crazybatto.solelink.ble.BleScanner
import de.crazybatto.solelink.ble.DiscoveredDevice
import de.crazybatto.solelink.ble.GattInspector
import de.crazybatto.solelink.ble.LogEntry
import de.crazybatto.solelink.ble.LogLevel
import de.crazybatto.solelink.util.JsonExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val scanner = BleScanner(application, ::appendLog)
    private val inspector = GattInspector(application, ::appendLog)
    private val bluetoothManager = application.getSystemService(BluetoothManager::class.java)

    val devices = scanner.devices
    val isScanning = scanner.isScanning
    val gattState = inspector.state

    init {
        appendLog(
            LogLevel.INFO,
            "APP",
            "Nur-Lese-Diagnose gestartet. Unbekannte Steuerbefehle sind deaktiviert.",
        )
    }

    fun startScan() = scanner.startScan()

    fun stopScan() = scanner.stopScan()

    fun clearDevices() = scanner.clear()

    @SuppressLint("MissingPermission")
    fun connect(device: DiscoveredDevice) {
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

    fun disconnect() = inspector.disconnect()

    fun clearLogs() {
        _logs.value = emptyList()
        appendLog(LogLevel.INFO, "APP", "Protokoll wurde geleert.")
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
