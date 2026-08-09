package de.crazybatto.solelink.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private data class LightChoice(
    val name: String,
    val color: Color,
)

private data class FitMode(
    val name: String,
    val description: String,
    val left: Int,
    val right: Int,
    val colorIndex: Int,
)

private data class BottomDestination(
    val label: String,
    val symbol: String,
)

private val LIGHT_CHOICES = listOf(
    LightChoice("Eisblau", Color(0xFF4A9BFF)),
    LightChoice("Violett", Color(0xFF8F5CFF)),
    LightChoice("Magenta", Color(0xFFFF4BC2)),
    LightChoice("Rot", Color(0xFFFF4E5D)),
    LightChoice("Orange", Color(0xFFFF9D42)),
    LightChoice("Gold", Color(0xFFFFD24A)),
    LightChoice("Grün", Color(0xFF45D58A)),
    LightChoice("Türkis", Color(0xFF2BD8D0)),
    LightChoice("Weiß", Color(0xFFF1F5FF)),
)

private val FIT_MODES = listOf(
    FitMode("Locker", "Für entspanntes Tragen", 38, 38, 0),
    FitMode("Bewegen", "Ausgewogene Passform", 62, 62, 6),
    FitMode("Spiel", "Fester Halt für Aktivität", 78, 78, 3),
)

