#!/usr/bin/env python3
"""Apply the single-shoe UI patch before the Android build.

The main Compose file predates the explicit single-shoe mode. Keeping this deterministic patch in
source control makes the generated APK and source artifact show that one connected shoe is enough,
while avoiding a risky wholesale rewrite of the large Compose screen in one commit.
"""

from pathlib import Path

SOURCE = Path(
    "apps/SoleLinkDiagnostic/app/src/main/java/"
    "de/crazybatto/solelink/ui/SoleLinkApp.kt"
)


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    text = SOURCE.read_text(encoding="utf-8")
    text = text.replace("import androidx.compose.foundation.layout.weight\n", "")

    text = replace_once(
        text,
        "    var customColorIndex by rememberSaveable { mutableIntStateOf(0) }\n\n"
        "    val selectedLight = LIGHT_CHOICES[selectedColorIndex]\n",
        "    var customColorIndex by rememberSaveable { mutableIntStateOf(0) }\n"
        "    var shoeModeName by rememberSaveable {\n"
        "        mutableStateOf(UiShoeMode.RIGHT_ONLY.name)\n"
        "    }\n\n"
        "    val shoeMode = UiShoeMode.valueOf(shoeModeName)\n"
        "    val selectedLight = LIGHT_CHOICES[selectedColorIndex]\n",
        "insert shoe mode state",
    )

    text = replace_once(
        text,
        "    fun applyMode(mode: FitMode) {\n"
        "        leftFit = mode.left\n"
        "        rightFit = mode.right\n"
        "        selectedColorIndex = mode.colorIndex.coerceIn(LIGHT_CHOICES.indices)\n"
        "        viewModel.notePreviewAction(\n"
        "            \"Modus „${mode.name}“ lokal geladen: L ${mode.left}, R ${mode.right}.\",\n"
        "        )\n"
        "    }\n",
        "    fun applyMode(mode: FitMode) {\n"
        "        if (shoeMode.leftActive) leftFit = mode.left\n"
        "        if (shoeMode.rightActive) rightFit = mode.right\n"
        "        selectedColorIndex = mode.colorIndex.coerceIn(LIGHT_CHOICES.indices)\n"
        "        viewModel.notePreviewAction(\n"
        "            \"Modus „${mode.name}“ für ${shoeMode.logLabel} lokal geladen. \" +\n"
        "                \"Ein zweiter Schuh ist nicht erforderlich.\",\n"
        "        )\n"
        "    }\n",
        "make modes side-aware",
    )

    text = replace_once(
        text,
        "                        0 -> HomeScreen(\n"
        "                            state = gattState,\n"
        "                            accent = selectedLight.color,\n"
        "                            leftFit = leftFit,\n"
        "                            rightFit = rightFit,\n"
        "                            onLeftFitChange = { leftFit = it.coerceIn(0, 100) },\n"
        "                            onRightFitChange = { rightFit = it.coerceIn(0, 100) },\n"
        "                            onOpenDevices = { selectedTab = 3 },\n"
        "                            onApplyMode = ::applyMode,\n"
        "                        )\n",
        "                        0 -> HomeScreen(\n"
        "                            state = gattState,\n"
        "                            accent = selectedLight.color,\n"
        "                            leftFit = leftFit,\n"
        "                            rightFit = rightFit,\n"
        "                            shoeMode = shoeMode,\n"
        "                            onShoeModeChange = { selectedMode ->\n"
        "                                shoeModeName = selectedMode.name\n"
        "                                viewModel.notePreviewAction(\n"
        "                                    \"Einzelschuh-Modus: ${selectedMode.logLabel}.\",\n"
        "                                )\n"
        "                            },\n"
        "                            onLeftFitChange = { leftFit = it.coerceIn(0, 100) },\n"
        "                            onRightFitChange = { rightFit = it.coerceIn(0, 100) },\n"
        "                            onOpenDevices = { selectedTab = 3 },\n"
        "                            onApplyMode = ::applyMode,\n"
        "                        )\n",
        "pass shoe mode into home screen",
    )

    text = replace_once(
        text,
        "private fun HomeScreen(\n"
        "    state: GattState,\n"
        "    accent: Color,\n"
        "    leftFit: Int,\n"
        "    rightFit: Int,\n"
        "    onLeftFitChange: (Int) -> Unit,\n"
        "    onRightFitChange: (Int) -> Unit,\n"
        "    onOpenDevices: () -> Unit,\n"
        "    onApplyMode: (FitMode) -> Unit,\n"
        ") {\n",
        "private fun HomeScreen(\n"
        "    state: GattState,\n"
        "    accent: Color,\n"
        "    leftFit: Int,\n"
        "    rightFit: Int,\n"
        "    shoeMode: UiShoeMode,\n"
        "    onShoeModeChange: (UiShoeMode) -> Unit,\n"
        "    onLeftFitChange: (Int) -> Unit,\n"
        "    onRightFitChange: (Int) -> Unit,\n"
        "    onOpenDevices: () -> Unit,\n"
        "    onApplyMode: (FitMode) -> Unit,\n"
        ") {\n",
        "extend home screen signature",
    )

    text = replace_once(
        text,
        "        item {\n"
        "            PairHeroCard(\n"
        "                state = state,\n"
        "                accent = accent,\n"
        "                onOpenDevices = onOpenDevices,\n"
        "            )\n"
        "        }\n\n"
        "        item {\n"
        "            FitControlCard(\n"
        "                accent = accent,\n"
        "                leftFit = leftFit,\n"
        "                rightFit = rightFit,\n"
        "                onLeftFitChange = onLeftFitChange,\n"
        "                onRightFitChange = onRightFitChange,\n"
        "            )\n"
        "        }\n",
        "        item {\n"
        "            PairHeroCard(\n"
        "                state = state,\n"
        "                accent = accent,\n"
        "                onOpenDevices = onOpenDevices,\n"
        "            )\n"
        "        }\n\n"
        "        item {\n"
        "            SingleShoeModeCard(\n"
        "                mode = shoeMode,\n"
        "                accent = accent,\n"
        "                onModeChange = onShoeModeChange,\n"
        "            )\n"
        "        }\n\n"
        "        item {\n"
        "            FitControlCard(\n"
        "                accent = accent,\n"
        "                leftFit = leftFit,\n"
        "                rightFit = rightFit,\n"
        "                shoeMode = shoeMode,\n"
        "                onLeftFitChange = onLeftFitChange,\n"
        "                onRightFitChange = onRightFitChange,\n"
        "            )\n"
        "        }\n",
        "insert single-shoe mode card",
    )

    text = replace_once(
        text,
        "private fun FitControlCard(\n"
        "    accent: Color,\n"
        "    leftFit: Int,\n"
        "    rightFit: Int,\n"
        "    onLeftFitChange: (Int) -> Unit,\n"
        "    onRightFitChange: (Int) -> Unit,\n"
        ") {\n",
        "private fun FitControlCard(\n"
        "    accent: Color,\n"
        "    leftFit: Int,\n"
        "    rightFit: Int,\n"
        "    shoeMode: UiShoeMode,\n"
        "    onLeftFitChange: (Int) -> Unit,\n"
        "    onRightFitChange: (Int) -> Unit,\n"
        ") {\n",
        "extend fit card signature",
    )

    text = replace_once(
        text,
        "                FitSideControl(\n"
        "                    modifier = Modifier.weight(1f),\n"
        "                    side = \"L\",\n"
        "                    value = leftFit,\n"
        "                    accent = accent,\n"
        "                    onValueChange = onLeftFitChange,\n"
        "                )\n"
        "                FitSideControl(\n"
        "                    modifier = Modifier.weight(1f),\n"
        "                    side = \"R\",\n"
        "                    value = rightFit,\n"
        "                    accent = accent,\n"
        "                    onValueChange = onRightFitChange,\n"
        "                )\n",
        "                FitSideControl(\n"
        "                    modifier = Modifier.weight(1f),\n"
        "                    side = \"L\",\n"
        "                    value = leftFit,\n"
        "                    accent = accent,\n"
        "                    enabled = shoeMode.leftActive,\n"
        "                    onValueChange = onLeftFitChange,\n"
        "                )\n"
        "                FitSideControl(\n"
        "                    modifier = Modifier.weight(1f),\n"
        "                    side = \"R\",\n"
        "                    value = rightFit,\n"
        "                    accent = accent,\n"
        "                    enabled = shoeMode.rightActive,\n"
        "                    onValueChange = onRightFitChange,\n"
        "                )\n",
        "enable only selected side controls",
    )

    text = replace_once(
        text,
        "private fun FitSideControl(\n"
        "    modifier: Modifier,\n"
        "    side: String,\n"
        "    value: Int,\n"
        "    accent: Color,\n"
        "    onValueChange: (Int) -> Unit,\n"
        ") {\n"
        "    Column(\n"
        "        modifier = modifier,\n",
        "private fun FitSideControl(\n"
        "    modifier: Modifier,\n"
        "    side: String,\n"
        "    value: Int,\n"
        "    accent: Color,\n"
        "    enabled: Boolean,\n"
        "    onValueChange: (Int) -> Unit,\n"
        ") {\n"
        "    Column(\n"
        "        modifier = modifier.alpha(if (enabled) 1f else 0.34f),\n",
        "make fit side disableable",
    )

    text = replace_once(
        text,
        "        Slider(\n"
        "            value = value.toFloat(),\n"
        "            onValueChange = { onValueChange(it.toInt()) },\n"
        "            valueRange = 0f..100f,\n",
        "        Slider(\n"
        "            value = value.toFloat(),\n"
        "            onValueChange = { onValueChange(it.toInt()) },\n"
        "            enabled = enabled,\n"
        "            valueRange = 0f..100f,\n",
        "disable inactive slider",
    )

    text = replace_once(
        text,
        "            RoundControlButton(\n"
        "                symbol = \"−\",\n"
        "                onClick = { onValueChange(value - 5) },\n"
        "            )\n"
        "            RoundControlButton(\n"
        "                symbol = \"+\",\n"
        "                onClick = { onValueChange(value + 5) },\n"
        "                accent = accent,\n"
        "            )\n",
        "            RoundControlButton(\n"
        "                symbol = \"−\",\n"
        "                enabled = enabled,\n"
        "                onClick = { onValueChange(value - 5) },\n"
        "            )\n"
        "            RoundControlButton(\n"
        "                symbol = \"+\",\n"
        "                enabled = enabled,\n"
        "                onClick = { onValueChange(value + 5) },\n"
        "                accent = accent,\n"
        "            )\n",
        "disable inactive step buttons",
    )

    text = replace_once(
        text,
        "private fun RoundControlButton(\n"
        "    symbol: String,\n"
        "    onClick: () -> Unit,\n"
        "    accent: Color? = null,\n"
        ") {\n"
        "    Surface(\n"
        "        modifier = Modifier\n"
        "            .size(48.dp)\n"
        "            .clickable(onClick = onClick),\n",
        "private fun RoundControlButton(\n"
        "    symbol: String,\n"
        "    enabled: Boolean = true,\n"
        "    onClick: () -> Unit,\n"
        "    accent: Color? = null,\n"
        ") {\n"
        "    Surface(\n"
        "        modifier = Modifier\n"
        "            .size(48.dp)\n"
        "            .clickable(enabled = enabled, onClick = onClick),\n",
        "make round control button disableable",
    )

    visible_replacements = {
        'text = "SoleLink",': 'text = "Nike Adapt BB 2.0",',
        'text = "DEIN PAAR",': 'text = "EIN SCHUH REICHT",',
        '"Verbinde deine Schuhe lokal per Bluetooth"':
            '"Verbinde links oder rechts – ein Schuh reicht"',
        '"Schuhe verbinden"': '"Einen Schuh verbinden"',
        'subtitle = "Linken und rechten Schuh getrennt einstellen",':
            'subtitle = "Jede Seite einzeln – der zweite Schuh ist optional",',
        'title = "Schuhe finden und verbinden",':
            'title = "Linken oder rechten Schuh verbinden",',
        'text = "Bluetooth-Scan, Verbindung und Diagnose an einem Ort.",':
            'text = "Ein Schuh reicht. Der zweite kann später verbunden werden.",',
        'text = "Aktiviere Bluetooth, damit deine Schuhe gefunden werden können.",':
            'text = "Aktiviere Bluetooth, damit ein Schuh gefunden werden kann.",',
        'text = "Schuhe einschalten, dicht ans Handy legen und den Scan starten.",':
            'text = "Nur einen Schuh einschalten, dicht ans Handy legen und den Scan starten.",',
    }
    for old, new in visible_replacements.items():
        if old not in text:
            raise SystemExit(f"visible wording not found: {old}")
        text = text.replace(old, new)

    SOURCE.write_text(text, encoding="utf-8")
    print("Single-shoe UI patch applied successfully.")


if __name__ == "__main__":
    main()
