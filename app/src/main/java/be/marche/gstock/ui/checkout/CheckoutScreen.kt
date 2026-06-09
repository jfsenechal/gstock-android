package be.marche.gstock.ui.checkout

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.R
import be.marche.gstock.data.remote.dto.ToolDto
import be.marche.gstock.data.remote.dto.WorkerDto
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
                title = stringResource(R.string.checkout_step1_title),
                instruction = stringResource(R.string.checkout_step1_instruction),
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
                message = state.resultMessage ?: stringResource(R.string.checkout_done_default),
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
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.worker?.let { WorkerHeaderCard(it) }
            Text(stringResource(R.string.checkout_step2_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.checkout_step2_instruction), style = MaterialTheme.typography.bodyMedium)
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
                stringResource(R.string.checkout_tools_to_checkout, state.tools.size),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.tools.isEmpty()) {
                Text(
                    stringResource(R.string.checkout_no_tools_scanned),
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
                Text(pluralStringResource(R.plurals.checkout_finish, state.tools.size, state.tools.size))
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !state.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

/** Prominent banner showing which worker the scanned tools will be checked out to. */
@Composable
private fun WorkerHeaderCard(worker: WorkerDto) {
    val fullName = "${worker.firstName} ${worker.lastName}".trim()
    val initials = listOf(worker.firstName, worker.lastName)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.checkout_worker_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
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
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.checkout_remove_tool, tool.name))
            }
        }
    }
}

@Composable
private fun ReservedToolDialog(tool: ToolDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checkout_reserved_title)) },
        text = {
            Text(stringResource(R.string.checkout_reserved_message, tool.name))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
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
            Text(stringResource(R.string.checkout_new))
        }
        OutlinedButton(onClick = onViewCheckouts, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.checkout_view))
        }
    }
}