private val LIGHT_EFFECTS = listOf("Konstant", "Puls", "Gradient", "Strobe")
private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination("Start", "⌂"),
    BottomDestination("Modi", "M"),
    BottomDestination("Licht", "✦"),
    BottomDestination("Gerät", "⌁"),
)

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
    var leftFit by rememberSaveable { mutableIntStateOf(45) }
    var rightFit by rememberSaveable { mutableIntStateOf(45) }
    var selectedColorIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedEffectIndex by rememberSaveable { mutableIntStateOf(1) }
    var lightDurationSeconds by rememberSaveable { mutableFloatStateOf(30f) }
    var lightsEnabled by rememberSaveable { mutableStateOf(true) }
    var customModeSaved by rememberSaveable { mutableStateOf(false) }
    var customLeft by rememberSaveable { mutableIntStateOf(45) }
    var customRight by rememberSaveable { mutableIntStateOf(45) }
    var customColorIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedLight = LIGHT_CHOICES[selectedColorIndex]
    val connected = gattState.status in setOf(
        ConnectionStatus.CONNECTED,
        ConnectionStatus.DISCOVERING,
        ConnectionStatus.INSPECTING,
        ConnectionStatus.READY,
    )

    fun applyMode(mode: FitMode) {
        leftFit = mode.left
        rightFit = mode.right
        selectedColorIndex = mode.colorIndex.coerceIn(LIGHT_CHOICES.indices)
        viewModel.notePreviewAction(
            "Modus „${mode.name}“ lokal geladen: L ${mode.left}, R ${mode.right}.",
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03060C)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            selectedLight.color.copy(alpha = 0.78f),
                            Color(0xFF111A32),
                            Color(0xFF060910),
                            Color(0xFF03060C),
                        ),
                    ),
                ),
        )

        Image(
            painter = painterResource(R.drawable.dragon_katana_hero),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight(0.68f)
                .fillMaxWidth(0.78f)
                .alpha(0.055f),
            contentScale = ContentScale.Fit,
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                ControlBottomBar(
                    selectedTab = selectedTab,
                    accent = selectedLight.color,
                    onSelect = { selectedTab = it },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ControlHeader(
                    accent = selectedLight.color,
                    connected = connected,
                    deviceName = gattState.deviceName,
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            state = gattState,
                            accent = selectedLight.color,
                            leftFit = leftFit,
                            rightFit = rightFit,
                            onLeftFitChange = { leftFit = it.coerceIn(0, 100) },
                            onRightFitChange = { rightFit = it.coerceIn(0, 100) },
                            onOpenDevices = { selectedTab = 3 },
                            onApplyMode = ::applyMode,
                        )

                        1 -> ModesScreen(
                            accent = selectedLight.color,
                            currentLeft = leftFit,
                            currentRight = rightFit,
                            customModeSaved = customModeSaved,
                            customLeft = customLeft,
                            customRight = customRight,
                            customColorIndex = customColorIndex,
                            onApplyMode = ::applyMode,
                            onSaveCustom = {
                                customLeft = leftFit
                                customRight = rightFit
                                customColorIndex = selectedColorIndex
                                customModeSaved = true
                                viewModel.notePreviewAction(
                                    "Benutzerdefinierter Modus lokal gespeichert.",
                                )
                            },
                        )

                        2 -> LightsScreen(
                            selectedColorIndex = selectedColorIndex,
                            selectedEffectIndex = selectedEffectIndex,
                            durationSeconds = lightDurationSeconds,
                            lightsEnabled = lightsEnabled,
                            onColorSelected = {
                                selectedColorIndex = it
                                viewModel.notePreviewAction(
                                    "Lichtfarbe „${LIGHT_CHOICES[it].name}“ lokal ausgewählt.",
                                )
                            },
                            onEffectSelected = {
                                selectedEffectIndex = it
                                viewModel.notePreviewAction(
                                    "Lichteffekt „${LIGHT_EFFECTS[it]}“ lokal ausgewählt.",
                                )
                            },
                            onDurationChange = { lightDurationSeconds = it },
                            onLightsEnabledChange = { lightsEnabled = it },
                        )

                        else -> DeviceScreen(
                            devices = devices,
                            isScanning = isScanning,
                            state = gattState,
                            logs = logs,
                            permissionsGranted = permissionsGranted,
                            bluetoothEnabled = bluetoothEnabled,
                            accent = selectedLight.color,
                            onRequestPermissions = onRequestPermissions,
                            onEnableBluetooth = onEnableBluetooth,
                            onStartScan = viewModel::startScan,
                            onStopScan = viewModel::stopScan,
                            onClearDevices = viewModel::clearDevices,
                            onConnect = viewModel::connect,
                            onDisconnect = viewModel::disconnect,
                            onClearLogs = viewModel::clearLogs,
                            onExport = onExport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlHeader(
    accent: Color,
    connected: Boolean,
    deviceName: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.dragon_katana_icon),
            contentDescription = "SoleLink App-Symbol",
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(17.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SoleLink",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = deviceName ?: "Adaptive Fit Control",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.28f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            if (connected) Color(0xFF63DB8C) else accent,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (connected) "Verbunden" else "Bereit",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ControlBottomBar(
    selectedTab: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color.Black.copy(alpha = 0.58f),
        tonalElevation = 0.dp,
    ) {
        BOTTOM_DESTINATIONS.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onSelect(index) },
                icon = {
                    Text(
                        text = destination.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (selectedTab == index) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.58f)
                        },
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        color = if (selectedTab == index) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.58f)
                        },
                    )
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    indicatorColor = accent.copy(alpha = 0.34f),
                ),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: GattState,
    accent: Color,
    leftFit: Int,
    rightFit: Int,
    onLeftFitChange: (Int) -> Unit,
    onRightFitChange: (Int) -> Unit,
    onOpenDevices: () -> Unit,
    onApplyMode: (FitMode) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PairHeroCard(
                state = state,
                accent = accent,
                onOpenDevices = onOpenDevices,
            )
        }

        item {
            FitControlCard(
                accent = accent,
                leftFit = leftFit,
                rightFit = rightFit,
                onLeftFitChange = onLeftFitChange,
                onRightFitChange = onRightFitChange,
            )
        }

        item {
            SectionTitle(
                title = "Schnellmodi",
                subtitle = "Passform und Licht mit einem Tipp laden",
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(FIT_MODES) { mode ->
                    CompactModeCard(
                        mode = mode,
                        selected = leftFit == mode.left && rightFit == mode.right,
                        onClick = { onApplyMode(mode) },
                    )
                }
            }
        }

        item {
            PreviewNotice()
        }
    }
}

