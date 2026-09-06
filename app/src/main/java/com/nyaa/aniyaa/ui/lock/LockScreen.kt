package com.nyaa.aniyaa.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    error: String?,
    lockRemainingMs: Long,
    biometricAvailable: Boolean,
    onUnlockWithPin: (String) -> Unit,
    onUnlockWithBiometric: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val lockedOut = lockRemainingMs > 0L

    LaunchedEffect(biometricAvailable, lockedOut) {
        if (biometricAvailable && !lockedOut) {
            onUnlockWithBiometric()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Aniyaa is locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (lockedOut) {
                    "Try again in ${(lockRemainingMs / 1000L).coerceAtLeast(1L)}s"
                } else {
                    "Enter your PIN to continue"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (pin.isEmpty()) "••••" else "•".repeat(pin.length),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (error != null && !lockedOut) {
                Spacer(Modifier.height(8.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            PinPad(
                enabled = !lockedOut,
                onDigit = { digit ->
                    if (pin.length < 8) pin += digit
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onUnlockWithPin(pin) },
                enabled = pin.length >= 4 && !lockedOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
            if (biometricAvailable) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUnlockWithBiometric,
                    enabled = !lockedOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use biometrics")
                }
            }
        }
    }
}

@Composable
private fun PinPad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))
                        "del" -> FilledTonalButton(
                            onClick = onBackspace,
                            enabled = enabled,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
                        }
                        else -> FilledTonalButton(
                            onClick = { onDigit(key) },
                            enabled = enabled,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
