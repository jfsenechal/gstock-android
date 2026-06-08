package be.marche.gstock.ui.workers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.marche.gstock.data.local.entity.WorkerEntity
import be.marche.gstock.ui.common.LoadingBox
import be.marche.gstock.ui.common.MessageBox
import be.marche.gstock.ui.common.SearchField

@Composable
fun WorkersScreen(viewModel: WorkersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search workers",
            onSearch = viewModel::refresh,
        )
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.workers.isEmpty() -> LoadingBox()
                state.error != null && state.workers.isEmpty() ->
                    MessageBox(state.error!!, onRetry = viewModel::refresh)
                state.workers.isEmpty() -> MessageBox("No workers found")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.workers, key = { it.id }) { worker ->
                        WorkerRow(worker)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerRow(worker: WorkerEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    worker.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                if (worker.activeCheckoutsCount > 0) {
                    Badge { Text("${worker.activeCheckoutsCount} out") }
                }
            }
            worker.phone?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            worker.email?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            worker.status?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