@Composable
private fun PairHeroCard(
    state: GattState,
    accent: Color,
    onOpenDevices: () -> Unit,
) {
    val connected = state.status in setOf(
        ConnectionStatus.CONNECTED,
        ConnectionStatus.DISCOVERING,
        ConnectionStatus.INSPECTING,
        ConnectionStatus.READY,
    )

    GlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.10f),
                        ),
                    ),
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.dragon_katana_hero),
                contentDescription = "Drache mit Katana",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.62f)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentScale = ContentScale.Fit,
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.58f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "DEIN PAAR",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = state.deviceName ?: "Noch kein Schuh verbunden",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            connected && state.status == ConnectionStatus.READY ->
                                "Bluetooth-Verbindung bereit"
                            connected ->
                                "Verbindung wird vorbereitet"
                            else ->
                                "Verbinde deine Schuhe lokal per Bluetooth"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.70f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.batteryPercent?.let { battery ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.30f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        ) {
                            Text(
                                text = "AKKU  $battery %",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Button(
                        onClick = onOpenDevices,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = if (connected) "Gerät verwalten" else "Schuhe verbinden",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FitControlCard(
    accent: Color,
    leftFit: Int,
    rightFit: Int,
    onLeftFitChange: (Int) -> Unit,
    onRightFitChange: (Int) -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(
                title = "Passform",
                subtitle = "Linken und rechten Schuh getrennt einstellen",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                FitSideControl(
                    modifier = Modifier.weight(1f),
                    side = "L",
                    value = leftFit,
                    accent = accent,
                    onValueChange = onLeftFitChange,
                )
                FitSideControl(
                    modifier = Modifier.weight(1f),
                    side = "R",
                    value = rightFit,
                    accent = accent,
                    onValueChange = onRightFitChange,
                )
            }

            Text(
                text = "Die Regler reagieren sofort in der App. Der Versand an den Schuh bleibt " +
                    "gesperrt, bis das echte Geräteprotokoll eindeutig zugeordnet ist.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.58f),
            )
        }
    }
}

@Composable
private fun FitSideControl(
    modifier: Modifier,
    side: String,
    value: Int,
    accent: Color,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = accent.copy(alpha = 0.24f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = side,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundControlButton(
                symbol = "−",
                onClick = { onValueChange(value - 5) },
            )
            RoundControlButton(
                symbol = "+",
                onClick = { onValueChange(value + 5) },
                accent = accent,
            )
        }
    }
}

