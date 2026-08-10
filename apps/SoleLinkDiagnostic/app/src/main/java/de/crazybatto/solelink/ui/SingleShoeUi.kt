package de.crazybatto.solelink.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class UiShoeMode {
    RIGHT_ONLY,
    LEFT_ONLY,
    BOTH_OPTIONAL,
}

val UiShoeMode.leftActive: Boolean
    get() = this != UiShoeMode.RIGHT_ONLY

val UiShoeMode.rightActive: Boolean
    get() = this != UiShoeMode.LEFT_ONLY

val UiShoeMode.shortLabel: String
    get() = when (this) {
        UiShoeMode.RIGHT_ONLY -> "Nur rechter Schuh"
        UiShoeMode.LEFT_ONLY -> "Nur linker Schuh"
        UiShoeMode.BOTH_OPTIONAL -> "Beide nacheinander (optional)"
    }

val UiShoeMode.logLabel: String
    get() = when (this) {
        UiShoeMode.RIGHT_ONLY -> "rechter Schuh"
        UiShoeMode.LEFT_ONLY -> "linker Schuh"
        UiShoeMode.BOTH_OPTIONAL -> "beide Schuhe nacheinander"
    }

@Composable
fun SingleShoeModeCard(
    mode: UiShoeMode,
    accent: Color,
    onModeChange: (UiShoeMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.34f),
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "EINZELSCHUH-MODUS",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Ein verbundener Schuh reicht. Der zweite Schuh ist optional und wird " +
                    "nicht zum Starten verlangt.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UiShoeMode.entries.forEach { option ->
                    FilterChip(
                        modifier = Modifier.fillMaxWidth(),
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        label = { Text(option.shortLabel) },
                    )
                }
            }
            Text(
                text = when (mode) {
                    UiShoeMode.RIGHT_ONLY ->
                        "Nur der rechte Schuh wird in der Bedienoberfläche verändert."
                    UiShoeMode.LEFT_ONLY ->
                        "Nur der linke Schuh wird in der Bedienoberfläche verändert."
                    UiShoeMode.BOTH_OPTIONAL ->
                        "Beide Seiten bleiben verfügbar, aber jeder Schuh kann einzeln und " +
                            "nacheinander verbunden werden."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.56f),
            )
        }
    }
}
