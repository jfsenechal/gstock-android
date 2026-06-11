package be.marche.gstock.ui.checkouts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.R
import be.marche.gstock.core.toReadableDateTime
import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.ui.common.LoadingBox
import be.marche.gstock.ui.common.MessageBox

@Composable
fun CheckoutsScreen(viewModel: CheckoutsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null && state.checkouts.isNotEmpty()) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckoutFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { viewModel.onFilterChange(f) },
                    label = { Text(stringResource(f.labelRes)) },
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.checkouts.isEmpty() -> LoadingBox()
                state.error != null && state.checkouts.isEmpty() ->
                    MessageBox(state.error!!, onRetry = viewModel::refresh)
                state.checkouts.isEmpty() -> MessageBox(stringResource(R.string.checkouts_empty), onRetry = viewModel::refresh)
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.checkouts, key = { it.id }) { checkout ->
                        CheckoutRow(
                            checkout = checkout,
                            isReturning = state.returningId == checkout.id,
                            onReturn = { viewModel.returnTool(checkout.id) },
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CheckoutRow(
    checkout: CheckoutEntity,
    isReturning: Boolean,
    onReturn: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            val unknown = stringResource(R.string.checkout_unknown)
            Text(
                checkout.toolName ?: stringResource(R.string.checkout_tool_fallback, checkout.toolId ?: unknown),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    R.string.checkout_worker_value,
                    checkout.workerName ?: checkout.workerId?.toString() ?: unknown,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val locale = LocalConfiguration.current.locales[0]
            checkout.checkedOutAt?.let {
                Text(
                    stringResource(R.string.checkout_out, it.toReadableDateTime(locale)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            checkout.dueAt?.let {
                Text(
                    stringResource(R.string.checkout_due, it.toReadableDateTime(locale)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusText = when {
                    checkout.isReturned -> stringResource(R.string.status_returned)
                    checkout.isOverdue -> stringResource(R.string.status_overdue)
                    else -> stringResource(R.string.status_active)
                }
                Text(statusText, style = MaterialTheme.typography.labelLarge)
                if (!checkout.isReturned) {
                    OutlinedButton(onClick = onReturn, enabled = !isReturning) {
                        if (isReturning) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.action_return))
                        }
                    }
                }
            }
        }
    }
}