@Composable
private fun RoundControlButton(
    symbol: String,
    onClick: () -> Unit,
    accent: Color? = null,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = accent?.copy(alpha = 0.86f) ?: Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineSmall,
                color = if (accent != null) Color.Black else Color.White,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CompactModeCard(
    mode: FitMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val modeColor = LIGHT_CHOICES[mode.colorIndex].color
    Card(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                modeColor.copy(alpha = 0.32f)
            } else {
                Color.Black.copy(alpha = 0.30f)
            },
        ),
        border = BorderStroke(
            1.dp,
            if (selected) modeColor else Color.White.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .background(modeColor, CircleShape),
            )
            Text(
                text = mode.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "L ${mode.left}  ·  R ${mode.right}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.80f),
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ModesScreen(
    accent: Color,
    currentLeft: Int,
    currentRight: Int,
    customModeSaved: Boolean,
    customLeft: Int,
    customRight: Int,
    customColorIndex: Int,
    onApplyMode: (FitMode) -> Unit,
    onSaveCustom: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenIntro(
                eyebrow = "DEINE MODI",
                title = "Eine Passform für jeden Moment",
                text = "Wähle einen Preset-Modus oder speichere deine aktuelle Kombination.",
                accent = accent,
            )
        }

        items(FIT_MODES) { mode ->
            LargeModeCard(
                mode = mode,
                selected = currentLeft == mode.left && currentRight == mode.right,
                onClick = { onApplyMode(mode) },
            )
        }

        if (customModeSaved) {
            item {
                LargeModeCard(
                    mode = FitMode(
                        name = "Mein Modus",
                        description = "Deine zuletzt gespeicherte Kombination",
                        left = customLeft,
                        right = customRight,
                        colorIndex = customColorIndex,
                    ),
                    selected = currentLeft == customLeft && currentRight == customRight,
                    onClick = {
                        onApplyMode(
                            FitMode(
                                name = "Mein Modus",
                                description = "Deine zuletzt gespeicherte Kombination",
                                left = customLeft,
                                right = customRight,
                                colorIndex = customColorIndex,
                            ),
                        )
                    },
                )
            }
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Aktuelle Kombination",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "L $currentLeft  ·  R $currentRight",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent,
                        fontWeight = FontWeight.Black,
                    )
                    Button(
                        onClick = onSaveCustom,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = if (customModeSaved) {
                                "Mein Modus aktualisieren"
                            } else {
                                "Als „Mein Modus“ speichern"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            PreviewNotice()
        }
    }
}

@Composable
private fun LargeModeCard(
    mode: FitMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val modeColor = LIGHT_CHOICES[mode.colorIndex].color
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick),
        borderColor = if (selected) modeColor else Color.White.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(modeColor.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, modeColor.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.name.take(1),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${mode.left} / ${mode.right}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) modeColor else Color.White,
                    fontWeight = FontWeight.Black,
                )
                if (selected) {
                    Text(
                        text = "AKTIV",
                        style = MaterialTheme.typography.labelSmall,
                        color = modeColor,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun LightsScreen(
    selectedColorIndex: Int,
    selectedEffectIndex: Int,
    durationSeconds: Float,
    lightsEnabled: Boolean,
    onColorSelected: (Int) -> Unit,
    onEffectSelected: (Int) -> Unit,
    onDurationChange: (Float) -> Unit,
    onLightsEnabledChange: (Boolean) -> Unit,
) {
    val light = LIGHT_CHOICES[selectedColorIndex]
    val infiniteTransition = rememberInfiniteTransition(label = "light-preview")
    val slowPulse by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (selectedEffectIndex == 3) 180 else 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "light-alpha",
    )
    val previewAlpha = when {
        !lightsEnabled -> 0.04f
        selectedEffectIndex == 0 -> 0.82f
        else -> slowPulse
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenIntro(
                eyebrow = "LICHT",
                title = "Dein Look, deine Farbe",
                text = "Passe Farbe, Effekt und Leuchtdauer an.",
                accent = light.color,
            )
        }

        item {
            GlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.24f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    light.color.copy(alpha = previewAlpha),
                                    light.color.copy(alpha = previewAlpha * 0.20f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                ) {
                    Image(
                        painter = painterResource(R.drawable.dragon_katana_hero),
                        contentDescription = "Lichtvorschau",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(0.96f)
                            .fillMaxWidth(0.72f),
                        contentScale = ContentScale.Fit,
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    ) {
                        Text(
                            text = if (lightsEnabled) {
                                LIGHT_EFFECTS[selectedEffectIndex]
                            } else {
                                "Aus"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Licht",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = if (lightsEnabled) light.name else "Ausgeschaltet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.62f),
                            )
                        }
                        Switch(
                            checked = lightsEnabled,
                            onCheckedChange = onLightsEnabledChange,
                        )
                    }

                    Text(
                        text = "Farbe",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(LIGHT_CHOICES) { index, choice ->
                            ColorSwatch(
                                choice = choice,
                                selected = selectedColorIndex == index,
                                onClick = { onColorSelected(index) },
                            )
                        }
                    }

                    Text(
                        text = "Effekt",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(LIGHT_EFFECTS) { index, effect ->
                            FilterChip(
                                selected = selectedEffectIndex == index,
                                onClick = { onEffectSelected(index) },
                                label = { Text(effect) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Leuchtdauer",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${durationSeconds.toInt()} s",
                            style = MaterialTheme.typography.labelLarge,
                            color = light.color,
                            fontWeight = FontWeight.Black,
                        )
                    }

                    Slider(
                        value = durationSeconds,
                        onValueChange = onDurationChange,
                        valueRange = 5f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = light.color,
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        }

        item {
            PreviewNotice()
        }
    }
}

@Composable
private fun ColorSwatch(
    choice: LightChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(choice.color, CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.20f),
                    shape = CircleShape,
                ),
        )
        Text(
            text = choice.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.56f),
        )
    }
}

