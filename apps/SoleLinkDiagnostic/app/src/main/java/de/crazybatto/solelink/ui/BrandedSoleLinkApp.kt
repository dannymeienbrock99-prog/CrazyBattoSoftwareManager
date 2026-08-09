package de.crazybatto.solelink.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.crazybatto.solelink.R
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

@Composable
fun BrandedSoleLinkApp(
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
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            DragonHeader(active = permissionsGranted && bluetoothEnabled)
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
            0 -> DragonScanScreen(
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
                onConnect = { device ->
                    viewModel.connect(device)
                    selectedTab = 1
                },
            )

            1 -> DragonInspectorScreen(
                modifier = Modifier.padding(innerPadding),
                state = gattState,
                onDisconnect = viewModel::disconnect,
            )

            else -> DragonLogScreen(
                modifier = Modifier.padding(innerPadding),
                logs = logs,
                onClear = viewModel::clearLogs,
                onExport = onExport,
            )
        }
    }
}

@Composable
private fun DragonHeader(active: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.dragon_katana_app_icon),
                contentDescription = "Drachen-Katana-App-Logo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SoleLink Diagnostic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Sicherer BLE-Inspektor",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(
                            if (active) Color(0xFF66C878) else MaterialTheme.colorScheme.error,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (active) "Bereit" else "Setup",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DragonScanScreen(
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
        if (!permissionsGranted) {
            item {
                DragonActionCard(
                    title = "Bluetooth-Zugriff erforderlich",
                    text = "Erlaube „Geräte in der Nähe“, damit die App deine Schuhe finden kann.",
                    buttonText = "Berechtigung erteilen",
                    onClick = onRequestPermissions,
                )
            }
        } else if (!bluetoothEnabled) {
            item {
                DragonActionCard(
                    title = "Bluetooth ist ausgeschaltet",
                    text = "Aktiviere Bluetooth, um nach dem Adapt-Schuh zu suchen.",
                    buttonText = "Bluetooth aktivieren",
                    onClick = onEnableBluetooth,
                )
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Bluetooth-Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Schuhe einschalten, dicht ans Handy legen und den Scan starten.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = if (isScanning) onStopScan else onStartScan,
                            enabled = permissionsGranted && bluetoothEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isScanning) "Scan stoppen" else "Scan starten")
                        }
                        OutlinedButton(
                            onClick = onClear,
                            enabled = devices.isNotEmpty() && !isScanning,
                        ) {
                            Text("Leeren")
                        }
                    }
                }
            }
        }

        if (isScanning) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            DragonSectionHeading(
                title = "Gefundene Geräte",
                subtitle = devices.size.toString(),
            )
        }

        if (devices.isEmpty()) {
            item {
                DragonInfoCard(
                    title = "Noch kein Schuh gefunden",
                    text = "Starte den Scan. Geräte mit „Adapt“ im Namen werden deutlich markiert.",
                )
            }
        } else {
            items(devices, key = { it.address }) { device ->
                DragonDeviceCard(
                    device = device,
                    enabled = permissionsGranted &&
                        bluetoothEnabled &&
                        device.connectable != false,
                    onConnect = { onConnect(device) },
                )
            }
        }
    }
}

@Composable
private fun DragonDeviceCard(
    device: DiscoveredDevice,
    enabled: Boolean,
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "${device.rssi} dBm",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (likelyAdapt) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Möglicher Adapt-Schuh",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = "Dienste im Werbepaket: ${device.serviceUuids.size}",
                style = MaterialTheme.typography.bodySmall,
            )

            Button(
                onClick = onConnect,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Verbinden und auslesen")
            }
        }
    }
}

