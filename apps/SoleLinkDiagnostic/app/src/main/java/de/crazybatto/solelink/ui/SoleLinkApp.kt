package de.crazybatto.solelink.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.crazybatto.solelink.ble.CharacteristicSnapshot
import de.crazybatto.solelink.ble.ConnectionStatus
import de.crazybatto.solelink.ble.DiscoveredDevice
import de.crazybatto.solelink.ble.GattState
import de.crazybatto.solelink.ble.LogEntry
import de.crazybatto.solelink.ble.LogLevel
import de.crazybatto.solelink.ble.ServiceSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoleLinkApp(
    viewModel: MainViewModel,
    permissionsGranted: Boolean,
    bluetoothEnabled: Boolean,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onExport: () -> Unit,
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val gattState by viewModel.gattState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SoleLink Diagnostic",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Sicherer BLE-Inspektor",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    StatusDot(
                        active = permissionsGranted && bluetoothEnabled,
                        modifier = Modifier.padding(end = 18.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("⌁") },
                    label = { Text("Scan") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("◫") },
                    label = { Text("Inspektor") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("≡") },
                    label = { Text("Protokoll") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ScanScreen(
                modifier = Modifier.padding(innerPadding),
                devices = devices,
                isScanning = isScanning,
                permissionsGranted = permissionsGranted,
                bluetoothEnabled = bluetoothEnabled,
                onRequestPermissions = onRequestPermissions,
                onEnableBluetooth = onEnableBluetooth,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onClear = viewModel::clearDevices,
                onConnect = {
                    viewModel.connect(it)
                    selectedTab = 1
                },
            )

            1 -> InspectorScreen(
                modifier = Modifier.padding(innerPadding),
                state = gattState,
                onDisconnect = viewModel::disconnect,
            )

            else -> LogScreen(
                modifier = Modifier.padding(innerPadding),
                logs = logs,
                onClear = viewModel::clearLogs,
                onExport = onExport,
            )
        }
    }
}

@Composable
private fun ScanScreen(
    modifier: Modifier,
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    permissionsGranted: Boolean,
    bluetoothEnabled: Boolean,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit,
    onConnect: (DiscoveredDevice) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SafetyBanner()
        }

        if (!permissionsGranted) {
            item {
                RequirementCard(
                    title = "Bluetooth-Zugriff erforderlich",
                    text = "Die App benötigt „Geräte in der Nähe“, um BLE-Geräte zu finden " +
                        "und deren öffentliche GATT-Struktur zu lesen.",
                    buttonText = "Berechtigung erteilen",
                    onClick = onRequestPermissions,
                )
            }
        } else if (!bluetoothEnabled) {
            item {
                RequirementCard(
                    title = "Bluetooth ist ausgeschaltet",
                    text = "Aktiviere Bluetooth, damit der Schuh oder ein anderes BLE-Gerät " +
                        "gefunden werden kann.",
                    buttonText = "Bluetooth aktivieren",
                    onClick = onEnableBluetooth,
                )
            }
        }

        item {
            ScanControls(
                isScanning = isScanning,
                enabled = permissionsGranted && bluetoothEnabled,
                hasDevices = devices.isNotEmpty(),
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onClear = onClear,
            )
        }

        if (isScanning) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            SectionHeading(
                title = "Gefundene BLE-Geräte",
                subtitle = "${devices.size} Gerät${if (devices.size == 1) "" else "e"}",
            )
        }

        if (devices.isEmpty()) {
            item {
                EmptyState(
                    title = "Noch kein Gerät gefunden",
                    text = "Versetze beide Schuhe in Reichweite und starte den Scan. " +
                        "Die App filtert absichtlich nicht nach einem unbekannten Herstellercode.",
                )
            }
        } else {
            items(devices, key = { it.address }) { device ->
                DeviceCard(
                    device = device,
                    connectEnabled = permissionsGranted &&
                        bluetoothEnabled &&
                        device.connectable != false,
                    onConnect = { onConnect(device) },
                )
            }
        }
    }
}