@Composable
private fun DeviceScreen(
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    state: GattState,
    logs: List<LogEntry>,
    permissionsGranted: Boolean,
    bluetoothEnabled: Boolean,
    accent: Color,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClearDevices: () -> Unit,
    onConnect: (DiscoveredDevice) -> Unit,
    onDisconnect: () -> Unit,
    onClearLogs: () -> Unit,
    onExport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenIntro(
                eyebrow = "GERÄT",
                title = "Schuhe finden und verbinden",
                text = "Bluetooth-Scan, Verbindung und Diagnose an einem Ort.",
                accent = accent,
            )
        }

        if (!permissionsGranted) {
            item {
                RequirementCard(
                    title = "Bluetooth-Zugriff erforderlich",
                    text = "Erlaube „Geräte in der Nähe“, damit SoleLink nach BLE-Geräten suchen kann.",
                    buttonText = "Berechtigung erteilen",
                    onClick = onRequestPermissions,
                    accent = accent,
                )
            }
        } else if (!bluetoothEnabled) {
            item {
                RequirementCard(
                    title = "Bluetooth ist ausgeschaltet",
                    text = "Aktiviere Bluetooth, damit deine Schuhe gefunden werden können.",
                    buttonText = "Bluetooth aktivieren",
                    onClick = onEnableBluetooth,
                    accent = accent,
                )
            }
        }

        item {
            ConnectionPanel(
                state = state,
                accent = accent,
                onDisconnect = onDisconnect,
            )
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bluetooth-Scan",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "${devices.size} Gerät${if (devices.size == 1) "" else "e"} gefunden",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.60f),
                            )
                        }
                        TextButton(
                            onClick = onClearDevices,
                            enabled = devices.isNotEmpty() && !isScanning,
                        ) {
                            Text("Leeren")
                        }
                    }

                    Button(
                        onClick = if (isScanning) onStopScan else onStartScan,
                        enabled = permissionsGranted && bluetoothEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = if (isScanning) "Scan stoppen" else "Scan starten",
                            fontWeight = FontWeight.Black,
                        )
                    }

                    if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = accent,
                            trackColor = Color.White.copy(alpha = 0.12f),
                        )
                    }
                }
            }
        }

        if (devices.isEmpty()) {
            item {
                InfoCard(
                    title = "Noch kein Gerät gefunden",
                    text = "Schuhe einschalten, dicht ans Handy legen und den Scan starten.",
                )
            }
        } else {
            items(devices, key = { it.address }) { device ->
                DeviceCard(
                    device = device,
                    accent = accent,
                    enabled = permissionsGranted &&
                        bluetoothEnabled &&
                        device.connectable != false,
                    onConnect = { onConnect(device) },
                )
            }
        }

        if (state.services.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Diagnose",
                    subtitle = "${state.services.size} Dienste · " +
                        "${state.services.sumOf { it.characteristics.size }} Merkmale",
                )
            }

            items(state.services, key = { it.uuid }) { service ->
                ServiceAccordion(service = service, accent = accent)
            }
        }

        item {
            RecentLogCard(
                logs = logs,
                accent = accent,
                onClear = onClearLogs,
                onExport = onExport,
            )
        }
    }
}