@Composable
private fun DragonInspectorScreen(
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
            DragonConnectionCard(state = state, onDisconnect = onDisconnect)
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.dragon_katana_display),
                    contentDescription = "Drache um ein Katana",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        if (state.status in setOf(
                ConnectionStatus.CONNECTING,
                ConnectionStatus.CONNECTED,
                ConnectionStatus.DISCOVERING,
                ConnectionStatus.INSPECTING,
            )
        ) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        if (state.services.isEmpty()) {
            item {
                DragonInfoCard(
                    title = "Noch keine GATT-Daten",
                    text = when (state.status) {
                        ConnectionStatus.ERROR ->
                            state.lastError ?: "Die Verbindung ist fehlgeschlagen."
                        else ->
                            "Öffne unten den Tab „Scan“, verbinde den Schuh und kehre hierher zurück."
                    },
                )
            }
        } else {
            item {
                DragonSectionHeading(
                    title = "GATT-Dienste",
                    subtitle = "${state.services.size} Dienste",
                )
            }
            items(state.services, key = { it.uuid }) { service ->
                DragonServiceCard(service)
            }
        }

        item {
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
                        text = "SICHERER DIAGNOSEMODUS",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Lesen und Benachrichtigungen funktionieren. Unbekannte Schreibbefehle " +
                            "für Motor, Schnürung und LEDs bleiben bis zur Protokollanalyse gesperrt.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DragonConnectionCard(
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
                        text = state.deviceName ?: "Kein Schuh verbunden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    state.deviceAddress?.let { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.status == ConnectionStatus.READY) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = state.status.toGermanLabel(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            state.batteryPercent?.let { battery ->
                Text(
                    text = "Akku: $battery %",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            state.lastError?.let { message ->
                Text(
                    text = message,
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
private fun DragonServiceCard(service: ServiceSnapshot) {
    var expanded by rememberSaveable(service.uuid) {
        mutableStateOf(shortGattId(service.uuid) == "FE2C")
    }
    val title = knownServiceName(service.uuid) ?: "Unbekannter Dienst"

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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                HorizontalDivider()
                service.characteristics.forEachIndexed { index, characteristic ->
                    DragonCharacteristicRow(characteristic)
                    if (index < service.characteristics.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DragonCharacteristicRow(characteristic: CharacteristicSnapshot) {
    val writable = characteristic.properties.any { property ->
        property == "WRITE" || property == "WRITE_NO_RESPONSE" || property == "SIGNED_WRITE"
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Merkmal ${shortGattId(characteristic.uuid)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = characteristic.uuid,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Eigenschaften: ${characteristic.properties.joinToString().ifBlank { "Keine" }}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (writable) {
            Text(
                text = "Schreibfähig erkannt – in dieser Version absichtlich gesperrt.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }

        if (characteristic.notificationsEnabled) {
            Text(
                text = "Benachrichtigungen aktiv",
                color = Color(0xFF80CBC4),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        characteristic.valueHex?.let { value ->
            Text(
                text = "HEX: $value",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        characteristic.valueAscii
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                Text(
                    text = "Text: $value",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
    }
}

@Composable
private fun DragonLogScreen(
    modifier: Modifier,
    logs: List<LogEntry>,
    onClear: () -> Unit,
    onExport: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Text("JSON exportieren")
                }
                OutlinedButton(onClick = onClear) {
                    Text("Leeren")
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                DragonInfoCard(
                    title = "Protokoll ist leer",
                    text = "Scan-, Verbindungs- und GATT-Ereignisse erscheinen hier.",
                )
            }
        } else {
            items(logs.asReversed(), key = { it.timestampMillis }) { entry ->
                DragonLogRow(entry)
            }
        }
    }
}

@Composable
private fun DragonLogRow(entry: LogEntry) {
    val accent = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARNING -> Color(0xFFFFCC80)
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
                        text = formatDragonTime(entry.timestampMillis),
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
private fun DragonActionCard(
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun DragonInfoCard(title: String, text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DragonSectionHeading(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(text = subtitle, style = MaterialTheme.typography.labelMedium)
    }
}

private fun ConnectionStatus.toGermanLabel(): String = when (this) {
    ConnectionStatus.IDLE -> "Bereit"
    ConnectionStatus.CONNECTING -> "Verbindet"
    ConnectionStatus.CONNECTED -> "Verbunden"
    ConnectionStatus.DISCOVERING -> "Dienste"
    ConnectionStatus.INSPECTING -> "Liest"
    ConnectionStatus.READY -> "Ausgelesen"
    ConnectionStatus.DISCONNECTED -> "Getrennt"
    ConnectionStatus.ERROR -> "Fehler"
}

private fun knownServiceName(uuid: String): String? = when (shortGattId(uuid)) {
    "1800" -> "Generic Access"
    "1801" -> "Generic Attribute"
    "180A" -> "Geräteinformationen"
    "180F" -> "Batteriedienst"
    else -> null
}

private fun shortGattId(uuid: String): String {
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

private val dragonTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private fun formatDragonTime(timestampMillis: Long): String =
    Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(dragonTimeFormatter)