@Composable
private fun ScanControls(
    isScanning: Boolean,
    enabled: Boolean,
    hasDevices: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Bluetooth-Scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Der Scan stoppt automatisch nach 12 Sekunden, um den Akku zu schonen.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = if (isScanning) onStopScan else onStartScan,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isScanning) "Scan stoppen" else "Scan starten")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = hasDevices && !isScanning,
                ) {
                    Text("Leeren")
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    connectEnabled: Boolean,
    onConnect: () -> Unit,
) {
    val likelyAdapt = device.name?.contains("adapt", ignoreCase = true) == true
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name ?: "Unbenanntes BLE-Gerät",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                RssiBadge(device.rssi)
            }

            if (likelyAdapt) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Name enthält „Adapt“",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Text(
                text = "Verbindbar: ${when (device.connectable) {
                    true -> "Ja"
                    false -> "Nein"
                    null -> "Unbekannt"
                }} · Dienste im Werbepaket: ${device.serviceUuids.size}",
                style = MaterialTheme.typography.bodySmall,
            )

            if (device.serviceUuids.isNotEmpty()) {
                Text(
                    text = device.serviceUuids.take(2).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (device.manufacturerData.isNotEmpty()) {
                Text(
                    text = "Hersteller-IDs: " +
                        device.manufacturerData.keys.joinToString { "0x%04X".format(it) },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onConnect,
                enabled = connectEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sicher verbinden und auslesen")
            }
        }
    }
}