@Composable
private fun ConnectionPanel(
    state: GattState,
    accent: Color,
    onDisconnect: () -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.deviceName ?: "Kein Gerät verbunden",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.deviceAddress ?: "Wähle unten ein Gerät aus",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f),
                        fontFamily = if (state.deviceAddress != null) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                ) {
                    Text(
                        text = state.status.toGermanLabel(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            state.batteryPercent?.let { battery ->
                Text(
                    text = "Akkustand: $battery %",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Black,
                )
            }

            state.lastError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFA8A8),
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
private fun DeviceCard(
    device: DiscoveredDevice,
    accent: Color,
    enabled: Boolean,
    onConnect: () -> Unit,
) {
    val likelyShoe = device.name?.let { name ->
        name.contains("adapt", ignoreCase = true) ||
            name.contains("bb", ignoreCase = true) ||
            name.contains("shoe", ignoreCase = true)
    } == true

    GlassCard(
        borderColor = if (likelyShoe) accent else Color.White.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name ?: "Unbenanntes BLE-Gerät",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f),
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = "${device.rssi} dBm",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (likelyShoe) {
                Text(
                    text = "Möglicher Adapt-Schuh",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Black,
                )
            }

            val advertisedServices = device.serviceUuids.map(::knownServiceName)
            if (advertisedServices.isNotEmpty()) {
                Text(
                    text = advertisedServices.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.56f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onConnect,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (likelyShoe) accent else Color.White.copy(alpha = 0.16f),
                    contentColor = if (likelyShoe) Color.Black else Color.White,
                ),
            ) {
                Text("Verbinden und prüfen", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServiceAccordion(
    service: ServiceSnapshot,
    accent: Color,
) {
    var expanded by rememberSaveable(service.uuid) { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .clickable { expanded = !expanded },
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = knownServiceName(service.uuid),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = service.uuid,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.54f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${if (service.isPrimary) "Primär" else "Sekundär"} · " +
                            "${service.characteristics.size} Merkmale",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Color.White,
                )
            }

            if (expanded) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                service.characteristics.forEachIndexed { index, characteristic ->
                    CharacteristicRow(characteristic)
                    if (index != service.characteristics.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                        )
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
            text = "Merkmal ${characteristic.uuid.shortUuid()}",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = characteristic.uuid,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.54f),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Eigenschaften: " +
                characteristic.properties.joinToString().ifBlank { "Keine" },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
        )

        if (characteristic.valueHex != null) {
            Text(
                text = "HEX: ${characteristic.valueHex}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.70f),
                fontFamily = FontFamily.Monospace,
            )
        }

        if (characteristic.properties.any { it.startsWith("WRITE") }) {
            Text(
                text = "Schreibfähig erkannt – ohne verifiziertes Steuerprofil gesperrt.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFA8A8),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RecentLogCard(
    logs: List<LogEntry>,
    accent: Color,
    onClear: () -> Unit,
    onExport: () -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protokoll",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${logs.size} Einträge",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                }
                TextButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                    Text("Leeren")
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "Noch keine Ereignisse.",
                    color = Color.White.copy(alpha = 0.56f),
                )
            } else {
                logs.takeLast(6).asReversed().forEach { entry ->
                    LogRow(entry)
                }
            }

            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Diagnose als JSON speichern", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFFFA8A8)
        LogLevel.WARNING -> Color(0xFFFFD58A)
        LogLevel.DATA -> Color(0xFF9FD7FF)
        LogLevel.INFO -> Color.White.copy(alpha = 0.72f)
    }
    val time = LOG_TIME_FORMAT.format(
        Instant.ofEpochMilli(entry.timestampMillis).atZone(ZoneId.systemDefault()),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.42f),
            fontFamily = FontFamily.Monospace,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.source,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.64f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
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
    accent: Color,
) {
    GlassCard(borderColor = accent.copy(alpha = 0.66f)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.66f),
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                ),
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScreenIntro(
    eyebrow: String,
    title: String,
    text: String,
    accent: Color,
) {
    Column(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.66f),
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.56f),
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    text: String,
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun PreviewNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.26f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Text(
            text = "LOKALE VORSCHAU · Die Bedienoberfläche ist aktiv. " +
                "Motor- und LED-Befehle werden erst nach einem verifizierten Protokollprofil gesendet.",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.34f),
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

private fun ConnectionStatus.toGermanLabel(): String = when (this) {
    ConnectionStatus.IDLE -> "Bereit"
    ConnectionStatus.CONNECTING -> "Verbinden"
    ConnectionStatus.CONNECTED -> "Verbunden"
    ConnectionStatus.DISCOVERING -> "Dienste"
    ConnectionStatus.INSPECTING -> "Prüfen"
    ConnectionStatus.READY -> "Bereit"
    ConnectionStatus.DISCONNECTED -> "Getrennt"
    ConnectionStatus.ERROR -> "Fehler"
}

private fun knownServiceName(uuid: String): String {
    val normalized = uuid.lowercase()
    return when {
        normalized.startsWith("00001800") -> "Allgemeiner Zugriff"
        normalized.startsWith("00001801") -> "GATT"
        normalized.startsWith("0000180a") -> "Geräteinformationen"
        normalized.startsWith("0000180f") -> "Batteriedienst"
        normalized.startsWith("0000fe2c") -> "Google Fast Pair"
        else -> "Dienst ${uuid.shortUuid()}"
    }
}

private fun String.shortUuid(): String {
    val normalized = lowercase()
    return when {
        normalized.length >= 8 && normalized.substring(4, 8) == "0000" ->
            uppercase().take(8)
        normalized.length >= 8 ->
            uppercase().take(8)
        else ->
            uppercase()
    }
}

private val LOG_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
