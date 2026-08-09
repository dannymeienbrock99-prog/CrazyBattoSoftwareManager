package de.crazybatto.solelink

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.crazybatto.solelink.ui.MainViewModel
import de.crazybatto.solelink.ui.SoleLinkApp
import de.crazybatto.solelink.ui.theme.SoleLinkTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private var permissionsGranted by mutableStateOf(false)
    private var bluetoothEnabled by mutableStateOf(false)
    private var pendingExport: String? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshSystemState()
            viewModel.notePermissionResult(permissionsGranted)
        }

    private val bluetoothEnableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshSystemState()
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val export = pendingExport
            pendingExport = null
            if (uri == null || export == null) return@registerForActivityResult

            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(export)
                } ?: error("Zieldatei konnte nicht geöffnet werden.")
            }.onSuccess {
                Toast.makeText(this, "Diagnose wurde gespeichert.", Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                Toast.makeText(
                    this,
                    "Export fehlgeschlagen: ${error.message.orEmpty()}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshSystemState()

        setContent {
            SoleLinkTheme {
                SoleLinkApp(
                    viewModel = viewModel,
                    permissionsGranted = permissionsGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    onRequestPermissions = ::requestBluetoothPermissions,
                    onEnableBluetooth = ::requestBluetoothEnable,
                    onExport = ::exportDiagnostic,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemState()
    }

    private fun refreshSystemState() {
        permissionsGranted = BluetoothPermissions.hasAll(this)
        bluetoothEnabled = BluetoothPermissions.isBluetoothEnabled(this)
    }

    private fun requestBluetoothPermissions() {
        permissionLauncher.launch(BluetoothPermissions.runtimePermissions())
    }

    private fun requestBluetoothEnable() {
        if (!permissionsGranted) {
            requestBluetoothPermissions()
            return
        }

        runCatching {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }.onFailure {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }

    private fun exportDiagnostic() {
        pendingExport = viewModel.exportJson()
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"),
        )
        createDocumentLauncher.launch("solelink-diagnostic-$timestamp.json")
    }
}
