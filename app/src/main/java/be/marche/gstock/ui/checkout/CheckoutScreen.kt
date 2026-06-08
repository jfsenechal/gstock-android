package be.marche.gstock.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.data.remote.dto.ToolDto
import be.marche.gstock.ui.scan.QrScannerView

@Composable
fun CheckoutScreen(
    onFinished: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (state.step) {
            CheckoutStep.SCAN_WORKER -> ScanStep(
                title = "Step 1 — Scan worker badge",
                instruction = "Point the camera at the worker's QR code.",
                isProcessing = state.isProcessing,
                error = state.error,
                onScanned = viewModel::onWorkerScanned,
            )

            CheckoutStep.SCAN_TOOLS -> ScanToolsStep(
                state = state,
                onScanned = viewModel::onToolScanned,
                onRemoveTool = viewModel::removeTool,
                onFinish = viewModel::finish,
                onCancel = viewModel::reset,
            )

            CheckoutStep.DONE -> DoneStep(
                message = state.resultMessage ?: "Done",
                onNewCheckout = viewModel::reset,
                onViewCheckouts = {
                    viewModel.reset()
                    onFinished()
                },
            )
        }

        state.reservedTool?.let { tool ->
            ReservedToolDialog(tool = tool, onDismiss = viewModel::dismissReservedAlert)
        }
    }
}

@Composable
private fun ScanStep(
    title: String,
    instruction: String,
    isProcessing: Boolean,
    error: String?,
    onScanned: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(instruction, style = MaterialTheme.typography.bodyMedium)
            if (error != null) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            QrScannerView(
                onQrScanned = onScanned,
                modifier = Modifier.fillMaxSize(),
            )
            if (isProcessing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ScanToolsStep(
    state: CheckoutUiState,
    onScanned: (String) -> Unit,
    onRemoveTool: (Long) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val workerName = state.worker?.let { "${it.firstName} ${it.lastName}" } ?: ""
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Step 2 — Scan tools", style = MaterialTheme.typography.titleLarge)
            Text("Worker: $workerName", style = MaterialTheme.typography.bodyMedium)
            Text("Scan each tool's QR code, then tap Finished.", style = MaterialTheme.typography.bodyMedium)
            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Camera occupies the upper half so the cart stays visible.
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            QrScannerView(
                onQrScanned = onScanned,
                modifier = Modifier.fillMaxSize(),
            )
            if (state.isProcessing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text(
                "Tools to check out (${state.tools.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.tools.isEmpty()) {
                Text(
                    "No tools scanned yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.tools, key = { it.id }) { tool ->
                        ToolRow(tool = tool, onRemove = { onRemoveTool(tool.id) })
                    }
                }
            }

            Button(
                onClick = onFinish,
                enabled = state.tools.isNotEmpty() && !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Finished — check out ${state.tools.size} tool${if (state.tools.size > 1) "s" else ""}")
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !state.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolDto, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(tool.name, style = MaterialTheme.typography.titleSmall)
                listOfNotNull(tool.manufacturer, tool.model)
                    .filter { it.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${tool.name}")
            }
        }
    }
}

@Composable
private fun ReservedToolDialog(tool: ToolDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tool already reserved") },
        text = {
            Text(
                "\"${tool.name}\" is already checked out and cannot be added to this checkout. " +
                    "It was not added.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
private fun DoneStep(
    message: String,
    onNewCheckout: () -> Unit,
    onViewCheckouts: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onNewCheckout, modifier = Modifier.fillMaxWidth()) {
            Text("New checkout")
        }
        OutlinedButton(onClick = onViewCheckouts, modifier = Modifier.fillMaxWidth()) {
            Text("View checkouts")
        }
    }
}
