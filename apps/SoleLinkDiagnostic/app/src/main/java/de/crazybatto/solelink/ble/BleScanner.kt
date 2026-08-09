package de.crazybatto.solelink.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import de.crazybatto.solelink.util.toHexString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BleScanner(
    context: Context,
    private val log: (LogLevel, String, String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var stopRunnable: Runnable? = null
    private val announcedKnownDevices = mutableSetOf<String>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleResult)
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            log(
                LogLevel.ERROR,
                "SCAN",
                "Bluetooth-Scan fehlgeschlagen (Fehlercode $errorCode).",
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(durationMillis: Long = 12_000L) {
        if (_isScanning.value) return

        val adapter = bluetoothManager?.adapter
        if (adapter == null) {
            log(LogLevel.ERROR, "SCAN", "Dieses Gerät besitzt keinen Bluetooth-Adapter.")
            return
        }
        if (!adapter.isEnabled) {
            log(LogLevel.WARNING, "SCAN", "Bluetooth ist ausgeschaltet.")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            log(LogLevel.ERROR, "SCAN", "Bluetooth-LE-Scanner ist nicht verfügbar.")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        stopRunnable?.let(handler::removeCallbacks)
        stopRunnable = Runnable { stopScan() }.also {
            handler.postDelayed(it, durationMillis)
        }

        try {
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
            log(LogLevel.INFO, "SCAN", "BLE-Scan für ${durationMillis / 1000} Sekunden gestartet.")
        } catch (securityException: SecurityException) {
            _isScanning.value = false
            log(
                LogLevel.ERROR,
                "SCAN",
                "Bluetooth-Berechtigung fehlt: ${securityException.message.orEmpty()}",
            )
        } catch (exception: IllegalStateException) {
            _isScanning.value = false
            log(LogLevel.ERROR, "SCAN", "Scan konnte nicht gestartet werden: ${exception.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        stopRunnable?.let(handler::removeCallbacks)
        stopRunnable = null

        if (!_isScanning.value) return

        try {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (securityException: SecurityException) {
            log(
                LogLevel.ERROR,
                "SCAN",
                "Scan konnte wegen fehlender Berechtigung nicht beendet werden.",
            )
        } finally {
            _isScanning.value = false
            log(LogLevel.INFO, "SCAN", "BLE-Scan beendet.")
        }
    }

    fun clear() {
        _devices.value = emptyList()
        announcedKnownDevices.clear()
        log(LogLevel.INFO, "SCAN", "Gefundene Geräte wurden aus der Liste entfernt.")
    }

    @SuppressLint("MissingPermission")
    private fun handleResult(result: ScanResult) {
        val record = result.scanRecord
        val manufacturerData = buildMap {
            val data = record?.manufacturerSpecificData
            if (data != null) {
                for (index in 0 until data.size()) {
                    put(data.keyAt(index), data.valueAt(index).toHexString())
                }
            }
        }

        val scannedItem = DiscoveredDevice(
            address = result.device.address,
            name = record?.deviceName ?: result.device.name,
            rssi = result.rssi,
            connectable = result.isConnectable,
            serviceUuids = record?.serviceUuids
                ?.map { parcelUuid -> parcelUuid.uuid.toString() }
                .orEmpty(),
            manufacturerData = manufacturerData,
            rawScanRecordHex = record?.bytes?.toHexString(),
        )

        val knownMatch = KnownShoeRegistry.identify(scannedItem)
        val item = when {
            knownMatch?.stored == true -> scannedItem.copy(
                name = "Mein rechter Schuh · Adapt " +
                    (scannedItem.name ?: KnownShoeRegistry.ADVERTISED_MODEL_NAME),
            )
            knownMatch != null -> scannedItem.copy(
                name = "Passender Schuh · ${scannedItem.name ?: scannedItem.address}",
            )
            else -> scannedItem
        }
        if (knownMatch != null && announcedKnownDevices.add(item.address.uppercase())) {
            log(
                LogLevel.INFO,
                "SCHUH",
                if (knownMatch.stored) {
                    "Gespeicherter rechter Schuh erkannt: " +
                        "${item.name ?: KnownShoeRegistry.ADVERTISED_MODEL_NAME} (${item.address})."
                } else {
                    "Passendes Schuhprofil erkannt: ${item.name ?: item.address}."
                },
            )
        }

        _devices.update { current ->
            (current.filterNot { it.address == item.address } + item)
                .sortedWith(
                    compareByDescending<DiscoveredDevice> { KnownShoeRegistry.priority(it) }
                        .thenByDescending { it.rssi },
                )
        }
    }

    fun close() {
        stopScan()
    }
}