@Composable
private fun InspectorScreen(
    modifier: Modifier,
    state: GattState,
    onDisconnect: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ConnectionCard(state = state, onDisconnect = onDisconnect)
        }

        item {
            SafetyBanner()
        }

        if (state.status in setOf(
                ConnectionStatus.CONNECTING,
                ConnectionStatus.CONNECTED,
                ConnectionStatus.DISCOVERING,
                ConnectionStatus.INSPECTING,
            )
        ) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (state.services.isEmpty()) {
            item {
                EmptyState(
                    title = "Keine GATT-Daten",
                    text = when (state.status) {
                        ConnectionStatus.IDLE ->
                            "Wähle im Tab „Scan“ zuerst ein BLE-Gerät aus."
                        ConnectionStatus.ERROR ->
                            state.lastError ?: "Die Verbindung ist fehlgeschlagen."
                        else ->
                            "Sobald Dienste erkannt wurden, erscheinen sie hier."
                    },
                )
            }
        } else {
            item {
                SectionHeading(
                    title = "GATT-Dienste",
                    subtitle = "${state.services.size} Dienste · " +
                        "${state.services.sumOf { it.characteristics.size }} Merkmale",
                )
            }
            items(state.services, key = { it.uuid }) { service ->
                ServiceCard(service)
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    state: GattState,
    onDisconnect: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.deviceName ?: "Keine aktive Gerätebezeichnung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.deviceAddress?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                ConnectionBadge(state.status)
            }

            state.batteryPercent?.let { battery ->
                Text(
                    text = "Standard-Batteriedienst: $battery %",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            state.lastError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.status !in setOf(
                    ConnectionStatus.IDLE,
                    ConnectionStatus.DISCONNECTED,
                    ConnectionStatus.ERROR,
                )
            ) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Verbindung trennen")
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceSnapshot) {
    var expanded by rememberSaveable(service.uuid) { mutableStateOf(false) }
    val title = knownGattName(service.uuid) ?: "Unbekannter Dienst"

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = service.uuid,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${if (service.isPrimary) "Primär" else "Sekundär"} · " +
                            "${service.characteristics.size} Merkmale",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                HorizontalDivider()
                service.characteristics.forEachIndexed { index, characteristic ->
                    CharacteristicRow(characteristic)
                    if (index < service.characteristics.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacteristicRow(characteristic: CharacteristicSnapshot) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = knownGattName(characteristic.uuid) ?: "Merkmal ${shortUuid(characteristic.uuid)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = characteristic.uuid,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = if (characteristic.properties.isEmpty()) {
                "Eigenschaften: keine angegeben"
            } else {
                "Eigenschaften: ${characteristic.properties.joinToString()}"
            },
            style = MaterialTheme.typography.labelSmall,
        )

        if (characteristic.notificationsEnabled) {
            Text(
                text = "Benachrichtigungen aktiv",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        characteristic.valueHex?.let { hex ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "HEX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = hex.ifBlank { "(leer)" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    characteristic.valueAscii?.let { ascii ->
                        Text(
                            text = "ASCII: $ascii",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        if (characteristic.properties.any { it.startsWith("WRITE") }) {
            Text(
                text = "Schreibfähig erkannt – in dieser Version absichtlich gesperrt.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun LogScreen(
    modifier: Modifier,
    logs: List<LogEntry>,
    onClear: () -> Unit,
    onExport: () -> Unit,
) {
    val reversed = logs.asReversed()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Diagnose exportieren",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Der JSON-Export enthält Bluetooth-Adressen, Werbedaten, " +
                            "GATT-UUIDs und empfangene Werte. Teile ihn nur bewusst.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExport,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("JSON speichern")
                        }
                        OutlinedButton(
                            onClick = onClear,
                            enabled = logs.isNotEmpty(),
                        ) {
                            Text("Leeren")
                        }
                    }
                }
            }
        }

        item {
            SectionHeading(
                title = "Ereignisprotokoll",
                subtitle = "${logs.size} Einträge · neueste zuerst",
            )
        }

        if (logs.isEmpty()) {
            item {
                EmptyState(
                    title = "Protokoll ist leer",
                    text = "Scan-, Verbindungs- und GATT-Ereignisse erscheinen hier.",
                )
            }
        } else {
            itemsIndexed(
                items = reversed,
                key = { index, entry -> "${entry.timestampMillis}-$index-${entry.source}" },
            ) { _, entry ->
                LogRow(entry)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val accent = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARNING -> Color(0xFFFFB74D)
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.DATA -> Color(0xFF80CBC4)
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(9.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.source,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(
                        text = formatTime(entry.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = if (entry.level == LogLevel.DATA) {
                        FontFamily.Monospace
                    } else {
                        FontFamily.Default
                    },
                )
            }
        }
    }
}

@Composable
private fun SafetyBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "NUR-LESE-MODUS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Die App scannt, verbindet, liest erlaubte Merkmale und aktiviert " +
                    "GATT-Benachrichtigungen. Sie sendet keine unbekannten Motor-, " +
                    "Schnürungs-, LED- oder Firmwarebefehle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun RequirementCard(
    title: String,
    text: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun StatusDot(active: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (active) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error,
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (active) "Bereit" else "Setup",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun RssiBadge(rssi: Int) {
    val label = when {
        rssi >= -60 -> "Sehr gut"
        rssi >= -75 -> "Gut"
        rssi >= -90 -> "Schwach"
        else -> "Sehr schwach"
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "$rssi dBm",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ConnectionBadge(status: ConnectionStatus) {
    val active = status in setOf(
        ConnectionStatus.CONNECTED,
        ConnectionStatus.DISCOVERING,
        ConnectionStatus.INSPECTING,
        ConnectionStatus.READY,
    )
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = status.germanLabel(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun ConnectionStatus.germanLabel(): String = when (this) {
    ConnectionStatus.IDLE -> "Bereit"
    ConnectionStatus.CONNECTING -> "Verbindet"
    ConnectionStatus.CONNECTED -> "Verbunden"
    ConnectionStatus.DISCOVERING -> "Dienste"
    ConnectionStatus.INSPECTING -> "Liest"
    ConnectionStatus.READY -> "Ausgelesen"
    ConnectionStatus.DISCONNECTED -> "Getrennt"
    ConnectionStatus.ERROR -> "Fehler"
}

private fun knownGattName(uuid: String): String? = when (shortUuid(uuid)) {
    "1800" -> "Generic Access"
    "1801" -> "Generic Attribute"
    "180A" -> "Geräteinformationen"
    "180F" -> "Batteriedienst"
    "2A00" -> "Gerätename"
    "2A01" -> "Darstellung"
    "2A19" -> "Batteriestand"
    "2A24" -> "Modellnummer"
    "2A25" -> "Seriennummer"
    "2A26" -> "Firmwareversion"
    "2A27" -> "Hardwareversion"
    "2A28" -> "Softwareversion"
    "2A29" -> "Herstellername"
    else -> null
}

private fun shortUuid(uuid: String): String {
    val normalized = uuid.lowercase()
    return if (
        normalized.length == 36 &&
        normalized.endsWith("-0000-1000-8000-00805f9b34fb") &&
        normalized.startsWith("0000")
    ) {
        normalized.substring(4, 8).uppercase()
    } else {
        uuid.take(8).uppercase()
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private fun formatTime(timestampMillis: Long): String =
    Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
